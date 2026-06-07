package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "search_logs")
public class SearchLog {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String query;
    private String queryNormalised;
    @Builder.Default
    private Map<String, Object> filters = new LinkedHashMap<>();
    @Indexed
    private int resultsCount;
    @Builder.Default
    private List<String> topResultIds = new ArrayList<>();
    private String clickedBookId;
    private String searchSource;
    private String sessionId;
    @Indexed
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
    
    // Manual getters and setters to fix compilation issues
    public static SearchLogBuilder builder() {
        return new SearchLogBuilder();
    }
    
    public static class SearchLogBuilder {
        private String id;
        private String memberId;
        private String query;
        private String queryNormalised;
        private Map<String, Object> filters = new LinkedHashMap<>();
        private int resultsCount;
        private List<String> topResultIds = new ArrayList<>();
        private String clickedBookId;
        private String searchSource;
        private String sessionId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        
        public SearchLogBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public SearchLogBuilder query(String query) {
            this.query = query;
            return this;
        }
        
        public SearchLogBuilder queryNormalised(String queryNormalised) {
            this.queryNormalised = queryNormalised;
            return this;
        }
        
        public SearchLogBuilder filters(Map<String, Object> filters) {
            this.filters = filters;
            return this;
        }
        
        public SearchLogBuilder resultsCount(int resultsCount) {
            this.resultsCount = resultsCount;
            return this;
        }
        
        public SearchLogBuilder topResultIds(List<String> topResultIds) {
            this.topResultIds = topResultIds;
            return this;
        }
        
        public SearchLogBuilder clickedBookId(String clickedBookId) {
            this.clickedBookId = clickedBookId;
            return this;
        }
        
        public SearchLogBuilder searchSource(String searchSource) {
            this.searchSource = searchSource;
            return this;
        }
        
        public SearchLogBuilder sessionId(String sessionId) {
            this.sessionId = sessionId;
            return this;
        }
        
        public SearchLogBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public SearchLogBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public SearchLogBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public SearchLogBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public SearchLog build() {
            SearchLog searchLog = new SearchLog();
            searchLog.id = this.id;
            searchLog.memberId = this.memberId;
            searchLog.query = this.query;
            searchLog.queryNormalised = this.queryNormalised;
            searchLog.filters = this.filters;
            searchLog.resultsCount = this.resultsCount;
            searchLog.topResultIds = this.topResultIds;
            searchLog.clickedBookId = this.clickedBookId;
            searchLog.searchSource = this.searchSource;
            searchLog.sessionId = this.sessionId;
            searchLog.createdAt = this.createdAt;
            searchLog.updatedAt = this.updatedAt;
            searchLog.createdBy = this.createdBy;
            searchLog.updatedBy = this.updatedBy;
            return searchLog;
        }
    }
}
