package com.goandstudybackend.repository;

import com.goandstudybackend.entity.ReadingSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ReadingSessionRepository extends MongoRepository<ReadingSession, String> {

    List<ReadingSession> findByMemberId(String memberId);
}
