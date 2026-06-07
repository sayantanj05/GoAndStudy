package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationResponse {

    @Builder.Default
    private List<RecommendationItem> recommendations = new ArrayList<>();
    @Builder.Default
    private List<RecommendationItem> similarBooks = new ArrayList<>();
    private String aiResponse;
    private String aiLogId;
    private String sessionId;
    private String basedOn;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationItem {
        private String bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private double matchScore;
        private String reason;
        private String genre;
        private int availableCopies;
    }
}

