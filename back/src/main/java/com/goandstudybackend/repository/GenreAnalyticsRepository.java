package com.goandstudybackend.repository;

import com.goandstudybackend.entity.GenreAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface GenreAnalyticsRepository extends MongoRepository<GenreAnalytics, String> {

    List<GenreAnalytics> findTop5ByOrderByLoansLast7DaysDesc();

    List<GenreAnalytics> findTop3ByOrderByTrendScoreDesc();

    List<GenreAnalytics> findAllByOrderByLoansLast30DaysDesc();
}
