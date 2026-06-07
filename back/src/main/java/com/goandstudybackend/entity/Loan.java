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
@Document(collection = "loans")
public class Loan {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String bookId;
    private String bookIsbn;
    private String bookTitle;
    private String issuedByStaffId;
    @Indexed
    private LocalDateTime issuedAt;
    @Indexed
    private LocalDateTime dueDate;
    private LocalDateTime returnedAt;
    private String returnedByStaffId;
    private String status;
    @Builder.Default
    private int renewalCount = 0;
    @Builder.Default
    private Boolean isOverdue = false;
    @Builder.Default
    private int overdueDays = 0;
    @Builder.Default
    private double fineAmount = 0.0;
    @Builder.Default
    private Boolean finePaid = false;
    private LocalDateTime finePaidAt;
    private String notes;
    @Builder.Default
    private Boolean isCurrentlyReading = false;
    private String currentChapter;
    private LocalDateTime readingStartedAt;
    private LocalDateTime updatedAt;
    private LocalDateTime createdAt;
    private String createdBy;
    private String updatedBy;

    @Indexed
    private String loanStatus;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getMemberId() {
        return memberId;
    }
    
    public void setMemberId(String memberId) {
        this.memberId = memberId;
    }
    
    public String getBookId() {
        return bookId;
    }
    
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public String getNotes() {
        return notes;
    }
    
    public void setNotes(String notes) {
        this.notes = notes;
    }
    
    public Boolean getIsCurrentlyReading() {
        return isCurrentlyReading;
    }
    
    public void setIsCurrentlyReading(Boolean isCurrentlyReading) {
        this.isCurrentlyReading = isCurrentlyReading;
    }
    
    public int getRenewalCount() {
        return renewalCount;
    }
    
    public void setRenewalCount(int renewalCount) {
        this.renewalCount = renewalCount;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public int getOverdueDays() {
        return overdueDays;
    }
    
    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }
    
    public Boolean getIsOverdue() {
        return isOverdue;
    }
    
    public void setIsOverdue(Boolean isOverdue) {
        this.isOverdue = isOverdue;
    }
    
    public double getFineAmount() {
        return fineAmount;
    }
    
    public void setFineAmount(double fineAmount) {
        this.fineAmount = fineAmount;
    }
    
    public String getBookTitle() {
        return bookTitle;
    }
    
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
    
    public String getBookIsbn() {
        return bookIsbn;
    }
    
    public void setBookIsbn(String bookIsbn) {
        this.bookIsbn = bookIsbn;
    }
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public LocalDateTime getReturnedAt() {
        return returnedAt;
    }
    
    public void setReturnedAt(LocalDateTime returnedAt) {
        this.returnedAt = returnedAt;
    }
    
    public Boolean getFinePaid() {
        return finePaid;
    }
    
    public void setFinePaid(Boolean finePaid) {
        this.finePaid = finePaid;
    }
    
    public String getCurrentChapter() {
        return currentChapter;
    }
    
    public void setCurrentChapter(String currentChapter) {
        this.currentChapter = currentChapter;
    }
    
    public LocalDateTime getReadingStartedAt() {
        return readingStartedAt;
    }
    
    public void setReadingStartedAt(LocalDateTime readingStartedAt) {
        this.readingStartedAt = readingStartedAt;
    }
    
    public LocalDateTime getFinePaidAt() {
        return finePaidAt;
    }
    
    public void setFinePaidAt(LocalDateTime finePaidAt) {
        this.finePaidAt = finePaidAt;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getIssuedByStaffId() {
        return issuedByStaffId;
    }
    
    public void setIssuedByStaffId(String issuedByStaffId) {
        this.issuedByStaffId = issuedByStaffId;
    }
    
    public static LoanBuilder builder() {
        return new LoanBuilder();
    }
    
    public static class LoanBuilder {
        private String id;
        private String memberId;
        private String bookId;
        private String bookIsbn;
        private String bookTitle;
        private String issuedByStaffId;
        private LocalDateTime issuedAt;
        private LocalDateTime dueDate;
        private LocalDateTime returnedAt;
        private String returnedByStaffId;
        private String status;
        private int renewalCount = 0;
        private Boolean isOverdue = false;
        private int overdueDays = 0;
        private double fineAmount = 0.0;
        private Boolean finePaid = false;
        private LocalDateTime finePaidAt;
        private String notes;
        private Boolean isCurrentlyReading = false;
        private String currentChapter;
        private LocalDateTime readingStartedAt;
        private LocalDateTime updatedAt;
        private LocalDateTime createdAt;
        private String createdBy;
        private String updatedBy;
        private String loanStatus;
        
        public LoanBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public LoanBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public LoanBuilder bookIsbn(String bookIsbn) {
            this.bookIsbn = bookIsbn;
            return this;
        }
        
        public LoanBuilder bookTitle(String bookTitle) {
            this.bookTitle = bookTitle;
            return this;
        }
        
        public LoanBuilder issuedByStaffId(String issuedByStaffId) {
            this.issuedByStaffId = issuedByStaffId;
            return this;
        }
        
        public LoanBuilder issuedAt(LocalDateTime issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public LoanBuilder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }
        
        public LoanBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public LoanBuilder notes(String notes) {
            this.notes = notes;
            return this;
        }
        
        public LoanBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public LoanBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public LoanBuilder loanStatus(String loanStatus) {
            this.loanStatus = loanStatus;
            return this;
        }
        
        public LoanBuilder isCurrentlyReading(Boolean isCurrentlyReading) {
            this.isCurrentlyReading = isCurrentlyReading;
            return this;
        }
        
        public LoanBuilder renewalCount(int renewalCount) {
            this.renewalCount = renewalCount;
            return this;
        }
        
        public LoanBuilder overdueDays(int overdueDays) {
            this.overdueDays = overdueDays;
            return this;
        }
        
        public LoanBuilder isOverdue(Boolean isOverdue) {
            this.isOverdue = isOverdue;
            return this;
        }
        
        public LoanBuilder fineAmount(double fineAmount) {
            this.fineAmount = fineAmount;
            return this;
        }
        
        public Loan build() {
            Loan loan = new Loan();
            loan.id = this.id;
            loan.memberId = this.memberId;
            loan.bookId = this.bookId;
            loan.bookIsbn = this.bookIsbn;
            loan.bookTitle = this.bookTitle;
            loan.issuedByStaffId = this.issuedByStaffId;
            loan.issuedAt = this.issuedAt;
            loan.dueDate = this.dueDate;
            loan.returnedAt = this.returnedAt;
            loan.returnedByStaffId = this.returnedByStaffId;
            loan.status = this.status;
            loan.renewalCount = this.renewalCount;
            loan.isOverdue = this.isOverdue;
            loan.overdueDays = this.overdueDays;
            loan.fineAmount = this.fineAmount;
            loan.finePaid = this.finePaid;
            loan.finePaidAt = this.finePaidAt;
            loan.notes = this.notes;
            loan.isCurrentlyReading = this.isCurrentlyReading;
            loan.currentChapter = this.currentChapter;
            loan.readingStartedAt = this.readingStartedAt;
            loan.updatedAt = this.updatedAt;
            loan.createdAt = this.createdAt;
            loan.createdBy = this.createdBy;
            loan.updatedBy = this.updatedBy;
            loan.loanStatus = this.loanStatus;
            return loan;
        }
    }
}
