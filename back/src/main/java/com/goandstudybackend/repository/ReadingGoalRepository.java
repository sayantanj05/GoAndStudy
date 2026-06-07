package com.goandstudybackend.repository;

import com.goandstudybackend.entity.ReadingGoal;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface ReadingGoalRepository extends MongoRepository<ReadingGoal, String> {

    Optional<ReadingGoal> findByMemberId(String memberId);
}
