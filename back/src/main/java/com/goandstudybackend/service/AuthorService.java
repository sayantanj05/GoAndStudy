package com.goandstudybackend.service;

import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.BookReview;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import com.goandstudybackend.exception.ResourceNotFoundException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.GroupOperation;
import org.springframework.data.mongodb.core.aggregation.MatchOperation;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Slf4j
public class AuthorService {

    private final AuthorRepository authorRepository;
    private final BookRepository bookRepository;
    private final BookReviewRepository bookReviewRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final MongoTemplate mongoTemplate;

    /**
     * Update stats for a single author: totalBooks, genres, averageRating
     */
    public void updateAuthorStats(String authorId) {
        Author author = authorRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + authorId));

        // totalBooks: count non-deleted books containing this authorId
        long totalBooks = bookRepository.countByAuthorIdsContainingAndIsDeletedFalse(authorId);
        author.setTotalBooks((int) totalBooks);

        // genres: unique categories from author's books
        Set<String> genres = new HashSet<>();
        List<Book> authorsBooks = bookRepository.findByAuthorIdsContainingAndIsDeletedFalseOrderByAverageRatingDesc(authorId);
        for (Book book : authorsBooks) {
            if (book.getCategoryIds() != null) {
                for (String catId : book.getCategoryIds()) {
                    BookCategory cat = bookCategoryRepository.findById(catId).orElse(null);
                    if (cat != null) {
                        genres.add(cat.getName());
                    }
                }
            }
        }
        author.setGenres(new ArrayList<>(genres));

        // averageRating: weighted average from book ratings
        double totalRating = 0;
        int totalRatingsWeight = 0;
        for (Book book : authorsBooks) {
            totalRating += book.getAverageRating() * book.getTotalRatings();
            totalRatingsWeight += book.getTotalRatings();
        }
        double avgRating = totalRatingsWeight > 0 ? totalRating / totalRatingsWeight : 0.0;
        author.setAverageRating(Math.round(avgRating * 100.0) / 100.0);  // 2 decimals

        author.setUpdatedAt(LocalDateTime.now());
        authorRepository.save(author);
        log.info("Updated stats for author {}: books={}, genres={}, rating={}", authorId, totalBooks, genres.size(), avgRating);
    }

    /**
     * Find existing or create new authors from name list
     */
    public List<String> findOrCreateAuthors(List<String> names) {
        List<String> authorIds = new ArrayList<>();
        for (String name : names.stream().filter(Objects::nonNull).map(String::trim).filter(s -> !s.isEmpty()).toList()) {
            List<Author> existingAuthors = authorRepository.findByNameContainingIgnoreCase(name);
            Optional<Author> existing = existingAuthors.stream().findFirst();
            existing.ifPresentOrElse(
                found -> authorIds.add(found.getId()),
                () -> {
                    String newId = generateNextAuthorId();
                    Author newAuthor = Author.builder()
                        .id(newId)
                        .name(name)
                        .bio("")
                        .nationality("")
                        .genres(new ArrayList<>())
                        .totalBooks(0)
                        .averageRating(0.0)
                        .createdAt(LocalDateTime.now())
                        .build();
                    Author saved = authorRepository.save(newAuthor);
                    authorIds.add(saved.getId());
                    log.info("Created new author: {} ({})", name, newId);
                }
            );
        }
        return authorIds;
    }

    private String generateNextAuthorId() {
        List<Author> allAuthors = authorRepository.findAll();
        int nextNum = allAuthors.stream()
            .map(Author::getId)
            .filter(Objects::nonNull)
            .filter(id -> id.startsWith("AUT"))
            .map(id -> {
                try {
                    return Integer.parseInt(id.substring(3));
                } catch (NumberFormatException e) {
                    return 0;
                }
            })
            .max(Integer::compareTo)
            .orElse(0) + 1;
        return String.format("AUT%03d", nextNum);
    }

    public Map<String, Object> getAuthorDetail(String authorId) {
        Author author = authorRepository.findById(authorId)
            .orElseThrow(() -> new ResourceNotFoundException("Author not found"));
        List<Book> books = bookRepository.findByAuthorIdsContainingAndIsDeletedFalseOrderByAverageRatingDesc(authorId);
        double overallAvg = books.stream().mapToDouble(Book::getAverageRating).average().orElse(0.0);

        Map<String, Object> response = new HashMap<>();
        response.put("author", author);
        response.put("books", books);
        response.put("overallAverageRating", overallAvg);
        return response;
    }

    /**
     * Update stats for multiple authors (e.g. after book op affecting many)
     */
    public void updateAuthorsStats(List<String> authorIds) {
        authorIds.forEach(this::updateAuthorStats);
    }

    /**
     * Create a new author
     */
    public Author createAuthor(String name, String bio, String nationality, List<String> genres) {
        String newId = generateNextAuthorId();
        Author author = Author.builder()
                .id(newId)
                .name(name)
                .bio(bio != null ? bio : "")
                .nationality(nationality != null ? nationality : "")
                .genres(genres != null ? genres : new ArrayList<>())
                .totalBooks(0)
                .averageRating(0.0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        Author saved = authorRepository.save(author);
        log.info("Created new author: {} ({})", name, newId);
        return saved;
    }

    /**
     * Update an existing author
     */
    public Author updateAuthor(String authorId, String name, String bio, String nationality, List<String> genres) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + authorId));
        
        if (name != null && !name.isEmpty()) author.setName(name);
        if (bio != null) author.setBio(bio);
        if (nationality != null) author.setNationality(nationality);
        if (genres != null) author.setGenres(genres);
        
        author.setUpdatedAt(LocalDateTime.now());
        Author saved = authorRepository.save(author);
        log.info("Updated author: {} ({})", name, authorId);
        return saved;
    }

    /**
     * Delete an author
     */
    public void deleteAuthor(String authorId) {
        Author author = authorRepository.findById(authorId)
                .orElseThrow(() -> new ResourceNotFoundException("Author not found: " + authorId));
        authorRepository.delete(author);
        log.info("Deleted author: {}", authorId);
    }
}

