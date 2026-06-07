package com.goandstudybackend.repository;

import com.goandstudybackend.entity.AiLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface AiLogRepository extends MongoRepository<AiLog, String> {

    List<AiLog> findByCreatedAtBetween(LocalDateTime from, LocalDateTime to);

    List<AiLog> findByMemberIdOrderByCreatedAtDesc(String memberId);

    long countByMemberId(String memberId);

    long countByWasActedOnTrue();

    Optional<AiLog> findByIdAndMemberId(String id, String memberId);
}
