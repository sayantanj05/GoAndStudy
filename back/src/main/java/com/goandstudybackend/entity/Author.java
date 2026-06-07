package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "authors")
public class Author {

    @Id
    private String id;
    @Indexed
    private String name;
    private String bio;
    private String nationality;
    @Builder.Default
    private List<String> genres = new ArrayList<>();
    private int totalBooks;
    private double averageRating;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    
    // Manual getters and setters to fix compilation issues
    public String getName() {
        return name;
    }
    
    public void setName(String name) {
        this.name = name;
    }
    
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public int getTotalBooks() {
        return totalBooks;
    }
    
    public void setTotalBooks(int totalBooks) {
        this.totalBooks = totalBooks;
    }
    
    public List<String> getGenres() {
        return genres;
    }
    
    public void setGenres(List<String> genres) {
        this.genres = genres;
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getBio() {
        return bio;
    }
    
    public void setBio(String bio) {
        this.bio = bio;
    }
    
    public String getNationality() {
        return nationality;
    }
    
    public void setNationality(String nationality) {
        this.nationality = nationality;
    }
    
    public static AuthorBuilder builder() {
        return new AuthorBuilder();
    }
    
    public static class AuthorBuilder {
        private String id;
        private String name;
        private String bio;
        private String nationality;
        private List<String> genres = new ArrayList<>();
        private int totalBooks;
        private double averageRating;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        
        public AuthorBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public AuthorBuilder name(String name) {
            this.name = name;
            return this;
        }
        
        public AuthorBuilder bio(String bio) {
            this.bio = bio;
            return this;
        }
        
        public AuthorBuilder nationality(String nationality) {
            this.nationality = nationality;
            return this;
        }
        
        public AuthorBuilder genres(List<String> genres) {
            this.genres = genres;
            return this;
        }
        
        public AuthorBuilder totalBooks(int totalBooks) {
            this.totalBooks = totalBooks;
            return this;
        }
        
        public AuthorBuilder averageRating(double averageRating) {
            this.averageRating = averageRating;
            return this;
        }
        
        public AuthorBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public AuthorBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public Author build() {
            Author author = new Author();
            author.id = this.id;
            author.name = this.name;
            author.bio = this.bio;
            author.nationality = this.nationality;
            author.genres = this.genres;
            author.totalBooks = this.totalBooks;
            author.averageRating = this.averageRating;
            author.createdAt = this.createdAt;
            author.updatedAt = this.updatedAt;
            return author;
        }
    }
}
