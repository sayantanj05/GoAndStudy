package com.goandstudybackend.repository;

import com.goandstudybackend.entity.ActivityLog;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface ActivityLogRepository extends MongoRepository<ActivityLog, String> {

    Page<ActivityLog> findByActorIdOrderByCreatedAtDesc(String actorId, Pageable pageable);

    List<ActivityLog> findTop10ByOrderByCreatedAtDesc();

    List<ActivityLog> findTop8ByActorIdOrderByCreatedAtDesc(String actorId);
}
