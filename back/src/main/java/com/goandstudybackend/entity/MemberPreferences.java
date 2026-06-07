package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "member_preferences")
public class MemberPreferences {

    @Id
    private String id;
    @Builder.Default
    private List<Map<String, Object>> topGenres = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> topAuthors = new ArrayList<>();
    private double avgRatingGiven;
    private double readingSpeedProxy;
    private String preferredPageLength;
    @Builder.Default
    private List<Double> genreEmbedding = new ArrayList<>();
    @Builder.Default
    private List<String> recentKeywords = new ArrayList<>();
    @Builder.Default
    private List<String> wishlistGenres = new ArrayList<>();
    private double diversityScore;
    private LocalDateTime lastComputedAt;
}
