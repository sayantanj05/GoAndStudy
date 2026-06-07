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
@Document(collection = "ai_logs")
public class AiLog {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String sessionId;
    private String queryText;
    @Builder.Default
    private Map<String, Object> contextSnapshot = new LinkedHashMap<>();
    private String modelUsed;
    private String systemPrompt;
    private String responseText;
    @Builder.Default
    private List<String> recommendedBookIds = new ArrayList<>();
    @Builder.Default
    private List<Double> vectorSearchQuery = new ArrayList<>();
    @Builder.Default
    private List<String> vectorSearchResults = new ArrayList<>();
    private int tokensUsed;
    private int latencyMs;
    @Builder.Default
    private boolean wasActedOn = false;
    @Indexed
    private LocalDateTime createdAt;
    
    // Manual getters to fix compilation issues
    public int getTokensUsed() {
        return tokensUsed;
    }
    
    public void setTokensUsed(int tokensUsed) {
        this.tokensUsed = tokensUsed;
    }
    
    public int getLatencyMs() {
        return latencyMs;
    }
    
    public void setLatencyMs(int latencyMs) {
        this.latencyMs = latencyMs;
    }
    
    public boolean isWasActedOn() {
        return wasActedOn;
    }
    
    public void setWasActedOn(boolean wasActedOn) {
        this.wasActedOn = wasActedOn;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
