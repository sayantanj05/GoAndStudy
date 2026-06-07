package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookResponse {

    private String bookId;
    private String isbn;
    private String title;
    @Builder.Default
    private List<String> authorIds = new ArrayList<>();
    private String author;
    private String category;
    private String genre;
    @Builder.Default
    private List<String> categoryIds = new ArrayList<>();
    private String coverImageUrl;
    private String coverImageThumbnailUrl;
    private boolean hasCoverImage;
    private int availableCopies;
    private int totalCopies;
    private double averageRating;
    private int totalRatings;
    private String description;
    private Integer pageCount;
    private Integer publishedYear;
    private String language;
    private boolean isDeleted;
    private LocalDateTime createdAt;
    
    // Manual getters and setters to fix compilation issues
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
    
    public List<String> getAuthorIds() {
        return authorIds;
    }
    
    public void setAuthorIds(List<String> authorIds) {
        this.authorIds = authorIds;
    }
    
    public String getAuthor() {
        return author;
    }
    
    public void setAuthor(String author) {
        this.author = author;
    }
    
    public String getCategory() {
        return category;
    }
    
    public void setCategory(String category) {
        this.category = category;
    }
    
    public String getGenre() {
        return genre;
    }
    
    public void setGenre(String genre) {
        this.genre = genre;
    }
    
    public String getCoverImageUrl() {
        return coverImageUrl;
    }
    
    public void setCoverImageUrl(String coverImageUrl) {
        this.coverImageUrl = coverImageUrl;
    }
    
    public String getCoverImageThumbnailUrl() {
        return coverImageThumbnailUrl;
    }
    
    public void setCoverImageThumbnailUrl(String coverImageThumbnailUrl) {
        this.coverImageThumbnailUrl = coverImageThumbnailUrl;
    }
    
    public boolean isHasCoverImage() {
        return hasCoverImage;
    }
    
    public void setHasCoverImage(boolean hasCoverImage) {
        this.hasCoverImage = hasCoverImage;
    }
    
    public int getAvailableCopies() {
        return availableCopies;
    }
    
    public void setAvailableCopies(int availableCopies) {
        this.availableCopies = availableCopies;
    }
    
    public int getTotalCopies() {
        return totalCopies;
    }
    
    public void setTotalCopies(int totalCopies) {
        this.totalCopies = totalCopies;
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
    
    public String getDescription() {
        return description;
    }
    
    public void setDescription(String description) {
        this.description = description;
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
    
    public boolean isDeleted() {
        return isDeleted;
    }
    
    public void setDeleted(boolean deleted) {
        isDeleted = deleted;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public static BookResponseBuilder builder() {
        return new BookResponseBuilder();
    }
    
    public static class BookResponseBuilder {
        private String bookId;
        private String isbn;
        private String title;
        private List<String> authorIds = new ArrayList<>();
        private String author;
        private String category;
        private String genre;
        private List<String> categoryIds = new ArrayList<>();
        private String coverImageUrl;
        private String coverImageThumbnailUrl;
        private boolean hasCoverImage;
        private int availableCopies;
        private int totalCopies;
        private double averageRating;
        private int totalRatings;
        private String description;
        private Integer pageCount;
        private Integer publishedYear;
        private String language;
        private boolean isDeleted;
        private LocalDateTime createdAt;
        
        public BookResponseBuilder bookId(String bookId) {
            this.bookId = bookId;
            return this;
        }
        
        public BookResponseBuilder isbn(String isbn) {
            this.isbn = isbn;
            return this;
        }
        
        public BookResponseBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public BookResponseBuilder authorIds(List<String> authorIds) {
            this.authorIds = authorIds;
            return this;
        }
        
        public BookResponseBuilder author(String author) {
            this.author = author;
            return this;
        }
        
        public BookResponseBuilder category(String category) {
            this.category = category;
            return this;
        }
        
        public BookResponseBuilder genre(String genre) {
            this.genre = genre;
            return this;
        }
        
        public BookResponseBuilder categoryIds(List<String> categoryIds) {
            this.categoryIds = categoryIds;
            return this;
        }
        
        public BookResponseBuilder coverImageUrl(String coverImageUrl) {
            this.coverImageUrl = coverImageUrl;
            return this;
        }
        
        public BookResponseBuilder coverImageThumbnailUrl(String coverImageThumbnailUrl) {
            this.coverImageThumbnailUrl = coverImageThumbnailUrl;
            return this;
        }
        
        public BookResponseBuilder hasCoverImage(boolean hasCoverImage) {
            this.hasCoverImage = hasCoverImage;
            return this;
        }
        
        public BookResponseBuilder availableCopies(int availableCopies) {
            this.availableCopies = availableCopies;
            return this;
        }
        
        public BookResponseBuilder totalCopies(int totalCopies) {
            this.totalCopies = totalCopies;
            return this;
        }
        
        public BookResponseBuilder averageRating(double averageRating) {
            this.averageRating = averageRating;
            return this;
        }
        
        public BookResponseBuilder totalRatings(int totalRatings) {
            this.totalRatings = totalRatings;
            return this;
        }
        
        public BookResponseBuilder description(String description) {
            this.description = description;
            return this;
        }
        
        public BookResponseBuilder pageCount(Integer pageCount) {
            this.pageCount = pageCount;
            return this;
        }
        
        public BookResponseBuilder publishedYear(Integer publishedYear) {
            this.publishedYear = publishedYear;
            return this;
        }
        
        public BookResponseBuilder language(String language) {
            this.language = language;
            return this;
        }
        
        public BookResponseBuilder isDeleted(boolean deleted) {
            isDeleted = deleted;
            return this;
        }
        
        public BookResponseBuilder createdAt(LocalDateTime createdAt) {
            this.createdAt = createdAt;
            return this;
        }
        
        public BookResponse build() {
            BookResponse bookResponse = new BookResponse();
            bookResponse.bookId = this.bookId;
            bookResponse.isbn = this.isbn;
            bookResponse.title = this.title;
            bookResponse.authorIds = this.authorIds;
            bookResponse.author = this.author;
            bookResponse.category = this.category;
            bookResponse.genre = this.genre;
            bookResponse.categoryIds = this.categoryIds;
            bookResponse.coverImageUrl = this.coverImageUrl;
            bookResponse.coverImageThumbnailUrl = this.coverImageThumbnailUrl;
            bookResponse.hasCoverImage = this.hasCoverImage;
            bookResponse.availableCopies = this.availableCopies;
            bookResponse.totalCopies = this.totalCopies;
            bookResponse.averageRating = this.averageRating;
            bookResponse.totalRatings = this.totalRatings;
            bookResponse.description = this.description;
            bookResponse.pageCount = this.pageCount;
            bookResponse.publishedYear = this.publishedYear;
            bookResponse.language = this.language;
            bookResponse.isDeleted = this.isDeleted;
            bookResponse.createdAt = this.createdAt;
            return bookResponse;
        }
    }
}
