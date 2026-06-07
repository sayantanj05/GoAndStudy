package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_queues")
public class BookQueue {

    @Id
    private String id;
    
    @Indexed
    private String memberId;
    
    private String queueName;
    
    private List<QueuedBook> books;
    
    private int currentPosition;
    
    @Builder.Default
    private boolean isActive = true;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getQueueName() {
        return queueName;
    }
    
    public void setQueueName(String queueName) {
        this.queueName = queueName;
    }
    
    public List<QueuedBook> getBooks() {
        return books;
    }
    
    public void setBooks(List<QueuedBook> books) {
        this.books = books;
    }
    
    public int getCurrentPosition() {
        return currentPosition;
    }
    
    public void setCurrentPosition(int currentPosition) {
        this.currentPosition = currentPosition;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class QueuedBook {
        private String bookId;
        private String title;
        private String author;
        private int position;
        private String status; // PENDING, READING, COMPLETED
        private LocalDateTime startedAt;
        private LocalDateTime completedAt;
        private LocalDateTime targetDate;
        
        // Manual getters and setters to fix compilation issues
        public int getPosition() {
            return position;
        }
        
        public void setPosition(int position) {
            this.position = position;
        }
        
        public String getStatus() {
            return status;
        }
        
        public void setStatus(String status) {
            this.status = status;
        }
        
        public LocalDateTime getStartedAt() {
            return startedAt;
        }
        
        public void setStartedAt(LocalDateTime startedAt) {
            this.startedAt = startedAt;
        }
    }
}
