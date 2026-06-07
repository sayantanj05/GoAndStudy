package com.goandstudybackend.repository;

import com.goandstudybackend.entity.RecommendationFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface RecommendationFeedbackRepository extends MongoRepository<RecommendationFeedback, String> {
}
