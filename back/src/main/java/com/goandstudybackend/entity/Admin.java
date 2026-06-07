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
@Document(collection = "admins")
public class Admin {

    @Id
    private String id;
    private String username;
    @Indexed(unique = true)
    private String email;
    @JsonIgnore
    private String passwordHash;
    private String plainPassword;
    @Builder.Default
    private String role = "ROLE_ADMIN";
    @Builder.Default
    private boolean isActive = true;
    @Builder.Default
    private boolean isSeeded = true;
    private LocalDateTime lastLogin;
    @Builder.Default
    private int loginCount = 0;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
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
    
    public static AdminBuilder builder() {
        return new AdminBuilder();
    }
    
    public static class AdminBuilder {
        private String id;
        private String username;
        private String email;
        private String passwordHash;
        private String plainPassword;
        private String role;
        private boolean isActive;
        private boolean isSeeded;
        private LocalDateTime lastLogin;
        private int loginCount;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public AdminBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public AdminBuilder username(String username) {
            this.username = username;
            return this;
        }
        
        public AdminBuilder email(String email) {
            this.email = email;
            return this;
        }
        
        public AdminBuilder passwordHash(String passwordHash) {
            this.passwordHash = passwordHash;
            return this;
        }
        
        public AdminBuilder plainPassword(String plainPassword) {
            this.plainPassword = plainPassword;
            return this;
        }
        
        public AdminBuilder role(String role) {
            this.role = role;
            return this;
        }
        
        public AdminBuilder isActive(boolean isActive) {
            this.isActive = isActive;
            return this;
        }
        
        public AdminBuilder isSeeded(boolean isSeeded) {
            this.isSeeded = isSeeded;
            return this;
        }
        
        public AdminBuilder lastLogin(LocalDateTime lastLogin) {
            this.lastLogin = lastLogin;
            return this;
        }
        
        public AdminBuilder loginCount(int loginCount) {
            this.loginCount = loginCount;
            return this;
        }
        
        public AdminBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public AdminBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Admin build() {
            Admin admin = new Admin();
            admin.id = this.id;
            admin.username = this.username;
            admin.email = this.email;
            admin.passwordHash = this.passwordHash;
            admin.plainPassword = this.plainPassword;
            admin.role = this.role;
            admin.isActive = this.isActive;
            admin.isSeeded = this.isSeeded;
            admin.lastLogin = this.lastLogin;
            admin.loginCount = this.loginCount;
            admin.createdAt = this.createdAt;
            admin.updatedAt = this.updatedAt;
            return admin;
        }
    }
}
