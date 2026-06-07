package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "member_analytics")
public class MemberAnalytics {

    @Id
    private String id;
    private int totalLoans;
    private int totalReturned;
    private int totalOverdue;
    private double overdueRate;
    private double totalFinesIncurred;
    private double totalFinesPaid;
    private double avgLoanDuration;
    private int totalSearches;
    private int totalAiCalls;
    private double aiConversionRate;
    private String mostActiveMonth;
    @Builder.Default
    private List<Map<String, Object>> genreBreakdown = new ArrayList<>();
private LocalDateTime computedAt;
    private int age = 0;
    
    // Manual getters and setters to fix compilation issues
    public int getTotalLoans() {
        return totalLoans;
    }
    
    public void setTotalLoans(int totalLoans) {
        this.totalLoans = totalLoans;
    }
    
    public int getTotalReturned() {
        return totalReturned;
    }
    
    public void setTotalReturned(int totalReturned) {
        this.totalReturned = totalReturned;
    }
    
    public int getTotalOverdue() {
        return totalOverdue;
    }
    
    public void setTotalOverdue(int totalOverdue) {
        this.totalOverdue = totalOverdue;
    }
    
    public double getOverdueRate() {
        return overdueRate;
    }
    
    public void setOverdueRate(double overdueRate) {
        this.overdueRate = overdueRate;
    }
    
    public double getTotalFinesIncurred() {
        return totalFinesIncurred;
    }
    
    public void setTotalFinesIncurred(double totalFinesIncurred) {
        this.totalFinesIncurred = totalFinesIncurred;
    }
    
    public double getTotalFinesPaid() {
        return totalFinesPaid;
    }
    
    public void setTotalFinesPaid(double totalFinesPaid) {
        this.totalFinesPaid = totalFinesPaid;
    }
    
    public double getAvgLoanDuration() {
        return avgLoanDuration;
    }
    
    public void setAvgLoanDuration(double avgLoanDuration) {
        this.avgLoanDuration = avgLoanDuration;
    }
    
    public int getTotalSearches() {
        return totalSearches;
    }
    
    public void setTotalSearches(int totalSearches) {
        this.totalSearches = totalSearches;
    }
    
    public int getTotalAiCalls() {
        return totalAiCalls;
    }
    
    public void setTotalAiCalls(int totalAiCalls) {
        this.totalAiCalls = totalAiCalls;
    }
    
    public double getAiConversionRate() {
        return aiConversionRate;
    }
    
    public void setAiConversionRate(double aiConversionRate) {
        this.aiConversionRate = aiConversionRate;
    }
    
    public String getMostActiveMonth() {
        return mostActiveMonth;
    }
    
    public void setMostActiveMonth(String mostActiveMonth) {
        this.mostActiveMonth = mostActiveMonth;
    }
    
    public List<Map<String, Object>> getGenreBreakdown() {
        return genreBreakdown;
    }
    
    public void setGenreBreakdown(List<Map<String, Object>> genreBreakdown) {
        this.genreBreakdown = genreBreakdown;
    }
    
    public LocalDateTime getComputedAt() {
        return computedAt;
    }
    
    public void setComputedAt(LocalDateTime computedAt) {
        this.computedAt = computedAt;
    }
    
    public static MemberAnalyticsBuilder builder() {
        return new MemberAnalyticsBuilder();
    }
    
    public static class MemberAnalyticsBuilder {
        private String id;
        private int totalLoans;
        private int totalReturned;
        private int totalOverdue;
        private double overdueRate;
        private double totalFinesIncurred;
        private double totalFinesPaid;
        private double avgLoanDuration;
        private int totalSearches;
        private int totalAiCalls;
        private double aiConversionRate;
        private String mostActiveMonth;
        private List<Map<String, Object>> genreBreakdown;
        private LocalDateTime computedAt;
        private int age;
        
        public MemberAnalyticsBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public MemberAnalyticsBuilder totalLoans(int totalLoans) {
            this.totalLoans = totalLoans;
            return this;
        }
        
        public MemberAnalyticsBuilder totalReturned(int totalReturned) {
            this.totalReturned = totalReturned;
            return this;
        }
        
        public MemberAnalyticsBuilder totalOverdue(int totalOverdue) {
            this.totalOverdue = totalOverdue;
            return this;
        }
        
        public MemberAnalyticsBuilder overdueRate(double overdueRate) {
            this.overdueRate = overdueRate;
            return this;
        }
        
        public MemberAnalyticsBuilder totalFinesIncurred(double totalFinesIncurred) {
            this.totalFinesIncurred = totalFinesIncurred;
            return this;
        }
        
        public MemberAnalyticsBuilder totalFinesPaid(double totalFinesPaid) {
            this.totalFinesPaid = totalFinesPaid;
            return this;
        }
        
        public MemberAnalyticsBuilder avgLoanDuration(double avgLoanDuration) {
            this.avgLoanDuration = avgLoanDuration;
            return this;
        }
        
        public MemberAnalyticsBuilder totalSearches(int totalSearches) {
            this.totalSearches = totalSearches;
            return this;
        }
        
        public MemberAnalyticsBuilder totalAiCalls(int totalAiCalls) {
            this.totalAiCalls = totalAiCalls;
            return this;
        }
        
        public MemberAnalyticsBuilder aiConversionRate(double aiConversionRate) {
            this.aiConversionRate = aiConversionRate;
            return this;
        }
        
        public MemberAnalyticsBuilder mostActiveMonth(String mostActiveMonth) {
            this.mostActiveMonth = mostActiveMonth;
            return this;
        }
        
        public MemberAnalyticsBuilder genreBreakdown(List<Map<String, Object>> genreBreakdown) {
            this.genreBreakdown = genreBreakdown;
            return this;
        }
        
        public MemberAnalyticsBuilder computedAt(LocalDateTime computedAt) {
            this.computedAt = computedAt;
            return this;
        }
        
        public MemberAnalyticsBuilder age(int age) {
            this.age = age;
            return this;
        }
        
        public MemberAnalytics build() {
            MemberAnalytics memberAnalytics = new MemberAnalytics();
            memberAnalytics.id = this.id;
            memberAnalytics.totalLoans = this.totalLoans;
            memberAnalytics.totalReturned = this.totalReturned;
            memberAnalytics.totalOverdue = this.totalOverdue;
            memberAnalytics.overdueRate = this.overdueRate;
            memberAnalytics.totalFinesIncurred = this.totalFinesIncurred;
            memberAnalytics.totalFinesPaid = this.totalFinesPaid;
            memberAnalytics.avgLoanDuration = this.avgLoanDuration;
            memberAnalytics.totalSearches = this.totalSearches;
            memberAnalytics.totalAiCalls = this.totalAiCalls;
            memberAnalytics.aiConversionRate = this.aiConversionRate;
            memberAnalytics.mostActiveMonth = this.mostActiveMonth;
            memberAnalytics.genreBreakdown = this.genreBreakdown;
            memberAnalytics.computedAt = this.computedAt;
            memberAnalytics.age = this.age;
            return memberAnalytics;
        }
    }
}
