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
@Document(collection = "book_embeddings")
public class BookEmbedding {
    @Id
    private String id;
    @Indexed
    private String bookId;
    @Indexed
    private String isbn;
    private String title;
    private String embeddingModel;
    private List<Double> embedding = new ArrayList<>();
    private String embeddingSource;
    private int dimensions = 1024;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private transient double score;
    private String createdBy;
    private String updatedBy;
    private String updatedByStaffId;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
    }
    
    public String getBookId() {
        return bookId;
    }
    
    public void setBookId(String bookId) {
        this.bookId = bookId;
    }
    
    public String getIsbn() {
        return isbn;
    }
    
    public void setIsbn(String isbn) {
        this.isbn = isbn;
    }
    
    public String getTitle() {
        return title;
    }
    
    public void setTitle(String title) {
        this.title = title;
    }
    
    public String getEmbeddingModel() {
        return embeddingModel;
    }
    
    public void setEmbeddingModel(String embeddingModel) {
        this.embeddingModel = embeddingModel;
    }
    
    public List<Double> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
    
    public String getEmbeddingSource() {
        return embeddingSource;
    }
    
    public void setEmbeddingSource(String embeddingSource) {
        this.embeddingSource = embeddingSource;
    }
    
    public int getDimensions() {
        return dimensions;
    }
    
    public void setDimensions(int dimensions) {
        this.dimensions = dimensions;
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
    
    public double getScore() {
        return score;
    }
    
    public void setScore(double score) {
        this.score = score;
    }
    
    public String getCreatedBy() {
        return createdBy;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public String getUpdatedBy() {
        return updatedBy;
    }
    
    public void setUpdatedBy(String updatedBy) {
        this.updatedBy = updatedBy;
    }
    
    public String getUpdatedByStaffId() {
        return updatedByStaffId;
    }
    
    public void setUpdatedByStaffId(String updatedByStaffId) {
        this.updatedByStaffId = updatedByStaffId;
    }
    
    public static BookEmbeddingBuilder builder() {
        return new BookEmbeddingBuilder();
    }
    
    public static class BookEmbeddingBuilder {
        private String id;
        private String bookId;
        private String isbn;
        private String title;
        private String embeddingModel;
        private List<Double> embedding = new ArrayList<>();
        private String embeddingSource;
        private int dimensions = 1024;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        private String updatedBy;
        private String updatedByStaffId;
        
        public BookEmbeddingBuilder id(String id) {
            this.id = id;
            return this;
        }
        
        public BookEmbeddingBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public BookEmbeddingBuilder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }
        
        public BookEmbeddingBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public BookEmbeddingBuilder embeddingModel(String embeddingModel) {
            this.embeddingModel = embeddingModel;
            return this;
        }
        
        public BookEmbeddingBuilder embedding(List<Double> embedding) {
            this.embedding = embedding;
            return this;
        }
        
        public BookEmbeddingBuilder embeddingSource(String embeddingSource) {
            this.embeddingSource = embeddingSource;
            return this;
        }
        
        public BookEmbeddingBuilder dimensions(int dimensions) {
            this.dimensions = dimensions;
            return this;
        }
        
        public BookEmbeddingBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public BookEmbeddingBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
        }
        
        public BookEmbeddingBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }
        
        public BookEmbeddingBuilder updatedBy(String updatedBy) {
            this.updatedBy = updatedBy;
            return this;
        }
        
        public BookEmbeddingBuilder updatedByStaffId(String updatedByStaffId) {
            this.updatedByStaffId = updatedByStaffId;
            return this;
        }
        
        public BookEmbedding build() {
            BookEmbedding bookEmbedding = new BookEmbedding();
            bookEmbedding.id = this.id;
            bookEmbedding.bookId = this.bookId;
            bookEmbedding.isbn = this.isbn;
            bookEmbedding.title = this.title;
            bookEmbedding.embeddingModel = this.embeddingModel;
            bookEmbedding.embedding = this.embedding;
            bookEmbedding.embeddingSource = this.embeddingSource;
            bookEmbedding.dimensions = this.dimensions;
            bookEmbedding.createdAt = this.createdAt;
            bookEmbedding.updatedAt = this.updatedAt;
            bookEmbedding.createdBy = this.createdBy;
            bookEmbedding.updatedBy = this.updatedBy;
            bookEmbedding.updatedByStaffId = this.updatedByStaffId;
            return bookEmbedding;
        }
    }
}
