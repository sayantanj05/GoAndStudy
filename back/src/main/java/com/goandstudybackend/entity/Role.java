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
@Document(collection = "roles")
public class Role {

    @Id
    private String id;

    @Indexed(unique = true)
    private String name;

    private String description;

    private String createdByAdminId;

    @Builder.Default
    private LocalDateTime createdAt = LocalDateTime.now();

    @Builder.Default
    private LocalDateTime updatedAt = LocalDateTime.now();
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public static RoleBuilder builder() {
        return new RoleBuilder();
    }
    
    public static class RoleBuilder {
        private String id;
        private String name;
        private String description;
        private String createdByAdminId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public RoleBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public RoleBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public RoleBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public RoleBuilder createdByAdminId(String createdByAdminId) {
            this.createdByAdminId = createdByAdminId;
            return this;
        }
        
        public RoleBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public RoleBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Role build() {
            Role role = new Role();
            role.id = this.id;
            role.name = this.name;
            role.description = this.description;
            role.createdByAdminId = this.createdByAdminId;
            role.createdAt = this.createdAt;
            role.updatedAt = this.updatedAt;
            return role;
        }
    }
}
