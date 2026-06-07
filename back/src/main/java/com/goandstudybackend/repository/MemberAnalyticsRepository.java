package com.goandstudybackend.repository;

import com.goandstudybackend.entity.MemberAnalytics;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface MemberAnalyticsRepository extends MongoRepository<MemberAnalytics, String> {
}
