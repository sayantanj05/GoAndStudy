package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.CreateBookRequest;
import com.goandstudybackend.dto.request.UpdateBookRequest;
import com.goandstudybackend.dto.response.BookResponse;
import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.BookEmbedding;
import com.goandstudybackend.entity.BookReview;
import com.goandstudybackend.entity.SearchLog;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.service.AuthorService;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookEmbeddingRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import com.goandstudybackend.repository.GenreAnalyticsRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.ReservationRepository;
import com.goandstudybackend.repository.SearchLogRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.bson.Document;
import org.bson.types.ObjectId;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.stereotype.Service;

import java.net.URI;
import java.net.URLDecoder;
import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookService {

    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final AuthorService authorService;
    private final BookEmbeddingRepository bookEmbeddingRepository;
    private final BookReviewRepository bookReviewRepository;
    private final MemberRepository memberRepository;
    private final WishlistRepository wishlistRepository;
    private final ReservationRepository reservationRepository;
    private final SearchLogRepository searchLogRepository;
    private final GenreAnalyticsRepository genreAnalyticsRepository;
    private final LoanRepository loanRepository;
    private final NvidiaEmbeddingService nvidiaEmbeddingService;
    private final NvidiaLlmService nvidiaLlmService;
    private final ActivityLogService activityLogService;
    private final RealtimeEventService realtimeEventService;
    private final SystemConfigService systemConfigService;
    private final MongoTemplate mongoTemplate;

    public String buildCoverImageUrl(String isbn) {
        return "https://covers.openlibrary.org/b/isbn/" + isbn + "-L.jpg";
    }

    public String buildCoverThumbnailUrl(String isbn) {
        return "https://covers.openlibrary.org/b/isbn/" + isbn + "-M.jpg";
    }

    public Map<String, Object> addBook(CreateBookRequest request, String staffId) {
        try {
            if (bookRepository.findByIsbn(request.getIsbn()).isPresent()) {
                throw new DuplicateResourceException("Book with this ISBN already exists");
            }

        List<String> authorIds = new ArrayList<>();
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            authorIds.addAll(request.getAuthorIds());
        } else if (request.getAuthorName() != null && !request.getAuthorName().isBlank()) {
            String[] nameParts = request.getAuthorName().trim().split(",\\s*");
            List<String> names = Arrays.stream(nameParts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (names.isEmpty()) {
                throw new IllegalArgumentException("No valid author names found");
            }
            authorIds.addAll(authorService.findOrCreateAuthors(names));
        } else {
            throw new IllegalArgumentException("Author ID(s) or author name is required");
        }

        List<Author> authors = authorRepository.findAllById(authorIds);
        List<BookCategory> categories = bookCategoryRepository.findAllById(request.getCategoryIds());
        String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
        String genreNames = categories.stream().map(BookCategory::getName).collect(Collectors.joining(", "));

        boolean descriptionGenerated = false;
        String description = request.getDescription();
        if (description == null || description.isBlank()) {
            try {
                description = nvidiaLlmService.generate(
                        "Write a 2-sentence synopsis for a book titled '" + request.getTitle() + "' by '" + authorNames
                                + "' in the " + genreNames + " genre. Return only the synopsis text."
                );
                descriptionGenerated = description != null && !description.isBlank();
            } catch (Exception e) {
                log.warn("Failed to generate description, using empty: {}", e.getMessage());
                description = "";
            }
        }

        String coverImageUrl = (request.getCoverImageUrl() == null || request.getCoverImageUrl().isBlank())
                ? buildCoverImageUrl(request.getIsbn())
                : normalizeCoverImageUrl(request.getCoverImageUrl());
        String coverThumbnailUrl = (request.getCoverImageUrl() == null || request.getCoverImageUrl().isBlank())
                ? buildCoverThumbnailUrl(request.getIsbn())
                : coverImageUrl + "?w=150&h=200";
        boolean hasCoverImage = coverImageUrl != null && !coverImageUrl.isBlank();

        List<Double> embedding = new ArrayList<>();
        try {
            embedding = nvidiaEmbeddingService.generateEmbedding(
                    request.getTitle() + " " + authorNames + " " + (description == null ? "" : description) + " "
                            + String.join(" ", request.getTags())
            );
        } catch (Exception e) {
            log.warn("Failed to generate embedding, continuing without: {}", e.getMessage());
        }

        LocalDateTime now = LocalDateTime.now();
        Book book = Book.builder()
                .isbn(request.getIsbn())
                .title(request.getTitle())
                .subtitle(request.getSubtitle())
                .authorIds(new ArrayList<>(authorIds))
                .publisherId(request.getPublisherId())
                .categoryIds(new ArrayList<>(request.getCategoryIds()))
                .description(description == null ? "" : description)
                .language(request.getLanguage() == null || request.getLanguage().isBlank() ? "en" : request.getLanguage())
                .publishedYear(request.getPublishedYear() == null ? 0 : request.getPublishedYear())
                .pageCount(request.getPageCount() == null ? 0 : request.getPageCount())
                .totalCopies(request.getTotalCopies())
                .availableCopies(request.getTotalCopies())
                .coverImageUrl(coverImageUrl)
                .coverImageThumbnailUrl(coverThumbnailUrl)
                .hasCoverImage(hasCoverImage)
                .coverImageFormat(detectImageFormat(coverImageUrl))
                .tags(new ArrayList<>(request.getTags()))
                .embedding(new ArrayList<>(embedding))
                .addedByStaffId(staffId)
                .createdAt(now)
                .updatedAt(now)
                .build();
        Book savedBook = bookRepository.save(book);

        try {
            authorService.updateAuthorsStats(savedBook.getAuthorIds());
        } catch (Exception e) {
            log.warn("Failed to update author stats: {}", e.getMessage());
        }

        try {
            bookEmbeddingRepository.save(BookEmbedding.builder()
                .bookId(savedBook.getId())
                .isbn(savedBook.getIsbn())
                .embeddingModel("nvidia/nv-embedqa-e5-v5")
                .embedding(new ArrayList<>(embedding))
                .embeddingSource("catalog_creation")
                .dimensions(1024)
                .createdAt(now)
                .updatedAt(now)
                .build());
        } catch (Exception e) {
            log.warn("Failed to save book embedding: {}", e.getMessage());
        }

        try {
            activityLogService.log(
                    "BOOK_CREATED",
                    staffId,
                    "ROLE_STAFF",
                    "books",
                    savedBook.getId(),
                    null,
                    Map.of("isbn", savedBook.getIsbn(), "title", savedBook.getTitle())
            );
} catch (Exception e) {
            log.warn("Failed to log activity: {}", e.getMessage());
        }
        try {
            realtimeEventService.broadcastBookAdded(savedBook.getId(), savedBook.getTitle());
        } catch (Exception e) {
            log.warn("Failed to broadcast book added: {}", e.getMessage());
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", savedBook.getId());
        response.put("isbn", savedBook.getIsbn());
        response.put("title", savedBook.getTitle());
        response.put("description", savedBook.getDescription());
        response.put("coverImageUrl", savedBook.getCoverImageUrl());
        response.put("hasCoverImage", savedBook.isHasCoverImage());
        response.put("embeddingCreated", !embedding.isEmpty());
        response.put("descriptionGenerated", descriptionGenerated);
        return response;
    } catch (Exception e) {
        log.error("Error adding book: {}", e.getMessage(), e);
        throw new RuntimeException("Failed to add book: " + e.getMessage(), e);
    }
}


    public Map<String, Object> editBook(String bookId, UpdateBookRequest request, String staffId) {
        LocalDateTime now = LocalDateTime.now();
        Book book = getActiveBook(bookId);
        Map<String, Object> before = bookSnapshot(book);
        List<String> updatedFields = new ArrayList<>();
        boolean reEmbedded = false;

        if (request.getTitle() != null && !request.getTitle().isBlank() && !request.getTitle().equals(book.getTitle())) {
            book.setTitle(request.getTitle());
            updatedFields.add("title");
        }
        if (request.getDescription() != null && !request.getDescription().equals(book.getDescription())) {
            book.setDescription(request.getDescription());
            updatedFields.add("description");
        }
        if (request.getTotalCopies() != null && request.getTotalCopies() != book.getTotalCopies()) {
            int delta = request.getTotalCopies() - book.getTotalCopies();
            book.setTotalCopies(request.getTotalCopies());
            book.setAvailableCopies(Math.max(0, book.getAvailableCopies() + delta));
            updatedFields.add("totalCopies");
        }
        if (request.getTags() != null && !request.getTags().isEmpty()) {
            book.setTags(new ArrayList<>(request.getTags()));
            updatedFields.add("tags");
        }
        
        // Handle author updates
        List<String> newAuthorIds = new ArrayList<>();
        if (request.getAuthorIds() != null && !request.getAuthorIds().isEmpty()) {
            newAuthorIds.addAll(request.getAuthorIds());
        } else if (request.getAuthorName() != null && !request.getAuthorName().isBlank()) {
            String[] nameParts = request.getAuthorName().trim().split(",\\s*");
            List<String> names = Arrays.stream(nameParts)
                .map(String::trim)
                .filter(s -> !s.isEmpty())
                .collect(Collectors.toList());
            if (!names.isEmpty()) {
                newAuthorIds.addAll(authorService.findOrCreateAuthors(names));
            }
        }
        if (!newAuthorIds.isEmpty() && !newAuthorIds.equals(book.getAuthorIds())) {
            List<String> oldAuthors = book.getAuthorIds();
            book.setAuthorIds(newAuthorIds);
            updatedFields.add("authorIds");
            // Update stats for old and new authors
            Set<String> allAffected = new HashSet<>();
            if (oldAuthors != null) allAffected.addAll(oldAuthors);
            allAffected.addAll(newAuthorIds);
            authorService.updateAuthorsStats(new ArrayList<>(allAffected));
        }

        // Handle category updates
        if (request.getCategoryIds() != null && !request.getCategoryIds().isEmpty()) {
            List<String> newCategoryIds = new ArrayList<>(request.getCategoryIds());
            if (!newCategoryIds.equals(book.getCategoryIds())) {
                book.setCategoryIds(newCategoryIds);
                updatedFields.add("categoryIds");
            }
        }

        if (request.getCoverImageUrl() != null) {
            String newUrl = normalizeCoverImageUrl(request.getCoverImageUrl());
            book.setCoverImageUrl(newUrl);
            book.setCoverImageThumbnailUrl(newUrl.isBlank() ? "" : newUrl + "?w=150&h=200");
            book.setHasCoverImage(!newUrl.isBlank());
            book.setCoverImageFormat(detectImageFormat(newUrl));
            updatedFields.add("coverImageUrl");
        }

        if (updatedFields.contains("title") || updatedFields.contains("description") || updatedFields.contains("tags") || updatedFields.contains("authorIds") || updatedFields.contains("categoryIds")) {
            List<Author> authors = authorRepository.findAllById(book.getAuthorIds());
            String authorNames = authors.stream().map(Author::getName).collect(Collectors.joining(", "));
            List<Double> embedding = nvidiaEmbeddingService.generateEmbedding(
                    book.getTitle() + " " + authorNames + " " + book.getDescription() + " " + String.join(" ", book.getTags())
            );
            book.setEmbedding(new ArrayList<>(embedding));
            BookEmbedding bookEmbedding = bookEmbeddingRepository.findByBookId(bookId)
                    .orElse(BookEmbedding.builder().bookId(bookId).isbn(book.getIsbn()).createdAt(LocalDateTime.now()).build());
            bookEmbedding.setEmbedding(new ArrayList<>(embedding));
            bookEmbedding.setEmbeddingModel("nvidia/nv-embedqa-e5-v5");
            bookEmbedding.setEmbeddingSource("catalog_update");
            bookEmbedding.setUpdatedAt(LocalDateTime.now());
            bookEmbeddingRepository.save(bookEmbedding);
            reEmbedded = !embedding.isEmpty();
        }

        book.setUpdatedByStaffId(staffId);
        book.setCreatedBy(staffId);
        book.setUpdatedAt(now);
        bookRepository.save(book);

        activityLogService.log(
                "BOOK_UPDATED",
                staffId,
                "ROLE_STAFF",
                "books",
                bookId,
                Map.of("before", before, "after", bookSnapshot(book)),
                Map.of("updatedFields", updatedFields)
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", bookId);
        response.put("updatedFields", updatedFields);
        response.put("reEmbedded", reEmbedded);
        return response;
    }

    public Map<String, Object> getBooks(String q, String genre, Boolean available, int page, int size) {
        Query query = new Query().addCriteria(Criteria.where("isDeleted").is(false));
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(pattern),
                    Criteria.where("description").regex(pattern)
            ));
        }
        if (genre != null && !genre.isBlank()) {
            query.addCriteria(Criteria.where("categoryIds").in(genre));
        }
        if (Boolean.TRUE.equals(available)) {
            query.addCriteria(Criteria.where("availableCopies").gt(0));
        }

        long total = mongoTemplate.count(query, Book.class);
        int pageSize = size > 0 ? size : 20;
        // If size is 0, fetch all books (no pagination limit)
        if (size != 0) {
            query.with(PageRequest.of(Math.max(page - 1, 0), pageSize));
        }
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        List<BookResponse> books = mongoTemplate.find(query, Book.class).stream().map(this::toBookResponse).toList();
        return pageResponse("books", books, total, page, size > 0 ? size : (int) total);
    }

    // Backward compatibility - default size of 20
    public Map<String, Object> getBooks(String q, String genre, Boolean available, int page) {
        return getBooks(q, genre, available, page, 20);
    }

    public Map<String, Object> getBookById(String bookId) {
        Book book = getActiveBook(bookId);
        List<Author> authors = authorRepository.findAllById(book.getAuthorIds());
        List<BookCategory> categories = bookCategoryRepository.findAllById(book.getCategoryIds());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("book", book);
        response.put("authors", authors);
        response.put("categories", categories);
        return response;
    }

    public Map<String, Object> getBookByIsbn(String isbn) {
        String normalizedInput = normalizeIsbn(isbn);
        Book book = mongoTemplate.findAll(Book.class).stream()
                .filter(existingBook -> !existingBook.isDeleted())
                .filter(existingBook -> normalizeIsbn(existingBook.getIsbn()).equalsIgnoreCase(normalizedInput))
                .findFirst()
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        BookResponse response = toBookResponse(book);
        Map<String, Object> result = new LinkedHashMap<>();
        result.put("bookId", response.getBookId());
        result.put("isbn", response.getIsbn());
        result.put("title", response.getTitle());
        result.put("authorIds", response.getAuthorIds());
        result.put("author", response.getAuthor());
        result.put("category", response.getCategory());
        result.put("genre", response.getGenre());
        result.put("coverImageUrl", response.getCoverImageUrl());
        result.put("coverImageThumbnailUrl", response.getCoverImageThumbnailUrl());
        result.put("hasCoverImage", response.isHasCoverImage());
        result.put("availableCopies", response.getAvailableCopies());
        result.put("totalCopies", response.getTotalCopies());
        result.put("averageRating", response.getAverageRating());
        result.put("totalRatings", response.getTotalRatings());
        result.put("description", response.getDescription());
        result.put("pageCount", response.getPageCount());
        result.put("publishedYear", response.getPublishedYear());
        result.put("language", response.getLanguage());
        result.put("isDeleted", response.isDeleted());
        result.put("createdAt", response.getCreatedAt());
        return result;
    }

    private String normalizeIsbn(String isbn) {
        if (isbn == null) {
            return "";
        }
        return isbn.replaceAll("[\\s-]", "").trim();
    }

    public Map<String, Object> getAllBooksIncludingDeleted(Boolean isDeleted, String genre, int page, int size) {
        Query query = new Query();
        if (isDeleted != null) {
            query.addCriteria(Criteria.where("isDeleted").is(isDeleted));
        }
        if (genre != null && !genre.isBlank()) {
            query.addCriteria(Criteria.where("categoryIds").in(genre));
        }

        long total = mongoTemplate.count(query, Book.class);
        // If size is 0 or negative, fetch all books (no pagination)
        if (size > 0) {
            query.with(PageRequest.of(Math.max(page - 1, 0), size));
        }
        query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        List<BookResponse> books = mongoTemplate.find(query, Book.class).stream().map(this::toBookResponse).toList();
        return pageResponse("books", books, total, page, size > 0 ? size : (int) total);
    }

    // Backward compatibility - default size of 20
    public Map<String, Object> getAllBooksIncludingDeleted(Boolean isDeleted, String genre, int page) {
        return getAllBooksIncludingDeleted(isDeleted, genre, page, 20);
    }

    public Map<String, Object> softDeleteBook(String bookId, String adminId, String reason) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (book.isDeleted()) {
            throw new IllegalArgumentException("Book is already deleted");
        }
        long activeLoans = loanRepository.countByBookIdAndStatusIn(bookId, List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Overdue", "Issued", "Renewed"));
        if (activeLoans > 0) {
            throw new DuplicateResourceException("Cannot delete book with active loans");
        }
        Map<String, Object> before = Map.of("isDeleted", false);
        book.setDeleted(true);
        book.setDeletedByAdminId(adminId);
        book.setDeletedAt(LocalDateTime.now());
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
        activityLogService.log(
                "BOOK_DELETED",
                adminId,
                "ROLE_ADMIN",
                "books",
                bookId,
                deletionChanges(before, reason),
                null
        );
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", bookId);
        response.put("isDeleted", true);
        response.put("deletedAt", book.getDeletedAt());
        return response;
    }

    public Map<String, Object> restoreDeletedBook(String bookId, String adminId) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        if (!book.isDeleted()) {
            throw new IllegalArgumentException("Book is not deleted");
        }
        book.setDeleted(false);
        book.setDeletedByAdminId(null);
        book.setDeletedAt(null);
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);
        activityLogService.log("BOOK_RESTORED", adminId, "ROLE_ADMIN", "books", bookId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("bookId", bookId);
        response.put("isDeleted", false);
        return response;
    }

    public Map<String, Object> browseBooks(String memberId,
                                           String q,
                                           String genre,
                                           String language,
                                           Boolean available,
                                           String sortBy,
                                           int page,
                                           int size) {
        Query query = new Query().addCriteria(Criteria.where("isDeleted").is(false));
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("title").regex(pattern),
                    Criteria.where("description").regex(pattern)
            ));
        }
        if (genre != null && !genre.isBlank()) {
            query.addCriteria(Criteria.where("categoryIds").in(genre));
        }
        if (language != null && !language.isBlank()) {
            query.addCriteria(Criteria.where("language").is(language));
        }
        if (Boolean.TRUE.equals(available)) {
            query.addCriteria(Criteria.where("availableCopies").gt(0));
        }

        if ("rating".equalsIgnoreCase(sortBy)) {
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "averageRating"));
        } else if ("newest".equalsIgnoreCase(sortBy)) {
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        } else if ("popular".equalsIgnoreCase(sortBy)) {
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "totalIssues"));
        } else {
            // Default sort: newest first
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "createdAt"));
        }

        long total = mongoTemplate.count(query, Book.class);
        // If size is 0 or negative, fetch all books (no pagination limit)
        if (size > 0) {
            query.with(PageRequest.of(Math.max(page - 1, 0), size));
        }
        List<Book> books = mongoTemplate.find(query, Book.class);

        // Batch-load names to avoid showing raw IDs in the UI.
        LinkedHashSet<String> authorIds = new LinkedHashSet<>();
        LinkedHashSet<String> categoryIds = new LinkedHashSet<>();
        for (Book book : books) {
            if (book.getAuthorIds() != null) authorIds.addAll(book.getAuthorIds());
            if (book.getCategoryIds() != null) categoryIds.addAll(book.getCategoryIds());
        }
        Map<String, String> authorNameById = authorRepository.findAllById(authorIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(Author::getId, Author::getName, (a, b) -> a));
        Map<String, String> categoryNameById = bookCategoryRepository.findAllById(categoryIds).stream()
                .filter(Objects::nonNull)
                .collect(Collectors.toMap(BookCategory::getId, BookCategory::getName, (a, b) -> a));

        List<Map<String, Object>> bookItems = new ArrayList<>();
        for (Book book : books) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("bookId", book.getId());
            item.put("isbn", book.getIsbn());
            item.put("title", book.getTitle());
            item.put("authorIds", book.getAuthorIds());
            item.put("author", joinAuthorNames(book.getAuthorIds(), authorNameById));
            String categoryName = primaryCategoryName(book.getCategoryIds(), categoryNameById);
            item.put("category", categoryName);
            item.put("genre", categoryName);
            item.put("categoryIds", book.getCategoryIds() != null ? book.getCategoryIds() : new ArrayList<>());
            // Map category IDs to names for display
            List<String> categoryNames = book.getCategoryIds() != null
                ? book.getCategoryIds().stream()
                    .map(id -> categoryNameById.getOrDefault(id, id))
                    .collect(Collectors.toList())
                : new ArrayList<>();
            item.put("categoryNames", categoryNames);
            item.put("coverImageUrl", book.getCoverImageUrl());
            item.put("availableCopies", book.getAvailableCopies());
            item.put("averageRating", book.getAverageRating());
            item.put("totalRatings", book.getTotalRatings());
            item.put("hasCoverImage", book.isHasCoverImage());
            item.put("isWishlisted", wishlistRepository.existsByMemberIdAndBookId(memberId, book.getId()));
            bookItems.add(item);
        }

        LocalDateTime now = LocalDateTime.now();
        searchLogRepository.save(SearchLog.builder()
                .memberId(memberId)
                .query(q)
                .queryNormalised(q == null ? "" : q.trim().toLowerCase())
                .filters(Map.of(
                        "genre", genre == null ? "" : genre,
                        "language", language == null ? "" : language,
                        "available", available == null ? false : available
                ))
                .resultsCount((int) total)
                .topResultIds(books.stream().map(Book::getId).limit(10).toList())
                .searchSource("browse")
                .createdAt(now)
                .createdBy(memberId)
                .updatedAt(now)
                .build());

        return pageResponse("books", bookItems, total, page, size > 0 ? size : (int) total);
    }

    // Backward compatibility method
    public Map<String, Object> browseBooks(String memberId,
                                           String q,
                                           String genre,
                                           String language,
                                           Boolean available,
                                           String sortBy,
                                           int page) {
        return browseBooks(memberId, q, genre, language, available, sortBy, page, 20);
    }

    public Map<String, Object> getBookDetail(String memberId, String bookId) {
        Book book = getActiveBook(bookId);
        String resolvedBookId = book.getId();
        
        // Safely fetch authors (handle missing authors gracefully)
        List<Author> authors = new ArrayList<>();
        try {
            if (book.getAuthorIds() != null && !book.getAuthorIds().isEmpty()) {
                authors = authorRepository.findAllById(book.getAuthorIds()).stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch authors for book " + resolvedBookId + ": " + e.getMessage());
            authors = new ArrayList<>();
        }
        
        // Safely fetch categories (handle missing categories gracefully)
        List<BookCategory> categories = new ArrayList<>();
        try {
            if (book.getCategoryIds() != null && !book.getCategoryIds().isEmpty()) {
                categories = bookCategoryRepository.findAllById(book.getCategoryIds()).stream()
                        .filter(java.util.Objects::nonNull)
                        .collect(java.util.stream.Collectors.toList());
            }
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch categories for book " + resolvedBookId + ": " + e.getMessage());
            categories = new ArrayList<>();
        }
        
        // Safely fetch reviews with member names
        List<Map<String, Object>> topReviews = new ArrayList<>();
        try {
            List<BookReview> reviews = bookReviewRepository.findByBookId(resolvedBookId).stream()
                    .sorted(Comparator.comparingInt(BookReview::getHelpfulVotes).reversed()
                            .thenComparing(BookReview::getRating, Comparator.reverseOrder()))
                    .limit(5)
                    .toList();
            
            // Fetch member names for reviews
            LinkedHashSet<String> reviewMemberIds = reviews.stream()
                    .map(BookReview::getMemberId)
                    .collect(Collectors.toCollection(LinkedHashSet::new));
            Map<String, String> memberNames = memberRepository.findAllById(reviewMemberIds).stream()
                    .filter(java.util.Objects::nonNull)
                    .collect(Collectors.toMap(com.goandstudybackend.entity.Member::getId, 
                            com.goandstudybackend.entity.Member::getName, (a, b) -> a));
            
            topReviews = reviews.stream().map(review -> {
                Map<String, Object> map = new LinkedHashMap<>();
                map.put("id", review.getId());
                map.put("rating", review.getRating());
                map.put("reviewText", review.getReviewText());
                map.put("feedbackText", review.getFeedbackText());
                map.put("helpfulVotes", review.getHelpfulVotes());
                map.put("createdAt", review.getCreatedAt());
                map.put("memberName", memberNames.getOrDefault(review.getMemberId(), "Anonymous Member"));
                return map;
            }).toList();
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch reviews for book " + resolvedBookId + ": " + e.getMessage());
            topReviews = new ArrayList<>();
        }
        
        // Safely check wishlist
        boolean isWishlisted = false;
        try {
            isWishlisted = wishlistRepository.existsByMemberIdAndBookId(memberId, resolvedBookId);
        } catch (Exception e) {
            System.err.println("Warning: Failed to check wishlist for book " + resolvedBookId + ": " + e.getMessage());
        }
        
        // Safely check reservations
        boolean canReserve = false;
        try {
            boolean reservationsEnabled = systemConfigService.isFeatureEnabled("reservations");
            boolean hasPendingReservation = reservationRepository.existsByMemberIdAndBookIdAndStatus(memberId, resolvedBookId, "Pending");
            canReserve = book.getAvailableCopies() == 0 && reservationsEnabled && !hasPendingReservation;
        } catch (Exception e) {
            System.err.println("Warning: Failed to check reservations for book " + resolvedBookId + ": " + e.getMessage());
        }

        // Safely fetch similar books
        List<Map<String, Object>> similarBooks = new ArrayList<>();
        try {
            similarBooks = fetchSimilarBooks(book);
        } catch (Exception e) {
            System.err.println("Warning: Failed to fetch similar books for " + resolvedBookId + ": " + e.getMessage());
            similarBooks = new ArrayList<>();
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("book", book);
        response.put("authors", authors);
        response.put("categories", categories);
        response.put("topReviews", topReviews);
        response.put("isWishlisted", isWishlisted);
        response.put("canReserve", canReserve);
        response.put("similarBooks", similarBooks);
        return response;
    }



public double getAverageRating(String bookId) {
        Aggregation avgAgg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("bookId").is(bookId)),
            Aggregation.group().avg("rating").as("averageRating")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(avgAgg, BookReview.class, Document.class);
        return results.getMappedResults().stream()
            .mapToDouble(doc -> doc.containsKey("averageRating") ? doc.getDouble("averageRating") : 0.0)
            .findFirst().orElse(0.0);
    }

    public int getTotalRatings(String bookId) {
        return (int) bookReviewRepository.countByBookId(bookId);
    }

    public Map<String, Object> voteReviewHelpful(String memberId, String bookId, String reviewId) {
        BookReview review = bookReviewRepository.findById(reviewId)
                .orElseThrow(() -> new ResourceNotFoundException("Review not found"));
        if (!bookId.equals(review.getBookId())) {
            throw new IllegalArgumentException("Review does not belong to this book");
        }
        if (memberId.equals(review.getMemberId())) {
            throw new IllegalArgumentException("You cannot upvote your own review");
        }
        LocalDateTime now = LocalDateTime.now();
        review.setHelpfulVotes(review.getHelpfulVotes() + 1);
        review.setUpdatedAt(now);
        review.setUpdatedBy(memberId);
        bookReviewRepository.save(review);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reviewId", review.getId());
        response.put("helpfulVotes", review.getHelpfulVotes());
        response.put("averageRating", getAverageRating(bookId));
        response.put("totalRatings", getTotalRatings(bookId));
        return response;
    }

    public Map<String, Object> getFeaturedBooks() {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("topRated", bookRepository.findTop6ByIsDeletedFalseOrderByAverageRatingDesc().stream().map(this::toBookResponse).toList());
        response.put("trending", genreAnalyticsRepository.findTop3ByOrderByTrendScoreDesc());
        response.put("newArrivals", bookRepository.findTop6ByIsDeletedFalseOrderByCreatedAtDesc().stream().map(this::toBookResponse).toList());
        return response;
    }

    public List<BookCategory> getBookCategories() {
        List<BookCategory> categories = bookCategoryRepository.findAllByOrderByNameAsc();
        // Dynamically populate book counts
        for (BookCategory category : categories) {
            long count = bookRepository.countByCategoryIdsContainingAndIsDeletedFalse(category.getId());
            category.setBookCount((int) count);
        }
        return categories;
    }

    public Book getActiveBook(String bookId) {
        String identifier = bookId == null ? "" : bookId.trim();
        
        // Try URL decoding in case the ID is URL-encoded
        try {
            String decodedIdentifier = java.net.URLDecoder.decode(identifier, "UTF-8");
            if (!decodedIdentifier.equals(identifier)) {
                // If decoding changed the string, also try the decoded version
                Optional<Book> book = findBookByAnyIdentifier(decodedIdentifier);
                if (book.isPresent() && !book.get().isDeleted()) {
                    return book.get();
                }
            }
        } catch (Exception e) {
            // If decoding fails, continue with original identifier
        }
        
        Book book = findBookByAnyIdentifier(identifier)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found with identifier: " + identifier));
        if (book.isDeleted()) {
            throw new ResourceNotFoundException("Book is no longer available");
        }
        return book;
    }

    private Optional<Book> findBookByAnyIdentifier(String identifier) {
        if (identifier == null || identifier.isBlank()) {
            return Optional.empty();
        }

        // 1. Try direct ID lookup (MongoDB _id as String)
        Optional<Book> byId = bookRepository.findById(identifier);
        if (byId.isPresent()) {
            return byId;
        }

        // 2. Try ObjectId conversion if it's a valid hex string
        if (ObjectId.isValid(identifier)) {
            Book byObjectId = mongoTemplate.findOne(new Query(Criteria.where("_id").is(new ObjectId(identifier))), Book.class);
            if (byObjectId != null) {
                return Optional.of(byObjectId);
            }
        }

        // 3. Try ISBN lookup
        Optional<Book> byIsbn = bookRepository.findByIsbn(identifier);
        if (byIsbn.isPresent()) {
            return byIsbn;
        }

        // 4. Try case-insensitive ISBN normalization
        String normalizedIdentifier = normalizeIsbn(identifier);
        if (!normalizedIdentifier.isBlank()) {
            Optional<Book> byNormalizedIsbn = mongoTemplate.findAll(Book.class).stream()
                    .filter(book -> {
                        String bookNormalized = normalizeIsbn(book.getIsbn());
                        return bookNormalized != null && bookNormalized.equalsIgnoreCase(normalizedIdentifier);
                    })
                    .findFirst();
            if (byNormalizedIsbn.isPresent()) {
                return byNormalizedIsbn;
            }
        }

        // 5. Try legacy ID lookup (case-insensitive) - bookId, id, or title
        Query legacyIdQuery = new Query(new Criteria().orOperator(
                Criteria.where("bookId").regex("^" + Pattern.quote(identifier) + "$", "i"),
                Criteria.where("id").regex("^" + Pattern.quote(identifier) + "$", "i"),
                Criteria.where("title").regex("^" + Pattern.quote(identifier) + "$", "i")
        ));
        Optional<Book> byLegacyId = Optional.ofNullable(mongoTemplate.findOne(legacyIdQuery, Book.class));
        if (byLegacyId.isPresent()) {
            return byLegacyId;
        }

        // 6. Try to find by partial ISBN match (case-insensitive, for mismatched formatting)
        Optional<Book> byPartialIsbn = mongoTemplate.findAll(Book.class).stream()
                .filter(book -> {
                    String bookIsbn = book.getIsbn();
                    if (bookIsbn == null) return false;
                    // Remove hyphens and spaces for comparison
                    String bookIsbnClean = bookIsbn.replaceAll("[\\s\\-]", "");
                    String identifierClean = identifier.replaceAll("[\\s\\-]", "");
                    return bookIsbnClean.equalsIgnoreCase(identifierClean);
                })
                .findFirst();
        if (byPartialIsbn.isPresent()) {
            return byPartialIsbn;
        }

        return Optional.empty();
    }

    private List<Map<String, Object>> fetchSimilarBooks(Book book) {
        try {
            List<Double> vector = bookEmbeddingRepository.findByBookId(book.getId())
                    .map(BookEmbedding::getEmbedding)
                    .orElse(book.getEmbedding());
            if (vector == null || vector.isEmpty()) {
                return fallbackSimilarBooks(book);
            }

            Aggregation aggregation = Aggregation.newAggregation(context -> new Document("$vectorSearch",
                    new Document("index", "book_embedding_index")
                            .append("path", "embedding")
                            .append("queryVector", vector)
                            .append("numCandidates", 20)
                            .append("limit", 5)
            ));

            List<Document> documents;
            try {
                documents = mongoTemplate.aggregate(aggregation, "book_embeddings", Document.class).getMappedResults();
            } catch (Exception ex) {
                System.err.println("Vector search failed for book " + book.getId() + ": " + ex.getMessage());
                return fallbackSimilarBooks(book);
            }
            
            List<String> bookIds = new ArrayList<>();
            for (Document document : documents) {
                String bookId = document.getString("bookId");
                if (bookId != null && !bookId.equals(book.getId())) {
                    bookIds.add(bookId);
                }
            }
            
            LinkedHashSet<String> distinctIds = new LinkedHashSet<>(bookIds);
            List<Map<String, Object>> similarBooks = new ArrayList<>();
            for (String similarBookId : distinctIds.stream().limit(4).toList()) {
                try {
                    bookRepository.findById(similarBookId)
                            .filter(found -> !found.isDeleted())
                            .ifPresent(found -> similarBooks.add(similarBookSummary(found)));
                } catch (Exception e) {
                    System.err.println("Failed to fetch similar book " + similarBookId + ": " + e.getMessage());
                }
            }
            return similarBooks;
        } catch (Exception e) {
            System.err.println("Error in fetchSimilarBooks: " + e.getMessage());
            return fallbackSimilarBooks(book);
        }
    }

    private List<Map<String, Object>> fallbackSimilarBooks(Book book) {
        try {
            if (book.getCategoryIds() == null || book.getCategoryIds().isEmpty()) {
                return new ArrayList<>();
            }
            
            String firstCategory = book.getCategoryIds().getFirst();
            if (firstCategory == null || firstCategory.isEmpty()) {
                return new ArrayList<>();
            }
            
            List<Book> similarByCategory = bookRepository.findByCategoryIdsContainingAndIsDeletedFalse(firstCategory);
            if (similarByCategory == null) {
                return new ArrayList<>();
            }
            
            return similarByCategory.stream()
                    .filter(candidate -> candidate != null && !candidate.getId().equals(book.getId()))
                    .limit(4)
                    .map(this::similarBookSummary)
                    .toList();
        } catch (Exception e) {
            System.err.println("Fallback similar books failed: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private Map<String, Object> similarBookSummary(Book book) {
        Map<String, Object> item = new LinkedHashMap<>();
        item.put("bookId", book.getId());
        item.put("title", book.getTitle());
        item.put("coverImageUrl", book.getCoverImageUrl());
        item.put("averageRating", book.getAverageRating());
        item.put("availableCopies", book.getAvailableCopies());
        return item;
    }

    private BookResponse toBookResponse(Book book) {
        String authorNames = authorRepository.findAllById(book.getAuthorIds() == null ? new ArrayList<>() : book.getAuthorIds())
                .stream()
                .map(Author::getName)
                .collect(Collectors.joining(", "));
        
        // Properly fetch category name like browseBooks method does
        String categoryName = "Uncategorized";
        if (book.getCategoryIds() != null && !book.getCategoryIds().isEmpty()) {
            String categoryId = book.getCategoryIds().getFirst();
            try {
                BookCategory category = bookCategoryRepository.findById(categoryId).orElse(null);
                if (category != null) {
                    categoryName = category.getName();
                }
            } catch (Exception e) {
                // Ignore repo errors and keep "Uncategorized"
            }
        }

        return BookResponse.builder()
                .bookId(book.getId())
                .isbn(book.getIsbn())
                .title(book.getTitle())
                .authorIds(book.getAuthorIds() == null ? new ArrayList<>() : new ArrayList<>(book.getAuthorIds()))
                .author(authorNames)
                .category(categoryName)
                .genre(categoryName)
                .categoryIds(book.getCategoryIds() == null ? new ArrayList<>() : new ArrayList<>(book.getCategoryIds()))
                .coverImageUrl(book.getCoverImageUrl())
                .coverImageThumbnailUrl(book.getCoverImageUrl())
                .hasCoverImage(book.isHasCoverImage())
                .availableCopies(book.getAvailableCopies())
                .totalCopies(book.getTotalCopies())
                .averageRating(book.getAverageRating())
                .totalRatings(book.getTotalRatings())
                .description(book.getDescription())
                .pageCount(book.getPageCount())
                .publishedYear(book.getPublishedYear())
                .language(book.getLanguage())
                .isDeleted(book.isDeleted())
                .createdAt(book.getCreatedAt())
                .build();
    }

private String primaryCategoryName(List<String> categoryIds, Map<String, String> categoryNameById) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "Uncategorized";
        }
        String id = categoryIds.getFirst();
        if (categoryNameById != null && categoryNameById.containsKey(id)) {
            return categoryNameById.get(id);
        }
        try {
            BookCategory cat = bookCategoryRepository.findById(id).orElse(null);
            if (cat != null) {
                return cat.getName();
            }
        } catch (Exception e) {
            // Ignore repo errors
        }
        return "Uncategorized";
    }

    private String joinAuthorNames(List<String> authorIds, Map<String, String> authorNameById) {
        if (authorIds == null || authorIds.isEmpty()) return "";
        if (authorNameById == null || authorNameById.isEmpty()) {
            return authorRepository.findAllById(authorIds).stream().map(Author::getName).collect(Collectors.joining(", "));
        }
        return authorIds.stream()
                .map((id) -> authorNameById.getOrDefault(id, id))
                .collect(Collectors.joining(", "));
    }

    private Map<String, Object> pageResponse(String key, Object values, long total, int page, int limit) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put(key, values);
        response.put("total", total);
        response.put("page", page);
        response.put("pages", (int) Math.ceil((double) total / limit));
        return response;
    }

    private Map<String, Object> bookSnapshot(Book book) {
        Map<String, Object> snapshot = new LinkedHashMap<>();
        snapshot.put("title", book.getTitle());
        snapshot.put("description", book.getDescription());
        snapshot.put("totalCopies", book.getTotalCopies());
        snapshot.put("availableCopies", book.getAvailableCopies());
        snapshot.put("tags", book.getTags());
        snapshot.put("coverImageUrl", book.getCoverImageUrl());
        return snapshot;
    }

    private String detectImageFormat(String imageUrl) {
        if (imageUrl == null || imageUrl.isBlank()) {
            return "";
        }
        if (imageUrl.endsWith(".png")) {
            return "png";
        }
        if (imageUrl.endsWith(".webp")) {
            return "webp";
        }
        return "jpg";
    }

    /**
     * Users sometimes paste "image search result" URLs (Bing/Google) instead of direct image URLs.
     * Try to extract a usable direct image link when wrapped in query params like "mediaurl" or "imgurl".
     */
    private String normalizeCoverImageUrl(String rawUrl) {
        if (rawUrl == null) return "";
        String trimmed = rawUrl.trim();
        if (trimmed.isBlank()) return "";

        try {
            URI uri = URI.create(trimmed);
            String query = uri.getRawQuery();
            if (query == null || query.isBlank()) {
                return trimmed;
            }

            String extracted = extractQueryParam(query, "mediaurl");
            if (extracted == null) extracted = extractQueryParam(query, "imgurl");
            if (extracted == null) extracted = extractQueryParam(query, "url");

            return extracted == null ? trimmed : extracted;
        } catch (IllegalArgumentException e) {
            return trimmed;
        }
    }

    private String extractQueryParam(String rawQuery, String key) {
        String[] pairs = rawQuery.split("&");
        for (String pair : pairs) {
            int idx = pair.indexOf('=');
            if (idx <= 0) continue;
            String k = pair.substring(0, idx);
            if (!k.equalsIgnoreCase(key)) continue;
            String v = pair.substring(idx + 1);
            if (v.isBlank()) return null;
            return URLDecoder.decode(v, StandardCharsets.UTF_8);
        }
        return null;
    }

    private Map<String, Object> deletionChanges(Map<String, Object> before, String reason) {
        Map<String, Object> after = new LinkedHashMap<>();
        after.put("isDeleted", true);
        after.put("reason", reason);
        Map<String, Object> changes = new LinkedHashMap<>();
        changes.put("before", before);
        changes.put("after", after);
        return changes;
    }
}
