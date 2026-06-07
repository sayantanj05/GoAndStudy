package com.goandstudybackend.repository;

import com.goandstudybackend.entity.RecommendationLog;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface RecommendationLogRepository extends MongoRepository<RecommendationLog, String> {
    List<RecommendationLog> findByUserIdOrderByGeneratedAtDesc(String userId);
    List<RecommendationLog> findByUserIdAndGeneratedAtAfter(String userId, LocalDateTime since);
}
