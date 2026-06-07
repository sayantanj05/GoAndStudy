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
public class MemberRecommendationResponse {

    private String memberId;

    @Builder.Default
    private List<RecommendationItem> top = new ArrayList<>();

    @Builder.Default
    private List<RecommendationItem> similar = new ArrayList<>();

    @Builder.Default
    private List<RecItem> topRecommendations = new ArrayList<>();

    @Builder.Default
    private List<RecItem> youMayAlsoLike = new ArrayList<>();

    private String modelVersion;
    private String model_type;
    private String branch;
    private boolean servedFromCache;
    private boolean fallbackToPopular;
    private boolean fallbackUsed;
    private long latencyMs;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationItem {
        private String bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private double score;
        private String reason;
        private String genre;
        private int availableCopies;
        private double twoTowerScore;
        private double rankerScore;
        private boolean isExploration;
        
        public static RecommendationItemBuilder builder() {
            return new RecommendationItemBuilder();
        }
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecItem {
        private String bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private double score;
        private String reason;
        private String genre;
        private int availableCopies;
        
        public static RecItemBuilder builder() {
            return new RecItemBuilder();
        }
    }
    
    // Manual builder pattern to fix compilation issues
    public static MemberRecommendationResponseBuilder builder() {
        return new MemberRecommendationResponseBuilder();
    }
    
    public static class MemberRecommendationResponseBuilder {
        private String memberId;
        private List<RecommendationItem> top = new ArrayList<>();
        private List<RecommendationItem> similar = new ArrayList<>();
        private List<RecItem> topRecommendations = new ArrayList<>();
        private List<RecItem> youMayAlsoLike = new ArrayList<>();
        private String modelVersion;
        private String model_type;
        private String branch;
        private boolean servedFromCache;
        private boolean fallbackToPopular;
        private boolean fallbackUsed;
        private long latencyMs;
        
        public MemberRecommendationResponseBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public MemberRecommendationResponseBuilder top(List<RecommendationItem> top) {
            this.top = top;
            return this;
        }
        
        public MemberRecommendationResponseBuilder similar(List<RecommendationItem> similar) {
            this.similar = similar;
            return this;
        }
        
        public MemberRecommendationResponseBuilder topRecommendations(List<RecItem> topRecommendations) {
            this.topRecommendations = topRecommendations;
            return this;
        }
        
        public MemberRecommendationResponseBuilder youMayAlsoLike(List<RecItem> youMayAlsoLike) {
            this.youMayAlsoLike = youMayAlsoLike;
            return this;
        }
        
        public MemberRecommendationResponseBuilder modelVersion(String modelVersion) {
            this.modelVersion = modelVersion;
            return this;
        }
        
        public MemberRecommendationResponseBuilder model_type(String model_type) {
            this.model_type = model_type;
            return this;
        }
        
        public MemberRecommendationResponseBuilder branch(String branch) {
            this.branch = branch;
            return this;
        }
        
        public MemberRecommendationResponseBuilder servedFromCache(boolean servedFromCache) {
            this.servedFromCache = servedFromCache;
            return this;
        }
        
        public MemberRecommendationResponseBuilder fallbackToPopular(boolean fallbackToPopular) {
            this.fallbackToPopular = fallbackToPopular;
            return this;
        }
        
        public MemberRecommendationResponseBuilder fallbackUsed(boolean fallbackUsed) {
            this.fallbackUsed = fallbackUsed;
            return this;
        }
        
        public MemberRecommendationResponseBuilder latencyMs(long latencyMs) {
            this.latencyMs = latencyMs;
            return this;
        }
        
        public MemberRecommendationResponse build() {
            MemberRecommendationResponse response = new MemberRecommendationResponse();
            response.memberId = this.memberId;
            response.top = this.top;
            response.similar = this.similar;
            response.topRecommendations = this.topRecommendations;
            response.youMayAlsoLike = this.youMayAlsoLike;
            response.modelVersion = this.modelVersion;
            response.model_type = this.model_type;
            response.branch = this.branch;
            response.servedFromCache = this.servedFromCache;
            response.fallbackToPopular = this.fallbackToPopular;
            response.fallbackUsed = this.fallbackUsed;
            response.latencyMs = this.latencyMs;
            return response;
        }
    }
    
    // Manual builder for RecommendationItem
    public static class RecommendationItemBuilder {
        private String bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private double score;
        private String reason;
        private String genre;
        private int availableCopies;
        private double twoTowerScore;
        private double rankerScore;
        private boolean isExploration;
        
        public RecommendationItemBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public RecommendationItemBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public RecommendationItemBuilder author(String author) {
            this.author = author;
            return this;
        }
        
        public RecommendationItemBuilder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }
        
        public RecommendationItemBuilder score(double score) {
            this.score = score;
            return this;
        }
        
        public RecommendationItemBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public RecommendationItemBuilder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public RecommendationItemBuilder availableCopies(int availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }
        
        public RecommendationItemBuilder twoTowerScore(double twoTowerScore) {
            this.twoTowerScore = twoTowerScore;
            return this;
        }
        
        public RecommendationItemBuilder rankerScore(double rankerScore) {
            this.rankerScore = rankerScore;
            return this;
        }
        
        public RecommendationItemBuilder isExploration(boolean isExploration) {
            this.isExploration = isExploration;
            return this;
        }
        
        public RecommendationItem build() {
            RecommendationItem item = new RecommendationItem();
            item.bookId = this.bookId;
            item.title = this.title;
            item.author = this.author;
            item.coverImageUrl = this.coverImageUrl;
            item.score = this.score;
            item.reason = this.reason;
            item.genre = this.genre;
            item.availableCopies = this.availableCopies;
            item.twoTowerScore = this.twoTowerScore;
            item.rankerScore = this.rankerScore;
            item.isExploration = this.isExploration;
            return item;
        }
    }
    
    // Manual builder for RecItem
    public static class RecItemBuilder {
        private String bookId;
        private String title;
        private String author;
        private String coverImageUrl;
        private double score;
        private String reason;
        private String genre;
        private int availableCopies;
        
        public RecItemBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public RecItemBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public RecItemBuilder author(String author) {
            this.author = author;
            return this;
        }
        
        public RecItemBuilder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }
        
        public RecItemBuilder score(double score) {
            this.score = score;
            return this;
        }
        
        public RecItemBuilder reason(String reason) {
            this.reason = reason;
            return this;
        }
        
        public RecItemBuilder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public RecItemBuilder availableCopies(int availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }
        
        public RecItem build() {
            RecItem item = new RecItem();
            item.bookId = this.bookId;
            item.title = this.title;
            item.author = this.author;
            item.coverImageUrl = this.coverImageUrl;
            item.score = this.score;
            item.reason = this.reason;
            item.genre = this.genre;
            item.availableCopies = this.availableCopies;
            return item;
        }
    }
}
