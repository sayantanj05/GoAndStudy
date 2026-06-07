package com.goandstudybackend.dto.request;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UpdateSystemConfigRequest {

    private Integer loanDurationDays;
    private Integer maxActiveLoans;
    private Double fineRatePerDay;
    private Integer gracePeriodDays;
    private String aiModelId;
    private Integer aiMaxTokens;
    private Boolean chatbotEnabled;
    private Integer chatbotMaxMessagesPerSession;
    private Integer chatbotMaxTokens;
    private Integer chatbotContextWindow;
    private Map<String, Boolean> featureFlags;
    
    // Manual getters and setters to fix compilation issues
    public Integer getLoanDurationDays() {
        return loanDurationDays;
    }
    
    public void setLoanDurationDays(Integer loanDurationDays) {
        this.loanDurationDays = loanDurationDays;
    }
    
    public Integer getMaxActiveLoans() {
        return maxActiveLoans;
    }
    
    public void setMaxActiveLoans(Integer maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
    }
    
    public Double getFineRatePerDay() {
        return fineRatePerDay;
    }
    
    public void setFineRatePerDay(Double fineRatePerDay) {
        this.fineRatePerDay = fineRatePerDay;
    }
    
    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }
    
    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }
    
    public String getAiModelId() {
        return aiModelId;
    }
    
    public void setAiModelId(String aiModelId) {
        this.aiModelId = aiModelId;
    }
    
    public Integer getAiMaxTokens() {
        return aiMaxTokens;
    }
    
    public void setAiMaxTokens(Integer aiMaxTokens) {
        this.aiMaxTokens = aiMaxTokens;
    }
    
    public Boolean getChatbotEnabled() {
        return chatbotEnabled;
    }
    
    public void setChatbotEnabled(Boolean chatbotEnabled) {
        this.chatbotEnabled = chatbotEnabled;
    }
    
    public Integer getChatbotMaxMessagesPerSession() {
        return chatbotMaxMessagesPerSession;
    }
    
    public void setChatbotMaxMessagesPerSession(Integer chatbotMaxMessagesPerSession) {
        this.chatbotMaxMessagesPerSession = chatbotMaxMessagesPerSession;
    }
    
    public Integer getChatbotMaxTokens() {
        return chatbotMaxTokens;
    }
    
    public void setChatbotMaxTokens(Integer chatbotMaxTokens) {
        this.chatbotMaxTokens = chatbotMaxTokens;
    }
    
    public Integer getChatbotContextWindow() {
        return chatbotContextWindow;
    }
    
    public void setChatbotContextWindow(Integer chatbotContextWindow) {
        this.chatbotContextWindow = chatbotContextWindow;
    }
    
    public Map<String, Boolean> getFeatureFlags() {
        return featureFlags;
    }
    
    public void setFeatureFlags(Map<String, Boolean> featureFlags) {
        this.featureFlags = featureFlags;
    }
}
