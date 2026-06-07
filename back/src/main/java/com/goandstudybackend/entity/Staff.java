package com.goandstudybackend.entity;

import com.fasterxml.jackson.annotation.JsonIgnore;
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
@Document(collection = "staff")
public class Staff {

    @Id
    private String id;
    private String name;
    @Indexed(unique = true)
    private String email;
    private String phone;
    @JsonIgnore
    private String passwordHash;
    private String plainPassword;
    @Builder.Default
    private String role = "ROLE_STAFF";
    @Builder.Default
    private boolean isActive = true;
    private String createdByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    @Builder.Default
    private int loginCount = 0;
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;
    private String deactivatedByAdminId;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getEmail() {
        return email;
    }
    
    public void setEmail(String email) {
        this.email = email;
    }
    
    public boolean isActive() {
        return isActive;
    }
    
    public void setActive(boolean isActive) {
        this.isActive = isActive;
    }
    
    public static StaffBuilder builder() {
        return new StaffBuilder();
    }
    
    public static class StaffBuilder {
        private String id;
        private String name;
        private String email;
        private String phone;
        private String passwordHash;
        private String plainPassword;
        private String role;
        private boolean isActive;
        private String createdByAdminId;
        private LocalDateTime createdAt;
        private LocalDateTime lastLogin;
        private int loginCount;
        private LocalDateTime updatedAt;
        private LocalDateTime deactivatedAt;
        private String deactivatedByAdminId;
        
        public StaffBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public StaffBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public StaffBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public StaffBuilder phone(String phone) {
            this.phone = phone;
            return this;
        }
        
        public StaffBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }
        
        public StaffBuilder plainPassword(String plainPassword) {
            this.plainPassword = plainPassword;
            return this;
        }
        
        public StaffBuilder role(String role) {
            this.role = role;
            return this;
        }
        
        public StaffBuilder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public StaffBuilder createdByAdminId(String createdByAdminId) {
            this.createdByAdminId = createdByAdminId;
            return this;
        }
        
        public StaffBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public StaffBuilder lastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }
        
        public StaffBuilder loginCount(int loginCount) {
            this.loginCount = loginCount;
            return this;
        }
        
        public StaffBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public StaffBuilder deactivatedAt(LocalDateTime deactivatedAt) {
            this.deactivatedAt = deactivatedAt;
            return this;
        }
        
        public StaffBuilder deactivatedByAdminId(String deactivatedByAdminId) {
            this.deactivatedByAdminId = deactivatedByAdminId;
            return this;
        }
        
        public Staff build() {
            Staff staff = new Staff();
            staff.id = this.id;
            staff.name = this.name;
            staff.email = this.email;
            staff.phone = this.phone;
            staff.passwordHash = this.passwordHash;
            staff.plainPassword = this.plainPassword;
            staff.role = this.role;
            staff.isActive = this.isActive;
            staff.createdByAdminId = this.createdByAdminId;
            staff.createdAt = this.createdAt;
            staff.lastLogin = this.lastLogin;
            staff.loginCount = this.loginCount;
            staff.updatedAt = this.updatedAt;
            staff.deactivatedAt = this.deactivatedAt;
            staff.deactivatedByAdminId = this.deactivatedByAdminId;
            return staff;
        }
    }
}
