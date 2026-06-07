package com.goandstudybackend.service;

import com.goandstudybackend.dto.response.MemberRecommendationResponse;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.MemberPreferences;
import com.goandstudybackend.entity.RecommendationFeedback;
import com.goandstudybackend.entity.Wishlist;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberPreferencesRepository;
import com.goandstudybackend.repository.RecommendationFeedbackRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class MlRecommendationService {

    private static final List<String> READ_STATUSES = List.of("RETURNED", "Returned");
    private static final List<String> ACTIVE_STATUSES = List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Issued", "Renewed", "Overdue");

    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final WishlistRepository wishlistRepository;
    private final MemberPreferencesRepository memberPreferencesRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;

    public MemberRecommendationResponse getRecommendations(String memberId, int topN, int similarN) {
        long startedAt = System.currentTimeMillis();
        RecommendationContext context = buildContext(memberId);
        List<BookScore> ranked = rankBooks(context);

        List<MemberRecommendationResponse.RecommendationItem> top = ranked.stream()
                .limit(Math.max(1, topN))
                .map(item -> toRecommendationItem(item, context, false))
                .toList();

        Set<String> topIds = top.stream().map(MemberRecommendationResponse.RecommendationItem::getBookId).collect(Collectors.toSet());
        List<MemberRecommendationResponse.RecommendationItem> similar = ranked.stream()
                .filter(item -> !topIds.contains(item.book.getId()))
                .limit(Math.max(1, similarN))
                .map(item -> toRecommendationItem(item, context, true))
                .toList();

        return MemberRecommendationResponse.builder()
                .memberId(memberId)
                .top(top)
                .similar(similar)
                .topRecommendations(top.stream().map(this::toRecItem).toList())
                .youMayAlsoLike(similar.stream().map(this::toRecItem).toList())
                .modelVersion("local-hybrid-v1")
                .model_type("content+history+wishlist")
                .branch("personalized")
                .fallbackToPopular(context.isColdStart)
                .fallbackUsed(context.isColdStart)
                .latencyMs(System.currentTimeMillis() - startedAt)
                .build();
    }

    public MemberRecommendationResponse getRecommendations(String memberId, int limit) {
        int topN = Math.min(5, Math.max(1, limit));
        int similarN = Math.max(0, limit - topN);
        return getRecommendations(memberId, topN, Math.max(1, similarN));
    }

    public void submitFeedback(String memberId, String bookId, String feedback) {
        recommendationFeedbackRepository.save(RecommendationFeedback.builder()
                .memberId(memberId)
                .recommendedBookId(bookId)
                .actionTaken(feedback)
                .actionAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
    }

    private RecommendationContext buildContext(String memberId) {
        List<Loan> readLoans = loanRepository.findByMemberIdAndStatusIn(memberId, READ_STATUSES);
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatusIn(memberId, ACTIVE_STATUSES);
        List<Wishlist> wishlist = wishlistRepository.findByMemberIdOrderByAddedAtDesc(memberId);
        MemberPreferences preferences = memberPreferencesRepository.findById(memberId).orElse(null);

        Map<String, Double> categoryWeights = new HashMap<>();
        Set<String> excludedBookIds = new HashSet<>();
        Set<String> wishedBookIds = wishlist.stream().map(Wishlist::getBookId).collect(Collectors.toSet());

        for (Loan loan : readLoans) {
            excludedBookIds.add(loan.getBookId());
            applyBookCategoryWeight(loan.getBookId(), categoryWeights, 3.0);
        }
        for (Loan loan : activeLoans) {
            excludedBookIds.add(loan.getBookId());
            applyBookCategoryWeight(loan.getBookId(), categoryWeights, 2.0);
        }
        for (Wishlist item : wishlist) {
            applyBookCategoryWeight(item.getBookId(), categoryWeights, 1.5);
        }
        if (preferences != null && preferences.getTopGenres() != null) {
            for (Map<String, Object> genre : preferences.getTopGenres()) {
                String label = String.valueOf(genre.getOrDefault("genre", "")).trim();
                if (!label.isBlank()) {
                    categoryWeights.merge(label, 1.0, Double::sum);
                }
            }
        }

        boolean coldStart = categoryWeights.isEmpty() && wishedBookIds.isEmpty();
        return new RecommendationContext(memberId, categoryWeights, excludedBookIds, wishedBookIds, coldStart);
    }

    private List<BookScore> rankBooks(RecommendationContext context) {
        List<Book> books = bookRepository.findByIsDeletedFalse(PageRequest.of(0, 250)).stream()
                .filter(book -> book.getId() != null)
                .filter(book -> !context.excludedBookIds.contains(book.getId()))
                .toList();

        return books.stream()
                .map(book -> scoreBook(book, context))
                .sorted(Comparator.comparingDouble(BookScore::score).reversed()
                        .thenComparing(item -> item.book.getTitle(), String.CASE_INSENSITIVE_ORDER))
                .toList();
    }

    private BookScore scoreBook(Book book, RecommendationContext context) {
        double score = Math.min(0.35, Math.max(0, book.getAverageRating()) / 5.0 * 0.20);
        score += Math.min(0.25, Math.log1p(Math.max(0, book.getTotalIssues())) / 10.0);
        if (book.getAvailableCopies() > 0) score += 0.10;
        if (context.wishedBookIds.contains(book.getId())) score += 0.15;

        List<String> categoryLabels = categoryLabels(book);
        for (String categoryId : book.getCategoryIds() == null ? List.<String>of() : book.getCategoryIds()) {
            score += context.categoryWeights.getOrDefault(categoryId, 0.0) * 0.10;
        }
        for (String label : categoryLabels) {
            score += context.categoryWeights.getOrDefault(label, 0.0) * 0.10;
        }

        if (context.isColdStart) {
            score += Math.min(0.40, Math.log1p(Math.max(0, book.getTotalIssues())) / 5.0);
        }
        return new BookScore(book, Math.min(0.99, score), categoryLabels);
    }

    private void applyBookCategoryWeight(String bookId, Map<String, Double> categoryWeights, double weight) {
        if (bookId == null || bookId.isBlank()) return;
        bookRepository.findById(bookId).ifPresent(book -> {
            if (book.getCategoryIds() != null) {
                for (String categoryId : book.getCategoryIds()) {
                    categoryWeights.merge(categoryId, weight, Double::sum);
                    bookCategoryRepository.findById(categoryId)
                            .map(BookCategory::getName)
                            .ifPresent(name -> categoryWeights.merge(name, weight, Double::sum));
                }
            }
        });
    }

    private MemberRecommendationResponse.RecommendationItem toRecommendationItem(BookScore scored, RecommendationContext context, boolean similar) {
        Book book = scored.book;
        return MemberRecommendationResponse.RecommendationItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(authorName(book))
                .coverImageUrl(book.getCoverImageUrl())
                .score(scored.score)
                .reason(reasonFor(scored, context, similar))
                .genre(scored.categoryLabels.isEmpty() ? "" : scored.categoryLabels.get(0))
                .availableCopies(book.getAvailableCopies())
                .twoTowerScore(scored.score)
                .rankerScore(scored.score)
                .isExploration(context.isColdStart)
                .build();
    }

    private MemberRecommendationResponse.RecItem toRecItem(MemberRecommendationResponse.RecommendationItem item) {
        return MemberRecommendationResponse.RecItem.builder()
                .bookId(item.getBookId())
                .title(item.getTitle())
                .author(item.getAuthor())
                .coverImageUrl(item.getCoverImageUrl())
                .score(item.getScore())
                .reason(item.getReason())
                .genre(item.getGenre())
                .availableCopies(item.getAvailableCopies())
                .build();
    }

    private String reasonFor(BookScore scored, RecommendationContext context, boolean similar) {
        if (context.isColdStart) return "Popular with GoAndStudy readers";
        for (String label : scored.categoryLabels) {
            if (context.categoryWeights.containsKey(label)) {
                return similar ? "Similar to your preferred " + label + " books" : "Matches your " + label + " reading pattern";
            }
        }
        return similar ? "Readers with nearby tastes enjoyed this" : "Balanced pick from your library activity";
    }

    private String authorName(Book book) {
        if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) return "";
        return authorRepository.findById(book.getAuthorIds().get(0)).map(author -> author.getName()).orElse(book.getAuthorIds().get(0));
    }

    private List<String> categoryLabels(Book book) {
        if (book.getCategoryIds() == null || book.getCategoryIds().isEmpty()) return List.of();
        LinkedHashSet<String> labels = new LinkedHashSet<>();
        for (String categoryId : book.getCategoryIds()) {
            bookCategoryRepository.findById(categoryId).map(BookCategory::getName).ifPresent(labels::add);
        }
        return new ArrayList<>(labels);
    }

    private record RecommendationContext(
            String memberId,
            Map<String, Double> categoryWeights,
            Set<String> excludedBookIds,
            Set<String> wishedBookIds,
            boolean isColdStart
    ) {}

    private record BookScore(Book book, double score, List<String> categoryLabels) {}
}
