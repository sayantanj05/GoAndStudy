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
@Document(collection = "renewal_requests")
public class RenewalRequest {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String loanId;
    private String bookTitle;
    private String memberName;
    private LocalDateTime currentDueDate;
    @Indexed
    private LocalDateTime requestedAt;
    @Builder.Default
    private String status = "Pending";
    private LocalDateTime reviewedAt;
    private String reviewedBy;
    private String processedByStaffId;
    private LocalDateTime processedAt;
    private String rejectionReason;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public void setRejectionReason(String rejectionReason) {
        this.rejectionReason = rejectionReason;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getLoanId() {
        return loanId;
    }
    
    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }
    
    public String getBookTitle() {
        return bookTitle;
    }
    
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public String getProcessedByStaffId() {
        return processedByStaffId;
    }
    
    public void setProcessedByStaffId(String processedByStaffId) {
        this.processedByStaffId = processedByStaffId;
    }
    
    public LocalDateTime getProcessedAt() {
        return processedAt;
    }
    
    public void setProcessedAt(LocalDateTime processedAt) {
        this.processedAt = processedAt;
    }
    
    public static RenewalRequestBuilder builder() {
        return new RenewalRequestBuilder();
    }
    
    public static class RenewalRequestBuilder {
        private String id;
        private String memberId;
        private String loanId;
        private String memberName;
        private String bookTitle;
        private LocalDateTime currentDueDate;
        private String status;
        private LocalDateTime requestedAt;
        private LocalDateTime reviewedAt;
        private String reviewedBy;
        private String rejectionReason;
        
        public RenewalRequestBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public RenewalRequestBuilder loanId(String loanId) {
            this.loanId = loanId;
            return this;
        }
        
        public RenewalRequestBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public RenewalRequestBuilder requestedAt(LocalDateTime requestedAt) {
            this.requestedAt = requestedAt;
            return this;
        }
        
        public RenewalRequestBuilder reviewedAt(LocalDateTime reviewedAt) {
            this.reviewedAt = reviewedAt;
            return this;
        }
        
        public RenewalRequestBuilder reviewedBy(String reviewedBy) {
            this.reviewedBy = reviewedBy;
            return this;
        }
        
        public RenewalRequestBuilder bookTitle(String bookTitle) {
            this.bookTitle = bookTitle;
            return this;
        }
        
        public RenewalRequestBuilder memberName(String memberName) {
            this.memberName = memberName;
            return this;
        }
        
        public RenewalRequestBuilder currentDueDate(LocalDateTime currentDueDate) {
            this.currentDueDate = currentDueDate;
            return this;
        }
        
        public RenewalRequestBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public RenewalRequestBuilder rejectionReason(String rejectionReason) {
            this.rejectionReason = rejectionReason;
            return this;
        }
        
        public RenewalRequest build() {
            RenewalRequest renewalRequest = new RenewalRequest();
            renewalRequest.id = this.id;
            renewalRequest.loanId = this.loanId;
            renewalRequest.memberId = this.memberId;
            renewalRequest.memberName = this.memberName;
            renewalRequest.bookTitle = this.bookTitle;
            renewalRequest.requestedAt = this.requestedAt;
            renewalRequest.reviewedAt = this.reviewedAt;
            renewalRequest.reviewedBy = this.reviewedBy;
            renewalRequest.rejectionReason = this.rejectionReason;
            renewalRequest.currentDueDate = this.currentDueDate;
            renewalRequest.status = this.status;
            return renewalRequest;
        }
    }
}
