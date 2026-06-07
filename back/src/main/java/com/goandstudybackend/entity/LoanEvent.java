package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "loan_events")
public class LoanEvent {
    @Id
    private String id;
    private String loanId;
private com.goandstudybackend.enumeration.LoanEventType eventType;
    private LocalDateTime timestamp;
    private String triggeredBy;
    private Map<String, Object> metadata;
    private LocalDateTime createdAt;
    
    // Manual getters and setters to fix compilation issues
    public static LoanEventBuilder builder() {
        return new LoanEventBuilder();
    }
    
    public static class LoanEventBuilder {
        private String id;
        private String loanId;
        private com.goandstudybackend.enumeration.LoanEventType eventType;
        private LocalDateTime timestamp;
        private String triggeredBy;
        private Map<String, Object> metadata;
        private LocalDateTime createdAt;
        
        public LoanEventBuilder loanId(String loanId) {
            this.loanId = loanId;
            return this;
        }
        
        public LoanEventBuilder eventType(com.goandstudybackend.enumeration.LoanEventType eventType) {
            this.eventType = eventType;
            return this;
        }
        
        public LoanEventBuilder timestamp(LocalDateTime timestamp) {
            this.timestamp = timestamp;
            return this;
        }
        
        public LoanEventBuilder triggeredBy(String triggeredBy) {
            this.triggeredBy = triggeredBy;
            return this;
        }
        
        public LoanEventBuilder metadata(Map<String, Object> metadata) {
            this.metadata = metadata;
            return this;
        }
        
        public LoanEventBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public LoanEvent build() {
            LoanEvent loanEvent = new LoanEvent();
            loanEvent.id = this.id;
            loanEvent.loanId = this.loanId;
            loanEvent.eventType = this.eventType;
            loanEvent.timestamp = this.timestamp;
            loanEvent.triggeredBy = this.triggeredBy;
            loanEvent.metadata = this.metadata;
            loanEvent.createdAt = this.createdAt;
            return loanEvent;
        }
    }
}
