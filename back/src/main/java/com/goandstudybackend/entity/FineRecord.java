package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "fine_records")
public class FineRecord {
    @Id
    private String id;
    private String memberId;
    private String loanId;
    private String bookId;
    private double amount;
    private double ratePerDay;
    private int overdueDays;
    private String status;
    private LocalDateTime issuedAt;
    private LocalDateTime dueDate;
    private LocalDateTime paidAt;
    private String collectedByStaffId;
    private LocalDateTime collectedAt;
    private String waivedByAdminId;
    private String waivedReason;
    private LocalDateTime createdAt;
    private String createdBy;
    private String bookTitle;
    private double totalAmount;
    
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
    
    public String getLoanId() {
        return loanId;
    }
    
    public void setLoanId(String loanId) {
        this.loanId = loanId;
    }
    
    public String getBookId() {
        return bookId;
    }
    
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    
    public double getAmount() {
        return amount;
    }
    
    public void setAmount(double amount) {
        this.amount = amount;
    }
    
    public double getRatePerDay() {
        return ratePerDay;
    }
    
    public void setRatePerDay(double ratePerDay) {
        this.ratePerDay = ratePerDay;
    }
    
    public int getOverdueDays() {
        return overdueDays;
    }
    
    public void setOverdueDays(int overdueDays) {
        this.overdueDays = overdueDays;
    }
    
    public String getStatus() {
        return status;
    }
    
    public void setStatus(String status) {
        this.status = status;
    }
    
    public LocalDateTime getIssuedAt() {
        return issuedAt;
    }
    
    public void setIssuedAt(LocalDateTime issuedAt) {
        this.issuedAt = issuedAt;
    }
    
    public LocalDateTime getDueDate() {
        return dueDate;
    }
    
    public void setDueDate(LocalDateTime dueDate) {
        this.dueDate = dueDate;
    }
    
    public LocalDateTime getPaidAt() {
        return paidAt;
    }
    
    public void setPaidAt(LocalDateTime paidAt) {
        this.paidAt = paidAt;
    }
    
    public String getCollectedByStaffId() {
        return collectedByStaffId;
    }
    
    public void setCollectedByStaffId(String collectedByStaffId) {
        this.collectedByStaffId = collectedByStaffId;
    }
    
    public LocalDateTime getCollectedAt() {
        return collectedAt;
    }
    
    public void setCollectedAt(LocalDateTime collectedAt) {
        this.collectedAt = collectedAt;
    }
    
    public void setWaivedByAdminId(String waivedByAdminId) {
        this.waivedByAdminId = waivedByAdminId;
    }
    
    public void setWaivedReason(String waivedReason) {
        this.waivedReason = waivedReason;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getBookTitle() {
        return bookTitle;
    }
    
    public void setBookTitle(String bookTitle) {
        this.bookTitle = bookTitle;
    }
    
    public double getTotalAmount() {
        return totalAmount;
    }
    
    public void setTotalAmount(double totalAmount) {
        this.totalAmount = totalAmount;
    }
    
    public static FineRecordBuilder builder() {
        return new FineRecordBuilder();
    }
    
    public static class FineRecordBuilder {
        private String id;
        private String memberId;
        private String loanId;
        private String bookId;
        private double amount;
        private double ratePerDay;
        private int overdueDays;
        private String status;
        private LocalDateTime issuedAt;
        private LocalDateTime dueDate;
        private LocalDateTime paidAt;
        private String collectedByStaffId;
        private LocalDateTime collectedAt;
        private String waivedByAdminId;
        private String waivedReason;
        private LocalDateTime createdAt;
        private String createdBy;
        private String bookTitle;
        private double totalAmount;
        
        public FineRecordBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public FineRecordBuilder memberId(String memberId) {
            this.memberId = memberId;
            return this;
        }
        
        public FineRecordBuilder loanId(String loanId) {
            this.loanId = loanId;
            return this;
        }
        
        public FineRecordBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public FineRecordBuilder amount(double amount) {
            this.amount = amount;
            return this;
        }
        
        public FineRecordBuilder ratePerDay(double ratePerDay) {
            this.ratePerDay = ratePerDay;
            return this;
        }
        
        public FineRecordBuilder overdueDays(int overdueDays) {
            this.overdueDays = overdueDays;
            return this;
        }
        
        public FineRecordBuilder status(String status) {
            this.status = status;
            return this;
        }
        
        public FineRecordBuilder issuedAt(LocalDateTime issuedAt) {
            this.issuedAt = issuedAt;
            return this;
        }
        
        public FineRecordBuilder dueDate(LocalDateTime dueDate) {
            this.dueDate = dueDate;
            return this;
        }
        
        public FineRecordBuilder paidAt(LocalDateTime paidAt) {
            this.paidAt = paidAt;
            return this;
        }
        
        public FineRecordBuilder collectedByStaffId(String collectedByStaffId) {
            this.collectedByStaffId = collectedByStaffId;
            return this;
        }
        
        public FineRecordBuilder collectedAt(LocalDateTime collectedAt) {
            this.collectedAt = collectedAt;
            return this;
        }
        
        public FineRecordBuilder waivedByAdminId(String waivedByAdminId) {
            this.waivedByAdminId = waivedByAdminId;
            return this;
        }
        
        public FineRecordBuilder waivedReason(String waivedReason) {
            this.waivedReason = waivedReason;
            return this;
        }
        
        public FineRecordBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public FineRecordBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public FineRecordBuilder bookTitle(String bookTitle) {
            this.bookTitle = bookTitle;
            return this;
        }
        
        public FineRecordBuilder totalAmount(double totalAmount) {
            this.totalAmount = totalAmount;
            return this;
        }
        
        public FineRecord build() {
            FineRecord fineRecord = new FineRecord();
            fineRecord.id = this.id;
            fineRecord.memberId = this.memberId;
            fineRecord.loanId = this.loanId;
            fineRecord.bookId = this.bookId;
            fineRecord.amount = this.amount;
            fineRecord.ratePerDay = this.ratePerDay;
            fineRecord.overdueDays = this.overdueDays;
            fineRecord.status = this.status;
            fineRecord.issuedAt = this.issuedAt;
            fineRecord.dueDate = this.dueDate;
            fineRecord.paidAt = this.paidAt;
            fineRecord.collectedByStaffId = this.collectedByStaffId;
            fineRecord.collectedAt = this.collectedAt;
            fineRecord.waivedByAdminId = this.waivedByAdminId;
            fineRecord.waivedReason = this.waivedReason;
            fineRecord.createdAt = this.createdAt;
            fineRecord.createdBy = this.createdBy;
            fineRecord.bookTitle = this.bookTitle;
            fineRecord.totalAmount = this.totalAmount;
            return fineRecord;
        }
    }
}
