package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MlRecommendationResponse {

    private String user_id;
    private String model_type;
    @Builder.Default
    private List<MlBookRecommendation> recommendations = new ArrayList<>();

    public String getUser_id() {
        return user_id;
    }

    public void setUser_id(String user_id) {
        this.user_id = user_id;
    }

    public String getModel_type() {
        return model_type;
    }

    public void setModel_type(String model_type) {
        this.model_type = model_type;
    }

    public List<MlBookRecommendation> getRecommendations() {
        return recommendations;
    }

    public void setRecommendations(List<MlBookRecommendation> recommendations) {
        this.recommendations = recommendations;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class MlBookRecommendation {
        private String book_id;
        private String title;
        private String author;
        private double predicted_rating;

        public static MlBookRecommendationBuilder builder() {
            return new MlBookRecommendationBuilder();
        }

        public String getBook_id() {
            return book_id;
        }

        public void setBook_id(String book_id) {
            this.book_id = book_id;
        }

        public String getTitle() {
            return title;
        }

        public void setTitle(String title) {
            this.title = title;
        }

        public String getAuthor() {
            return author;
        }

        public void setAuthor(String author) {
            this.author = author;
        }

        public double getPredicted_rating() {
            return predicted_rating;
        }

        public void setPredicted_rating(double predicted_rating) {
            this.predicted_rating = predicted_rating;
        }
    }

    // Manual builder pattern to fix compilation issues
    public static MlRecommendationResponseBuilder builder() {
        return new MlRecommendationResponseBuilder();
    }

    
    public static class MlRecommendationResponseBuilder {
        private String user_id;
        private String model_type;
        private List<MlBookRecommendation> recommendations = new ArrayList<>();
        
        public MlRecommendationResponseBuilder user_id(String user_id) {
            this.user_id = user_id;
            return this;
        }
        
        public MlRecommendationResponseBuilder model_type(String model_type) {
            this.model_type = model_type;
            return this;
        }
        
        public MlRecommendationResponseBuilder recommendations(List<MlBookRecommendation> recommendations) {
            this.recommendations = recommendations;
            return this;
        }
        
        public MlRecommendationResponse build() {
            MlRecommendationResponse response = new MlRecommendationResponse();
            response.user_id = this.user_id;
            response.model_type = this.model_type;
            response.recommendations = this.recommendations;
            return response;
        }
    }
    
    // Manual builder for inner class
    public static class MlBookRecommendationBuilder {
        private String book_id;
        private String title;
        private String author;
        private double predicted_rating;
        
        public MlBookRecommendationBuilder book_id(String book_id) {
            this.book_id = book_id;
            return this;
        }
        
        public MlBookRecommendationBuilder title(String title) {
            this.title = title;
            return this;
        }
        
        public MlBookRecommendationBuilder author(String author) {
            this.author = author;
            return this;
        }
        
        public MlBookRecommendationBuilder predicted_rating(double predicted_rating) {
            this.predicted_rating = predicted_rating;
            return this;
        }
        
        public MlBookRecommendation build() {
            MlBookRecommendation recommendation = new MlBookRecommendation();
            recommendation.book_id = this.book_id;
            recommendation.title = this.title;
            recommendation.author = this.author;
            recommendation.predicted_rating = this.predicted_rating;
            return recommendation;
        }
    }
}
