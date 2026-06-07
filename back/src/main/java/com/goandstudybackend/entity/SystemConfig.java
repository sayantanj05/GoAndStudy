package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "system_configs")
public class SystemConfig {
    @Id
    private String id;
    @Indexed(unique = true)
    private String configKey;
    private String configValue;
    private String description;
    private String configType;
    
    // Loan settings
    private Integer gracePeriodDays;
    private Integer loanDurationDays;
    private Integer maxActiveLoans;
    private Double fineRatePerDay;
    
    // AI settings
    private String aiModelId;
    private Integer aiMaxTokens;
    private Boolean chatbotEnabled;
    private Boolean recommendationsEnabled;
    private String recommendationModel;

    // Chatbot settings (added to match compilation expectations)
    private Integer chatbotMaxMessagesPerSession;
    private Integer chatbotMaxTokens;
    private Integer chatbotContextWindow;
    
    // Feature flags
    private Map<String, Boolean> featureFlags;
    
    // Audit fields
    private String createdBy;
    private LocalDateTime createdAt;
    private String updatedBy;
    private LocalDateTime updatedAt;
    private String updatedByAdminId;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getConfigKey() {
        return configKey;
    }
    
    public void setConfigKey(String configKey) {
        this.configKey = configKey;
    }
    
    public String getConfigValue() {
        return configValue;
    }
    
    public void setConfigValue(String configValue) {
        this.configValue = configValue;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public String getConfigType() {
        return configType;
    }
    
    public void setConfigType(String configType) {
        this.configType = configType;
    }
    
    public Integer getGracePeriodDays() {
        return gracePeriodDays;
    }
    
    public void setGracePeriodDays(Integer gracePeriodDays) {
        this.gracePeriodDays = gracePeriodDays;
    }
    
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
    
    public Boolean getChatbotEnabled() {
        return chatbotEnabled;
    }
    
    public void setChatbotEnabled(Boolean chatbotEnabled) {
        this.chatbotEnabled = chatbotEnabled;
    }
    
    public Boolean getRecommendationsEnabled() {
        return recommendationsEnabled;
    }
    
    public void setRecommendationsEnabled(Boolean recommendationsEnabled) {
        this.recommendationsEnabled = recommendationsEnabled;
    }
    
    public String getRecommendationModel() {
        return recommendationModel;
    }
    
    public void setRecommendationModel(String recommendationModel) {
        this.recommendationModel = recommendationModel;
    }
    
    public Map<String, Boolean> getFeatureFlags() {
        if (featureFlags == null) {
            featureFlags = new HashMap<>();
        }
        return featureFlags;
    }
    
    public void setFeatureFlags(Map<String, Boolean> featureFlags) {
        this.featureFlags = featureFlags;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public void setUpdatedByAdminId(String updatedByAdminId) {
        this.updatedByAdminId = updatedByAdminId;
    }
    
    public static SystemConfigBuilder builder() {
        return new SystemConfigBuilder();
    }
    
    public static class SystemConfigBuilder {
        private String id;
        private String configKey;
        private String configValue;
        private String description;
        private String configType;
        private Integer gracePeriodDays;
        private Integer loanDurationDays;
        private Integer maxActiveLoans;
        private Double fineRatePerDay;
        private String aiModelId;
        private Integer aiMaxTokens;
        private Boolean chatbotEnabled;
        private Boolean recommendationsEnabled;
        private String recommendationModel;

        private Integer chatbotMaxMessagesPerSession;
        private Integer chatbotMaxTokens;
        private Integer chatbotContextWindow;

        private Map<String, Boolean> featureFlags;
        private String createdBy;
        private LocalDateTime createdAt;
        private String updatedBy;
        private LocalDateTime updatedAt;
        private String updatedByAdminId;
        
        public SystemConfigBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public SystemConfigBuilder configKey(String configKey) {
            this.configKey = configKey;
            return this;
        }
        
        public SystemConfigBuilder configValue(String configValue) {
            this.configValue = configValue;
            return this;
        }
        
        public SystemConfigBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public SystemConfigBuilder configType(String configType) {
            this.configType = configType;
            return this;
        }
        
        public SystemConfigBuilder gracePeriodDays(Integer gracePeriodDays) {
            this.gracePeriodDays = gracePeriodDays;
            return this;
        }
        
        public SystemConfigBuilder loanDurationDays(Integer loanDurationDays) {
            this.loanDurationDays = loanDurationDays;
            return this;
        }
        
        public SystemConfigBuilder maxActiveLoans(Integer maxActiveLoans) {
            this.maxActiveLoans = maxActiveLoans;
            return this;
        }
        
        public SystemConfigBuilder fineRatePerDay(Double fineRatePerDay) {
            this.fineRatePerDay = fineRatePerDay;
            return this;
        }
        
        public SystemConfigBuilder aiModelId(String aiModelId) {
            this.aiModelId = aiModelId;
            return this;
        }
        
        public SystemConfigBuilder aiMaxTokens(Integer aiMaxTokens) {
            this.aiMaxTokens = aiMaxTokens;
            return this;
        }
        
        public SystemConfigBuilder chatbotEnabled(Boolean chatbotEnabled) {
            this.chatbotEnabled = chatbotEnabled;
            return this;
        }
        
        public SystemConfigBuilder recommendationsEnabled(Boolean recommendationsEnabled) {
            this.recommendationsEnabled = recommendationsEnabled;
            return this;
        }
        
        public SystemConfigBuilder recommendationModel(String recommendationModel) {
            this.recommendationModel = recommendationModel;
            return this;
        }

        public SystemConfigBuilder chatbotMaxMessagesPerSession(int chatbotMaxMessagesPerSession) {
            this.chatbotMaxMessagesPerSession = chatbotMaxMessagesPerSession;
            return this;
        }

        public SystemConfigBuilder chatbotMaxTokens(int chatbotMaxTokens) {
            this.chatbotMaxTokens = chatbotMaxTokens;
            return this;
        }

        public SystemConfigBuilder chatbotContextWindow(int chatbotContextWindow) {
            this.chatbotContextWindow = chatbotContextWindow;
            return this;
        }
        
        public SystemConfigBuilder featureFlags(Map<String, Boolean> featureFlags) {
            this.featureFlags = featureFlags;
            return this;
        }
        
        public SystemConfigBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public SystemConfigBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public SystemConfigBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public SystemConfigBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public SystemConfigBuilder updatedByAdminId(String updatedByAdminId) {
            this.updatedByAdminId = updatedByAdminId;
            return this;
        }
        
        public SystemConfig build() {
            SystemConfig systemConfig = new SystemConfig();
            systemConfig.id = this.id;
            systemConfig.configKey = this.configKey;
            systemConfig.configValue = this.configValue;
            systemConfig.description = this.description;
            systemConfig.configType = this.configType;
            systemConfig.gracePeriodDays = this.gracePeriodDays;
            systemConfig.loanDurationDays = this.loanDurationDays;
            systemConfig.maxActiveLoans = this.maxActiveLoans;
            systemConfig.fineRatePerDay = this.fineRatePerDay;
            systemConfig.aiModelId = this.aiModelId;
            systemConfig.aiMaxTokens = this.aiMaxTokens;
            systemConfig.chatbotEnabled = this.chatbotEnabled;
            systemConfig.recommendationsEnabled = this.recommendationsEnabled;
            systemConfig.recommendationModel = this.recommendationModel;

            systemConfig.chatbotMaxMessagesPerSession = this.chatbotMaxMessagesPerSession;
            systemConfig.chatbotMaxTokens = this.chatbotMaxTokens;
            systemConfig.chatbotContextWindow = this.chatbotContextWindow;

            systemConfig.featureFlags = this.featureFlags;
            systemConfig.createdBy = this.createdBy;
            systemConfig.createdAt = this.createdAt;
            systemConfig.updatedBy = this.updatedBy;
            systemConfig.updatedAt = this.updatedAt;
            systemConfig.updatedByAdminId = this.updatedByAdminId;
            return systemConfig;
        }
    }
}
