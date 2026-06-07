package com.goandstudybackend.repository;

import com.goandstudybackend.entity.SearchLog;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface SearchLogRepository extends MongoRepository<SearchLog, String> {

    long countByMemberId(String memberId);

    List<SearchLog> findByMemberIdAndCreatedAtBetween(String memberId, LocalDateTime from, LocalDateTime to);
}
