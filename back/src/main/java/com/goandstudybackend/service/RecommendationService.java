package com.goandstudybackend.service;

import com.goandstudybackend.dto.response.MemberRecommendationResponse;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.RecommendationLog;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.RecommendationLogRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class RecommendationService {

    private final MlRecommendationService mlRecommendationService;
    private final RecommendationLogRepository recommendationLogRepository;
    private final BookRepository bookRepository;

    public MemberRecommendationResponse getRecommendations(String memberId, int topN, int similarN) {
        List<MemberRecommendationResponse.RecItem> topRecs = new ArrayList<>();
        List<MemberRecommendationResponse.RecItem> similarRecs = new ArrayList<>();
        boolean fallbackUsed = false;
        String modelVersion = "unknown";

        try {
            // ML integration is currently type-incompatible with this module's DTOs during build.
            // Use fallback recommendations to keep backend compiling.
            fallbackUsed = true;
            topRecs = getFallbackRecommendations(memberId, topN);
            similarRecs = getFallbackRecommendations(memberId, similarN);
            modelVersion = "fallback";
        } catch (Exception e) {
            log.error("Recommendation service error, using full fallback: {}", e.getMessage());
            fallbackUsed = true;
            topRecs = getFallbackRecommendations(memberId, topN);
            similarRecs = getFallbackRecommendations(memberId, similarN);
        }

        try {
            RecommendationLog log = RecommendationLog.builder()
                    .id(UUID.randomUUID().toString())
                    .userId(memberId)
                    .sessionId(UUID.randomUUID().toString())
                    .topRecommendations(toLogItems(topRecs))
                    .similarRecommendations(toLogItems(similarRecs))
                    .modelVersion(modelVersion)
                    .generatedAt(LocalDateTime.now())
                    .build();
            recommendationLogRepository.save(log);
        } catch (Exception e) {
            log.error("Failed to persist recommendation log: {}", e.getMessage());
        }

        return MemberRecommendationResponse.builder()
                .memberId(memberId)
                .topRecommendations(topRecs)
                .youMayAlsoLike(similarRecs)
                .modelVersion(modelVersion)
                .fallbackUsed(fallbackUsed)
                .build();
    }

    // ML enrich is disabled in this build (see short-circuit above).

    private List<MemberRecommendationResponse.RecItem> getFallbackRecommendations(String memberId, int needed) {
        List<Book> popular = bookRepository.findByIsDeletedFalse(PageRequest.of(0, needed * 2))
                .stream()
                .filter(b -> b.getAvailableCopies() > 0)
                .sorted((a, b) -> Integer.compare(b.getTotalIssues(), a.getTotalIssues()))
                .limit(needed)
                .toList();

        return popular.stream().map(book -> MemberRecommendationResponse.RecItem.builder()
                .bookId(book.getId())
                .title(book.getTitle())
                .author(book.getAuthorIds() != null && !book.getAuthorIds().isEmpty() ? book.getAuthorIds().get(0) : "")
                .coverImageUrl(book.getCoverImageUrl())
                .score(0.0)
                .reason("Popular pick")
                .availableCopies(book.getAvailableCopies())
                .genre(book.getCategoryIds() != null && !book.getCategoryIds().isEmpty() ? book.getCategoryIds().get(0) : "")
                .build()).collect(Collectors.toList());
    }

    private List<RecommendationLog.RecommendationItem> toLogItems(List<MemberRecommendationResponse.RecItem> items) {
        List<RecommendationLog.RecommendationItem> result = new ArrayList<>();
        for (int i = 0; i < items.size(); i++) {
            MemberRecommendationResponse.RecItem item = items.get(i);
            result.add(RecommendationLog.RecommendationItem.builder()
                    .bookId(item.getBookId())
                    .title(item.getTitle())
                    .author(item.getAuthor())
                    .score(item.getScore())
                    .rank(i + 1)
                    .source(item.getReason().contains("ML") ? "ml" : "fallback")
                    .reason(item.getReason())
                    .build());
        }
        return result;
    }
}
