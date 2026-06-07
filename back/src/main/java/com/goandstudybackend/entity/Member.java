package com.goandstudybackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "members")
public class Member {

    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String phone;
    private String gender;
    @JsonIgnore
    private String passwordHash;
    private String plainPassword;
    @Builder.Default
    private String role = "ROLE_MEMBER";
    @Builder.Default
    private boolean isActive = true;
    private String registrationDate;
    private int dailySequence;
    private LocalDateTime timeCreated;
    private LocalDateTime lastLogin;
    @Builder.Default
    private int loginCount = 0;
    @Builder.Default
    private int activeLoanCount = 0;
    @Builder.Default
    private int totalLoans = 0;
    @Builder.Default
    private double totalFinesPaid = 0.0;
    @Builder.Default
    private List<String> preferredGenres = new ArrayList<>();
    private LocalDateTime updatedAt;
    @Builder.Default
    private int readingGoalBooks = 0;
    @Builder.Default
    private String membershipType = "BASIC";
    private LocalDateTime membershipStartDate;
    private LocalDateTime membershipExpiryDate;
    @Builder.Default
    private double membershipFeePaid = 0.0;
    @Builder.Default
    private int maxActiveLoans = 2;
    @Builder.Default
    private int loanDurationDays = 14;
    @Builder.Default
    private double fineRatePerDay = 10.0;
    @Builder.Default
    private int renewalLimit = 1;
    
    private LocalDate dateOfBirth;
    private String createdBy;
    private String updatedBy;
    
    // Neural Controls - AI Recommendation Preferences
    @Builder.Default
    private String inferenceLevel = "Exploratory"; // Exploratory, Balanced, Focused
    @Builder.Default
    private String privacyProjection = "Standard"; // Standard, High
    @Builder.Default
    private Boolean obfuscationMode = false;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public int getActiveLoanCount() {
        return activeLoanCount;
    }
    
    public void setActiveLoanCount(int activeLoanCount) {
        this.activeLoanCount = activeLoanCount;
    }
    
    public int getTotalLoans() {
        return totalLoans;
    }
    
    public void setTotalLoans(int totalLoans) {
        this.totalLoans = totalLoans;
    }
    
    public int getLoanDurationDays() {
        return loanDurationDays;
    }
    
    public void setLoanDurationDays(int loanDurationDays) {
        this.loanDurationDays = loanDurationDays;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public double getTotalFinesPaid() {
        return totalFinesPaid;
    }
    
    public void setTotalFinesPaid(double totalFinesPaid) {
        this.totalFinesPaid = totalFinesPaid;
    }
    
    public int getRenewalLimit() {
        return renewalLimit;
    }
    
    public void setRenewalLimit(int renewalLimit) {
        this.renewalLimit = renewalLimit;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean active) {
        isActive = active;
    }
    
    public int getMaxActiveLoans() {
        return maxActiveLoans;
    }
    
    public void setMaxActiveLoans(int maxActiveLoans) {
        this.maxActiveLoans = maxActiveLoans;
    }
    
    public double getFineRatePerDay() {
        return fineRatePerDay;
    }
    
    public void setFineRatePerDay(double fineRatePerDay) {
        this.fineRatePerDay = fineRatePerDay;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    // Manual builder pattern to fix compilation issues
    public static MemberBuilder builder() {
        return new MemberBuilder();
    }
    
    public static class MemberBuilder {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String gender;
        private String passwordHash;
        private String plainPassword;
        private String role = "ROLE_MEMBER";
        private boolean isActive = true;
        private String registrationDate;
        private int dailySequence;
        private LocalDateTime timeCreated;
        private LocalDateTime lastLogin;
        private int loginCount = 0;
        private int activeLoanCount = 0;
        private int totalLoans = 0;
        private double totalFinesPaid = 0.0;
        private List<String> preferredGenres = new ArrayList<>();
        private LocalDateTime updatedAt;
        private int readingGoalBooks = 0;
        private String membershipType = "BASIC";
        private LocalDateTime membershipStartDate;
        private LocalDateTime membershipExpiryDate;
        private double membershipFeePaid = 0.0;
        private int maxActiveLoans = 2;
        private int loanDurationDays = 14;
        private double fineRatePerDay = 10.0;
        private int renewalLimit = 1;
        private LocalDate dateOfBirth;
        private String createdBy;
        private String updatedBy;
        private String inferenceLevel = "Exploratory";
        private String privacyProjection = "Standard";
        private Boolean obfuscationMode = false;
        
        public MemberBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public MemberBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public MemberBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public MemberBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public MemberBuilder gender(String gender) {
            this.gender = gender;
            return this;
        }
        
        public MemberBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }
        
        public MemberBuilder plainPassword(String plainPassword) {
            this.plainPassword = plainPassword;
            return this;
        }
        
        public MemberBuilder role(String role) {
            this.role = role;
            return this;
        }
        
        public MemberBuilder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public MemberBuilder registrationDate(String registrationDate) {
            this.registrationDate = registrationDate;
            return this;
        }
        
        public MemberBuilder dailySequence(int dailySequence) {
            this.dailySequence = dailySequence;
            return this;
        }
        
        public MemberBuilder timeCreated(LocalDateTime timeCreated) {
            this.timeCreated = timeCreated;
            return this;
        }
        
        public MemberBuilder lastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }
        
        public MemberBuilder loginCount(int loginCount) {
            this.loginCount = loginCount;
            return this;
        }
        
        public MemberBuilder activeLoanCount(int activeLoanCount) {
            this.activeLoanCount = activeLoanCount;
            return this;
        }
        
        public MemberBuilder totalLoans(int totalLoans) {
            this.totalLoans = totalLoans;
            return this;
        }
        
        public MemberBuilder totalFinesPaid(double totalFinesPaid) {
            this.totalFinesPaid = totalFinesPaid;
            return this;
        }
        
        public MemberBuilder preferredGenres(List<String> preferredGenres) {
            this.preferredGenres = preferredGenres;
            return this;
        }
        
        public MemberBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public MemberBuilder readingGoalBooks(int readingGoalBooks) {
            this.readingGoalBooks = readingGoalBooks;
            return this;
        }
        
        public MemberBuilder membershipType(String membershipType) {
            this.membershipType = membershipType;
            return this;
        }
        
        public MemberBuilder membershipStartDate(LocalDateTime membershipStartDate) {
            this.membershipStartDate = membershipStartDate;
            return this;
        }
        
        public MemberBuilder membershipExpiryDate(LocalDateTime membershipExpiryDate) {
            this.membershipExpiryDate = membershipExpiryDate;
            return this;
        }
        
        public MemberBuilder membershipFeePaid(double membershipFeePaid) {
            this.membershipFeePaid = membershipFeePaid;
            return this;
        }
        
        public MemberBuilder maxActiveLoans(int maxActiveLoans) {
            this.maxActiveLoans = maxActiveLoans;
            return this;
        }
        
        public MemberBuilder loanDurationDays(int loanDurationDays) {
            this.loanDurationDays = loanDurationDays;
            return this;
        }
        
        public MemberBuilder fineRatePerDay(double fineRatePerDay) {
            this.fineRatePerDay = fineRatePerDay;
            return this;
        }
        
        public MemberBuilder renewalLimit(int renewalLimit) {
            this.renewalLimit = renewalLimit;
            return this;
        }
        
        public MemberBuilder dateOfBirth(LocalDate dateOfBirth) {
            this.dateOfBirth = dateOfBirth;
            return this;
        }
        
        public MemberBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public MemberBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public MemberBuilder inferenceLevel(String inferenceLevel) {
            this.inferenceLevel = inferenceLevel;
            return this;
        }
        
        public MemberBuilder privacyProjection(String privacyProjection) {
            this.privacyProjection = privacyProjection;
            return this;
        }
        
        public MemberBuilder obfuscationMode(Boolean obfuscationMode) {
            this.obfuscationMode = obfuscationMode;
            return this;
        }
        
        public Member build() {
            Member member = new Member();
            member.id = this.id;
            member.name = this.name;
            member.email = this.email;
            member.phone = this.phone;
            member.gender = this.gender;
            member.passwordHash = this.passwordHash;
            member.plainPassword = this.plainPassword;
            member.role = this.role;
            member.isActive = this.isActive;
            member.registrationDate = this.registrationDate;
            member.dailySequence = this.dailySequence;
            member.timeCreated = this.timeCreated;
            member.lastLogin = this.lastLogin;
            member.loginCount = this.loginCount;
            member.activeLoanCount = this.activeLoanCount;
            member.totalLoans = this.totalLoans;
            member.totalFinesPaid = this.totalFinesPaid;
            member.preferredGenres = this.preferredGenres;
            member.updatedAt = this.updatedAt;
            member.readingGoalBooks = this.readingGoalBooks;
            member.membershipType = this.membershipType;
            member.membershipStartDate = this.membershipStartDate;
            member.membershipExpiryDate = this.membershipExpiryDate;
            member.membershipFeePaid = this.membershipFeePaid;
            member.maxActiveLoans = this.maxActiveLoans;
            member.loanDurationDays = this.loanDurationDays;
            member.fineRatePerDay = this.fineRatePerDay;
            member.renewalLimit = this.renewalLimit;
            member.dateOfBirth = this.dateOfBirth;
            member.createdBy = this.createdBy;
            member.updatedBy = this.updatedBy;
            member.inferenceLevel = this.inferenceLevel;
            member.privacyProjection = this.privacyProjection;
            member.obfuscationMode = this.obfuscationMode;
            return member;
        }
    }
}
