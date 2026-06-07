package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "recommendation_logs")
public class RecommendationLog {

    @Id
    private String id;

    private String userId;
    private String sessionId;

    private List<RecommendationItem> topRecommendations;
    private List<RecommendationItem> similarRecommendations;

    private String modelVersion;
    private String experimentId;
    private LocalDateTime generatedAt;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationItem {
        private String bookId;
        private String title;
        private String author;
        private Double score;
        private Integer rank;
        private String source;
        private String reason;
    }
}
