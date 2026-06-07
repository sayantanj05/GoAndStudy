package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "activity_logs")
public class ActivityLog {

    @Id
    private String id;
    private String eventType;
    @Indexed
    private String actorId;
    @Indexed
    private String actorRole;
    private String targetCollection;
    @Indexed
    private String targetId;
    @Builder.Default
    private Map<String, Object> changes = new LinkedHashMap<>();
    @Builder.Default
    private Map<String, Object> metadata = new LinkedHashMap<>();
    @Indexed
    private LocalDateTime createdAt;
    
    // Manual builder pattern to fix compilation issues
    public static ActivityLogBuilder builder() {
        return new ActivityLogBuilder();
    }
    
    public static class ActivityLogBuilder {
        private String id;
        private String eventType;
        private String actorId;
        private String actorRole;
        private String targetCollection;
        private String targetId;
        private Map<String, Object> changes = new LinkedHashMap<>();
        private Map<String, Object> metadata = new LinkedHashMap<>();
        private LocalDateTime createdAt;
        
        public ActivityLogBuilder eventType(String eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public ActivityLogBuilder actorId(String actorId) {
            this.actorId = actorId;
            return this;
        }
        
        public ActivityLogBuilder actorRole(String actorRole) {
            this.actorRole = actorRole;
            return this;
        }
        
        public ActivityLogBuilder targetCollection(String targetCollection) {
            this.targetCollection = targetCollection;
            return this;
        }
        
        public ActivityLogBuilder targetId(String targetId) {
            this.targetId = targetId;
            return this;
        }
        
        public ActivityLogBuilder changes(Map<String, Object> changes) {
            this.changes = changes;
            return this;
        }
        
        public ActivityLogBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public ActivityLogBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public ActivityLog build() {
            ActivityLog activityLog = new ActivityLog();
            activityLog.id = this.id;
            activityLog.eventType = this.eventType;
            activityLog.actorId = this.actorId;
            activityLog.actorRole = this.actorRole;
            activityLog.targetCollection = this.targetCollection;
            activityLog.targetId = this.targetId;
            activityLog.changes = this.changes;
            activityLog.metadata = this.metadata;
            activityLog.createdAt = this.createdAt;
            return activityLog;
        }
    }
    
    // Manual getters to fix compilation issues
    public String getEventType() {
        return eventType;
    }
    
    public void setEventType(String eventType) {
        this.eventType = eventType;
    }
    
    public String getActorId() {
        return actorId;
    }
    
    public void setActorId(String actorId) {
        this.actorId = actorId;
    }
    
    public String getActorRole() {
        return actorRole;
    }
    
    public void setActorRole(String actorRole) {
        this.actorRole = actorRole;
    }
    
    public String getTargetId() {
        return targetId;
    }
    
    public void setTargetId(String targetId) {
        this.targetId = targetId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
}
