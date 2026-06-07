package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.AiFeedbackRequest;
import com.goandstudybackend.dto.request.AiRecommendationRequest;
import com.goandstudybackend.dto.response.AiRecommendationResponse;
import com.goandstudybackend.dto.response.MemberRecommendationResponse;
import com.goandstudybackend.entity.AiLog;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.MemberPreferences;
import com.goandstudybackend.entity.RecommendationFeedback;
import com.goandstudybackend.entity.Wishlist;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.AiLogRepository;
import com.goandstudybackend.repository.BookEmbeddingRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberPreferencesRepository;
import com.goandstudybackend.repository.RecommendationFeedbackRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.bson.Document;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AiRecommendationService {

    private final MemberPreferencesRepository memberPreferencesRepository;
    private final LoanRepository loanRepository;
    private final WishlistRepository wishlistRepository;
    private final BookRepository bookRepository;
    private final BookEmbeddingRepository bookEmbeddingRepository;
    private final AiLogRepository aiLogRepository;
    private final RecommendationFeedbackRepository recommendationFeedbackRepository;
    private final NvidiaEmbeddingService nvidiaEmbeddingService;
    private final NvidiaRerankerService nvidiaRerankerService;
    private final NvidiaRecommendationService nvidiaRecommendationService;
    private final MlRecommendationService mlRecommendationService;
    private final ActivityLogService activityLogService;
    private final MongoTemplate mongoTemplate;

    public AiRecommendationResponse getAiRecommendations(String memberId, AiRecommendationRequest request) {
        String sessionId = request.getSessionId() == null || request.getSessionId().isBlank()
                ? UUID.randomUUID().toString()
                : request.getSessionId();

        MemberPreferences memberPreferences = memberPreferencesRepository.findById(memberId)
                .orElse(MemberPreferences.builder().id(memberId).build());
        List<String> recentReadBookIds = loanRepository.findByMemberIdAndStatusIn(memberId, List.of("RETURNED", "Returned"))
                .stream()
                .sorted((left, right) -> {
                    if (left.getReturnedAt() == null && right.getReturnedAt() == null) return 0;
                    if (left.getReturnedAt() == null) return 1;
                    if (right.getReturnedAt() == null) return -1;
                    return right.getReturnedAt().compareTo(left.getReturnedAt());
                })
                .limit(10)
                .map(loan -> loan.getBookId())
                .toList();
        List<Wishlist> wishlist = wishlistRepository.findByMemberIdOrderByAddedAtDesc(memberId);
        List<Double> queryVector = nvidiaEmbeddingService.generateEmbedding(request.getQuery());

        List<String> candidateBookIds = queryVector.isEmpty()
                ? bookRepository.findByIsDeletedFalse(PageRequest.of(0, 20)).stream().map(Book::getId).toList()
                : vectorSearchBookIds(queryVector, 30);
        List<Book> candidateBooks = candidateBookIds.stream()
                .map(id -> bookRepository.findById(id).orElse(null))
                .filter(book -> book != null && !book.isDeleted() && book.getAvailableCopies() > 0)
                .filter(book -> !Boolean.TRUE.equals(request.getExcludeRead()) || !recentReadBookIds.contains(book.getId()))
                .toList();

        if (candidateBooks.isEmpty()) {
            candidateBooks = bookRepository.findByIsDeletedFalse(PageRequest.of(0, 20)).stream()
                    .filter(book -> book.getAvailableCopies() > 0)
                    .sorted(Comparator.comparingInt(Book::getTotalIssues).reversed())
                    .toList();
        }

        List<String> passages = candidateBooks.stream()
                .map(book -> book.getTitle() + " " + truncate(book.getDescription(), 200))
                .toList();
        List<NvidiaRerankerService.BookRerankedResult> ranked = nvidiaRerankerService.rerank(request.getQuery(), passages);
        List<Book> rankedCandidates = candidateBooks;
        List<Book> topCandidateBooks = ranked.stream()
                .map(result -> rankedCandidates.get(Math.min(result.getIndex(), rankedCandidates.size() - 1)))
                .distinct()
                .limit(10)
                .toList();

        String topGenres = memberPreferences.getTopGenres().stream()
                .map(item -> String.valueOf(item.getOrDefault("genre", "")))
                .collect(Collectors.joining(", "));
        String recentTitles = recentReadBookIds.stream()
                .map(id -> bookRepository.findById(id).map(Book::getTitle).orElse(""))
                .filter(value -> !value.isBlank())
                .collect(Collectors.joining(", "));
        String wishlistTitles = wishlist.stream().map(Wishlist::getBookTitle).collect(Collectors.joining(", "));

        String prompt = """
                You are recommending books for a library member.
                Return ONLY a JSON array where each item has:
                bookId, title, author, coverImageUrl, matchScore, reason, genre, availableCopies.

                Member query: %s
                Top genres: %s
                Recently read titles: %s
                Wishlist titles: %s

                Candidate books:
                %s
                """.formatted(
                request.getQuery(),
                topGenres,
                recentTitles,
                wishlistTitles,
                topCandidateBooks.stream()
                        .map(book -> book.getId() + " | " + book.getTitle() + " | " + primaryGenre(book) + " | " + book.getAvailableCopies())
                        .collect(Collectors.joining("\n"))
        );

        NvidiaRecommendationService.RecommendationPayload payload = nvidiaRecommendationService.recommend(prompt);
        List<AiRecommendationResponse.RecommendationItem> recommendations = mergeRecommendations(payload.getRecommendations(), topCandidateBooks);
        if (recommendations.isEmpty()) {
            recommendations = topCandidateBooks.stream().limit(5).map(book -> AiRecommendationResponse.RecommendationItem.builder()
                    .bookId(book.getId())
                    .title(book.getTitle())
                    .author(book.getAuthorIds().isEmpty() ? "" : book.getAuthorIds().getFirst())
                    .coverImageUrl(book.getCoverImageUrl())
                    .matchScore(0.0)
                    .reason("Popular pick for new members")
                    .genre(primaryGenre(book))
                    .availableCopies(book.getAvailableCopies())
                    .build()).toList();
        }

        AiLog aiLog = aiLogRepository.save(AiLog.builder()
                .memberId(memberId)
                .sessionId(sessionId)
                .queryText(request.getQuery())
                .contextSnapshot(Map.of(
                        "topGenres", topGenres,
                        "recentTitles", recentTitles,
                        "wishlistTitles", wishlistTitles
                ))
                .modelUsed("meta/llama-3.1-8b-instruct")
                .systemPrompt(prompt)
                .responseText(payload.getRawText())
                .recommendedBookIds(recommendations.stream().map(AiRecommendationResponse.RecommendationItem::getBookId).toList())
                .vectorSearchQuery(new ArrayList<>(queryVector))
                .vectorSearchResults(new ArrayList<>(new LinkedHashSet<>(candidateBookIds)))
                .tokensUsed(payload.getTokensUsed())
                .latencyMs(payload.getLatencyMs())
                .wasActedOn(false)
                .createdAt(LocalDateTime.now())
                .build());

        activityLogService.log("AI_RECOMMENDATION", memberId, "ROLE_MEMBER", "ai_logs", aiLog.getId());

        return AiRecommendationResponse.builder()
                .recommendations(recommendations)
                .aiResponse(payload.getRawText())
                .aiLogId(aiLog.getId())
                .sessionId(sessionId)
                .basedOn("reading history + wishlist")
                .build();
    }

public AiRecommendationResponse getPrecomputedRecommendations(String memberId, int limit) {
        // 1. Try ML service (collaborative filtering) for up to 15 recommendations (5 top + 10 similar)
        MemberRecommendationResponse mlResponse =
                mlRecommendationService.getRecommendations(memberId, 15);
        List<AiRecommendationResponse.RecommendationItem> allRecs = convertMemberToAiRecommendations(mlResponse);

        // 2. Split into top 5 and similar 10
        List<AiRecommendationResponse.RecommendationItem> topRecs = new ArrayList<>();
        List<AiRecommendationResponse.RecommendationItem> similarRecs = new ArrayList<>();

        if (allRecs != null && !allRecs.isEmpty()) {
            int topCount = Math.min(5, allRecs.size());
            topRecs.addAll(allRecs.subList(0, topCount));
            if (allRecs.size() > topCount) {
                int similarCount = Math.min(10, allRecs.size() - topCount);
                similarRecs.addAll(allRecs.subList(topCount, topCount + similarCount));
            }
        }

        // 3. If topRecs < 5, fill from fallback (vector search / popularity)
        if (topRecs.size() < 5) {
            List<AiRecommendationResponse.RecommendationItem> fallback = getFallbackRecommendations(memberId, 5 - topRecs.size());
            topRecs.addAll(fallback);
        }

        // 4. If similarRecs is empty, fill with content-based similar or popular books
        if (similarRecs.isEmpty()) {
            similarRecs = getContentBasedSimilar(topRecs, 10);
        }

        return AiRecommendationResponse.builder()
                .recommendations(topRecs)
                .similarBooks(similarRecs)
                .basedOn("ml collaborative filtering + content similarity")
                .build();
    }

    /**
     * Fallback recommendations when ML service returns insufficient results.
     * Uses vector search on genre embeddings, falling back to popularity.
     */
    private List<AiRecommendationResponse.RecommendationItem> getFallbackRecommendations(String memberId, int needed) {
        MemberPreferences preferences = memberPreferencesRepository.findById(memberId)
                .orElse(MemberPreferences.builder().id(memberId).build());

        List<Book> fallbackBooks;
        if (preferences.getGenreEmbedding() != null && !preferences.getGenreEmbedding().isEmpty()) {
            // Try vector search first
            fallbackBooks = vectorSearchBookIds(preferences.getGenreEmbedding(), needed * 2).stream()
                    .map(id -> bookRepository.findById(id).orElse(null))
                    .filter(book -> book != null && !book.isDeleted())
                    .limit(needed)
                    .toList();
        } else {
            fallbackBooks = List.of();
        }

        // If vector search didn't return enough, fill with popular books
        if (fallbackBooks.size() < needed) {
            int remaining = needed - fallbackBooks.size();
            List<String> existingIds = fallbackBooks.stream().map(Book::getId).toList();
            List<Book> popular = bookRepository.findByIsDeletedFalse(PageRequest.of(0, remaining * 2)).stream()
                    .filter(book -> !existingIds.contains(book.getId()))
                    .sorted(Comparator.comparingInt(Book::getTotalIssues).reversed())
                    .limit(remaining)
                    .toList();
            List<Book> combined = new ArrayList<>(fallbackBooks);
            combined.addAll(popular);
            fallbackBooks = combined;
        }

        return fallbackBooks.stream().map(book -> AiRecommendationResponse.RecommendationItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthorIds().isEmpty() ? "" : book.getAuthorIds().getFirst())
                .coverImageUrl(book.getCoverImageUrl())
                .matchScore(0.0)
                .genre(primaryGenre(book))
                .availableCopies(book.getAvailableCopies())
                .reason("Popular in your genre")
                .build()).toList();
    }

    /**
     * Content-based similar books for the "You May Also Like" section.
     * Uses genre/category overlap with top recommendations.
     */
    private List<AiRecommendationResponse.RecommendationItem> getContentBasedSimilar(
            List<AiRecommendationResponse.RecommendationItem> topRecs, int needed) {
        // Collect genres from top recommendations
        List<String> topGenres = topRecs.stream()
                .map(AiRecommendationResponse.RecommendationItem::getGenre)
                .filter(g -> g != null && !g.isBlank())
                .distinct()
                .toList();

        List<String> topBookIds = topRecs.stream()
                .map(AiRecommendationResponse.RecommendationItem::getBookId)
                .toList();

        // Find books with matching genres, excluding already recommended
        List<Book> candidates = bookRepository.findByIsDeletedFalse(PageRequest.of(0, needed * 3)).stream()
                .filter(book -> !topBookIds.contains(book.getId()))
                .filter(book -> book.getCategoryIds() != null && 
                        book.getCategoryIds().stream().anyMatch(topGenres::contains))
                .sorted(Comparator.comparingInt(Book::getTotalIssues).reversed())
                .limit(needed)
                .toList();

        // If not enough genre matches, fill with popular books
        if (candidates.size() < needed) {
            int remaining = needed - candidates.size();
            List<String> existingIds = candidates.stream().map(Book::getId).toList();
            existingIds.addAll(topBookIds);
            List<Book> popular = bookRepository.findByIsDeletedFalse(PageRequest.of(0, remaining * 2)).stream()
                    .filter(book -> !existingIds.contains(book.getId()))
                    .sorted(Comparator.comparingInt(Book::getTotalIssues).reversed())
                    .limit(remaining)
                    .toList();
            List<Book> combined = new ArrayList<>(candidates);
            combined.addAll(popular);
            candidates = combined;
        }

        return candidates.stream().map(book -> AiRecommendationResponse.RecommendationItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthorIds().isEmpty() ? "" : book.getAuthorIds().getFirst())
                .coverImageUrl(book.getCoverImageUrl())
                .matchScore(0.0)
                .genre(primaryGenre(book))
                .availableCopies(book.getAvailableCopies())
                .reason("Readers also enjoyed")
                .build()).toList();
    }

    public Map<String, Object> submitAiFeedback(String memberId, AiFeedbackRequest request) {
        AiLog aiLog = aiLogRepository.findByIdAndMemberId(request.getAiLogId(), memberId)
                .orElseThrow(() -> new ResourceNotFoundException("AI log not found"));
        RecommendationFeedback feedback = recommendationFeedbackRepository.save(RecommendationFeedback.builder()
                .aiLogId(aiLog.getId())
                .memberId(memberId)
                .recommendedBookId(request.getRecommendedBookId())
                .actionTaken(request.getActionTaken())
                .actionAt(LocalDateTime.now())
                .createdAt(LocalDateTime.now())
                .build());
        if (!"none".equalsIgnoreCase(request.getActionTaken())) {
            aiLog.setWasActedOn(true);
            aiLogRepository.save(aiLog);
        }
        return Map.of("feedbackId", feedback.getId(), "recorded", true);
    }

    private List<String> vectorSearchBookIds(List<Double> queryVector, int limit) {
        Aggregation aggregation = Aggregation.newAggregation(context -> new Document("$vectorSearch",
                new Document("index", "book_embedding_index")
                        .append("path", "embedding")
                        .append("queryVector", queryVector)
                        .append("numCandidates", Math.max(20, limit * 2))
                        .append("limit", limit)
        ));
        List<Document> documents = mongoTemplate.aggregate(aggregation, "book_embeddings", Document.class).getMappedResults();
        List<String> bookIds = new ArrayList<>();
        for (Document document : documents) {
            if (document.getString("bookId") != null) {
                bookIds.add(document.getString("bookId"));
            }
        }
        return bookIds;
    }

    private List<AiRecommendationResponse.RecommendationItem> mergeRecommendations(
            List<AiRecommendationResponse.RecommendationItem> rawRecommendations,
            List<Book> candidates
    ) {
        Map<String, Book> byId = candidates.stream().collect(Collectors.toMap(Book::getId, book -> book, (left, right) -> left));
        List<AiRecommendationResponse.RecommendationItem> merged = new ArrayList<>();
        for (AiRecommendationResponse.RecommendationItem item : rawRecommendations) {
            if (item.getBookId() != null && byId.containsKey(item.getBookId())) {
                Book book = byId.get(item.getBookId());
                merged.add(fillRecommendation(item, book));
                continue;
            }
            Book match = candidates.stream()
                    .filter(book -> item.getTitle() != null && item.getTitle().equalsIgnoreCase(book.getTitle()))
                    .findFirst()
                    .orElse(null);
            if (match != null) {
                merged.add(fillRecommendation(item, match));
            }
        }
        return merged;
    }

    private AiRecommendationResponse.RecommendationItem fillRecommendation(AiRecommendationResponse.RecommendationItem item, Book book) {
        return AiRecommendationResponse.RecommendationItem.builder()
                .bookId(book.getId())
                .title(item.getTitle() == null || item.getTitle().isBlank() ? book.getTitle() : item.getTitle())
                .author(item.getAuthor() == null || item.getAuthor().isBlank() ? (book.getAuthorIds().isEmpty() ? "" : book.getAuthorIds().getFirst()) : item.getAuthor())
                .coverImageUrl(item.getCoverImageUrl() == null || item.getCoverImageUrl().isBlank() ? book.getCoverImageUrl() : item.getCoverImageUrl())
                .matchScore(item.getMatchScore())
                .reason(item.getReason())
                .genre(item.getGenre() == null || item.getGenre().isBlank() ? primaryGenre(book) : item.getGenre())
                .availableCopies(book.getAvailableCopies())
                .build();
    }

    private String truncate(String value, int maxLength) {
        if (value == null || value.length() <= maxLength) {
            return value == null ? "" : value;
        }
        return value.substring(0, maxLength);
    }

private String primaryGenre(Book book) {
        return book.getCategoryIds() == null || book.getCategoryIds().isEmpty() ? "" : book.getCategoryIds().getFirst();
    }

    /**
     * Converts MemberRecommendationResponse to List of AiRecommendationResponse.RecommendationItem.
     */
    private List<AiRecommendationResponse.RecommendationItem> convertMemberToAiRecommendations(
            MemberRecommendationResponse memberRecommendations
    ) {
        if (memberRecommendations == null) {
            return new ArrayList<>();
        }

        List<MemberRecommendationResponse.RecItem> combined = new ArrayList<>();
        if (memberRecommendations.getTopRecommendations() != null) {
            combined.addAll(memberRecommendations.getTopRecommendations());
        }
        if (memberRecommendations.getYouMayAlsoLike() != null) {
            combined.addAll(memberRecommendations.getYouMayAlsoLike());
        }

        return combined.stream().map(rec -> {
            Book book = null;
            try {
                book = rec.getBookId() == null ? null : bookRepository.findById(rec.getBookId()).orElse(null);
            } catch (Exception ignored) {}

            return AiRecommendationResponse.RecommendationItem.builder()
                    .bookId(rec.getBookId())
                    .title(book != null ? book.getTitle() : rec.getTitle())
                    .author(book != null && !book.getAuthorIds().isEmpty() ? book.getAuthorIds().getFirst() : rec.getAuthor())
                    .coverImageUrl(book != null ? book.getCoverImageUrl() : rec.getCoverImageUrl())
                    .matchScore(rec.getScore())
                    .reason("Recommended for you")
                    .genre(book != null && book.getCategoryIds() != null && !book.getCategoryIds().isEmpty()
                            ? book.getCategoryIds().getFirst()
                            : rec.getGenre())
                    .availableCopies(book != null ? book.getAvailableCopies() : rec.getAvailableCopies())
                    .build();
        }).toList();
    }
}
