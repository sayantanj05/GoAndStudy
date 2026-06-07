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
@Document(collection = "books")
public class Book {

    @Id
    private String id;
    @Indexed(unique = true)
    private String isbn;
    private String title;
    private String subtitle;
    @Builder.Default
    private List<String> authorIds = new ArrayList<>();
    private String publisherId;
    @Builder.Default
    private List<String> categoryIds = new ArrayList<>();
    private String description;
    @Builder.Default
    private String language = "en";
    private int publishedYear;
    private int pageCount;
    private int totalCopies;
    private int availableCopies;
    @Builder.Default
    private int reservedCopies = 0;
    @Builder.Default
    private double averageRating = 0.0;
    @Builder.Default
    private int totalRatings = 0;
    @Builder.Default
    private int totalIssues = 0;
    @Builder.Default
    private List<Double> embedding = new ArrayList<>();
    private String coverImageUrl;
    private String coverImagePublicId;
    private String coverImageThumbnailUrl;
    private String coverImageFormat;
    @Builder.Default
    private boolean hasCoverImage = false;
    @Builder.Default
    private List<String> tags = new ArrayList<>();
    @Builder.Default
    private boolean isDeleted = false;
    private String deletedByAdminId;
    private LocalDateTime deletedAt;
    private String addedByStaffId;
    private String updatedByStaffId;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
    private String createdBy;
    
    // Manual getters and setters to fix compilation issues
    public String getId() {
        return id;
    }
    
    public void setId(String id) {
        this.id = id;
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
    
    public int getAvailableCopies() {
        return availableCopies;
    }
    
    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    
    public List<String> getAuthorIds() {
        return authorIds;
    }
    
    public void setAuthorIds(List<String> authorIds) {
        this.authorIds = authorIds;
    }
    
    public List<String> getCategoryIds() {
        return categoryIds;
    }
    
    public void setCategoryIds(List<String> categoryIds) {
        this.categoryIds = categoryIds;
    }
    
    public int getTotalCopies() {
        return totalCopies;
    }
    
    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
    }
    
    public String getCoverImageThumbnailUrl() {
        return coverImageThumbnailUrl;
    }
    
    public void setCoverImageThumbnailUrl(String coverImageThumbnailUrl) {
        this.coverImageThumbnailUrl = coverImageThumbnailUrl;
    }
    
    public String getCoverImageFormat() {
        return coverImageFormat;
    }
    
    public void setCoverImageFormat(String coverImageFormat) {
        this.coverImageFormat = coverImageFormat;
    }
    
    public List<Double> getEmbedding() {
        return embedding;
    }
    
    public void setEmbedding(List<Double> embedding) {
        this.embedding = embedding;
    }
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
    }
    
    public boolean isHasCoverImage() {
        return hasCoverImage;
    }
    
    public void setHasCoverImage(boolean hasCoverImage) {
        this.hasCoverImage = hasCoverImage;
    }
    
    public Integer getPageCount() {
        return pageCount;
    }
    
    public void setPageCount(Integer pageCount) {
        this.pageCount = pageCount;
    }
    
    public Integer getPublishedYear() {
        return publishedYear;
    }
    
    public void setPublishedYear(Integer publishedYear) {
        this.publishedYear = publishedYear;
    }
    
    public String getLanguage() {
        return language;
    }
    
    public void setLanguage(String language) {
        this.language = language;
    }
    
    public List<String> getTags() {
        return tags;
    }
    
    public void setTags(List<String> tags) {
        this.tags = tags;
    }
    
    public void setUpdatedByStaffId(String updatedByStaffId) {
        this.updatedByStaffId = updatedByStaffId;
    }
    
    public void setCreatedBy(String createdBy) {
        this.createdBy = createdBy;
    }
    
    public double getAverageRating() {
        return averageRating;
    }
    
    public void setAverageRating(double averageRating) {
        this.averageRating = averageRating;
    }
    
    public int getTotalRatings() {
        return totalRatings;
    }
    
    public void setTotalRatings(int totalRatings) {
        this.totalRatings = totalRatings;
    }
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
    
    public String getDeletedByAdminId() {
        return deletedByAdminId;
    }
    
    public void setDeletedByAdminId(String deletedByAdminId) {
        this.deletedByAdminId = deletedByAdminId;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getDeletedAt() {
        return deletedAt;
    }
    
    public void setDeletedAt(LocalDateTime deletedAt) {
        this.deletedAt = deletedAt;
    }
    
    public static BookBuilder builder() {
        return new BookBuilder();
    }
    
    public static class BookBuilder {
        private String id;
        private String isbn;
        private String title;
        private String subtitle;
        private List<String> authorIds = new ArrayList<>();
        private String publisherId;
        private List<String> categoryIds = new ArrayList<>();
        private String description;
        private String language = "en";
        private int publishedYear;
        private int pageCount;
        private int totalCopies;
        private int availableCopies;
        private int reservedCopies = 0;
        private double averageRating = 0.0;
        private int totalRatings = 0;
        private int totalIssues = 0;
        private List<Double> embedding = new ArrayList<>();
        private String coverImageUrl;
        private String coverImagePublicId;
        private String coverImageThumbnailUrl;
        private String coverImageFormat;
        private boolean hasCoverImage = false;
        private List<String> tags = new ArrayList<>();
        private boolean isDeleted = false;
        private String deletedByAdminId;
        private LocalDateTime deletedAt;
        private String addedByStaffId;
        private String updatedByStaffId;
        private LocalDateTime createdAt;
        private LocalDateTime updatedAt;
        private String createdBy;
        
        public BookBuilder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }
        
        public BookBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public BookBuilder subtitle(String subtitle) {
            this.subtitle = subtitle;
            return this;
        }
        
        public BookBuilder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }
        
        public BookBuilder coverImageThumbnailUrl(String coverImageThumbnailUrl) {
            this.coverImageThumbnailUrl = coverImageThumbnailUrl;
            return this;
        }
        
        public BookBuilder hasCoverImage(boolean hasCoverImage) {
            this.hasCoverImage = hasCoverImage;
            return this;
        }
        
        public BookBuilder coverImageFormat(String coverImageFormat) {
            this.coverImageFormat = coverImageFormat;
            return this;
        }
        
        public BookBuilder authorIds(List<String> authorIds) {
            this.authorIds = authorIds;
            return this;
        }
        
        public BookBuilder publisherId(String publisherId) {
            this.publisherId = publisherId;
            return this;
        }
        
        public BookBuilder categoryIds(List<String> categoryIds) {
            this.categoryIds = categoryIds;
            return this;
        }
        
        public BookBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public BookBuilder language(String language) {
            this.language = language;
            return this;
        }
        
        public BookBuilder publishedYear(int publishedYear) {
            this.publishedYear = publishedYear;
            return this;
        }
        
        public BookBuilder pageCount(int pageCount) {
            this.pageCount = pageCount;
            return this;
        }
        
        public BookBuilder totalCopies(int totalCopies) {
            this.totalCopies = totalCopies;
            return this;
        }
        
        public BookBuilder availableCopies(int availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }
        
        public BookBuilder tags(List<String> tags) {
            this.tags = tags;
            return this;
        }
        
        public BookBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public BookBuilder updatedAt(LocalDateTime updatedAt) {
            this.updatedAt = updatedAt;
            return this;
}

        public BookBuilder createdBy(String createdBy) {
            this.createdBy = createdBy;
            return this;
        }

        public BookBuilder addedByStaffId(String addedByStaffId) {
            this.addedByStaffId = addedByStaffId;
            return this;
        }

        public Book build() {
            Book book = new Book();
            book.id = this.id;
            book.isbn = this.isbn;
            book.title = this.title;
            book.subtitle = this.subtitle;
            book.authorIds = this.authorIds;
            book.publisherId = this.publisherId;
            book.categoryIds = this.categoryIds;
            book.description = this.description;
            book.language = this.language;
            book.publishedYear = this.publishedYear;
            book.pageCount = this.pageCount;
            book.totalCopies = this.totalCopies;
            book.availableCopies = this.availableCopies;
            book.reservedCopies = this.reservedCopies;
            book.averageRating = this.averageRating;
            book.totalRatings = this.totalRatings;
            book.totalIssues = this.totalIssues;
            book.embedding = this.embedding;
            book.coverImageUrl = this.coverImageUrl;
            book.coverImagePublicId = this.coverImagePublicId;
            book.coverImageThumbnailUrl = this.coverImageThumbnailUrl;
            book.coverImageFormat = this.coverImageFormat;
            book.hasCoverImage = this.hasCoverImage;
            book.tags = this.tags;
            book.isDeleted = this.isDeleted;
            book.deletedByAdminId = this.deletedByAdminId;
            book.deletedAt = this.deletedAt;
            book.addedByStaffId = this.addedByStaffId;
            book.updatedByStaffId = this.updatedByStaffId;
            book.createdAt = this.createdAt;
            book.updatedAt = this.updatedAt;
            book.createdBy = this.createdBy;
            return book;
        }
    }
}
