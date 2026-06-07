package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "notifications")
public class Notification {

    @Id
    private String id;
    @Indexed
    private String memberId;
    private String type;
    private String title;
    private String message;
    private String relatedLoanId;
    private String relatedBookId;
    private String channel;
    private String status;
    private String sentByStaffId;
    private boolean isAutoTriggered;
    private LocalDateTime readAt;
    @Indexed
    private LocalDateTime createdAt;
    
    // Explicit getters to fix Lombok issues
    public String getId() { return id; }
    public String getMemberId() { return memberId; }
    public String getType() { return type; }
    public String getTitle() { return title; }
    public String getMessage() { return message; }
    public String getRelatedLoanId() { return relatedLoanId; }
    public String getRelatedBookId() { return relatedBookId; }
    public String getChannel() { return channel; }
    public String getStatus() { return status; }
    public String getSentByStaffId() { return sentByStaffId; }
    public boolean isAutoTriggered() { return isAutoTriggered; }
    public LocalDateTime getReadAt() { return readAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    
    // Explicit setters
    public void setId(String id) { this.id = id; }
    public void setMemberId(String memberId) { this.memberId = memberId; }
    public void setType(String type) { this.type = type; }
    public void setTitle(String title) { this.title = title; }
    public void setMessage(String message) { this.message = message; }
    public void setRelatedLoanId(String relatedLoanId) { this.relatedLoanId = relatedLoanId; }
    public void setRelatedBookId(String relatedBookId) { this.relatedBookId = relatedBookId; }
    public void setChannel(String channel) { this.channel = channel; }
    public void setStatus(String status) { this.status = status; }
    public void setSentByStaffId(String sentByStaffId) { this.sentByStaffId = sentByStaffId; }
    public void setAutoTriggered(boolean autoTriggered) { isAutoTriggered = autoTriggered; }
    public void setReadAt(LocalDateTime readAt) { this.readAt = readAt; }
    public void setCreatedAt(LocalDateTime createdAt) { this.createdAt = createdAt; }
    
    // Manual builder pattern to fix compilation issues
    public static NotificationBuilder builder() {
        return new NotificationBuilder();
    }
    
    public static class NotificationBuilder {
        private String id;
        private String memberId;
        private String type;
        private String title;
        private String message;
        private String relatedLoanId;
        private String relatedBookId;
        private String channel;
        private String status;
        private String sentByStaffId;
        private boolean isAutoTriggered;
        private LocalDateTime readAt;
        private LocalDateTime createdAt;
        
        public NotificationBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public NotificationBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public NotificationBuilder type(String type) {
            this.type = type;
            return this;
        }
        
        public NotificationBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public NotificationBuilder message(String message) {
            this.message = message;
            return this;
        }
        
        public NotificationBuilder relatedLoanId(String relatedLoanId) {
            this.relatedLoanId = relatedLoanId;
            return this;
        }
        
        public NotificationBuilder relatedBookId(String relatedBookId) {
            this.relatedBookId = relatedBookId;
            return this;
        }
        
        public NotificationBuilder channel(String channel) {
            this.channel = channel;
            return this;
        }
        
        public NotificationBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public NotificationBuilder sentByStaffId(String sentByStaffId) {
            this.sentByStaffId = sentByStaffId;
            return this;
        }
        
        public NotificationBuilder isAutoTriggered(boolean isAutoTriggered) {
            this.isAutoTriggered = isAutoTriggered;
            return this;
        }
        
        public NotificationBuilder readAt(LocalDateTime readAt) {
            this.readAt = readAt;
            return this;
        }
        
        public NotificationBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public Notification build() {
            Notification notification = new Notification();
            notification.id = this.id;
            notification.memberId = this.memberId;
            notification.type = this.type;
            notification.title = this.title;
            notification.message = this.message;
            notification.relatedLoanId = this.relatedLoanId;
            notification.relatedBookId = this.relatedBookId;
            notification.channel = this.channel;
            notification.status = this.status;
            notification.sentByStaffId = this.sentByStaffId;
            notification.isAutoTriggered = this.isAutoTriggered;
            notification.readAt = this.readAt;
            notification.createdAt = this.createdAt;
            return notification;
        }
    }
}
