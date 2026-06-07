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
@Document(collection = "genre_analytics")
public class GenreAnalytics {

    @Id
    private String id;
    private String genreName;
    private int totalBooks;
    private int totalLoans;
    @Indexed
    private int loansLast30Days;
    private int loansLast7Days;
    private double avgRating;
    private int totalWishlists;
    private double wishlistToLoanRate;
    @Builder.Default
    private List<String> popularBooks = new ArrayList<>();
    @Indexed
    private double trendScore;
    private LocalDateTime computedAt;
    
    // Manual getters to fix compilation issues
    public String getGenreName() {
        return genreName;
    }
    
    public void setGenreName(String genreName) {
        this.genreName = genreName;
    }
    
    public int getLoansLast7Days() {
        return loansLast7Days;
    }
    
    public void setLoansLast7Days(int loansLast7Days) {
        this.loansLast7Days = loansLast7Days;
    }
    
    public double getTrendScore() {
        return trendScore;
    }
    
    public void setTrendScore(double trendScore) {
        this.trendScore = trendScore;
    }
}
