package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Notification;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface NotificationRepository extends MongoRepository<Notification, String> {

    List<Notification> findByMemberIdOrderByCreatedAtDesc(String memberId);

    List<Notification> findByMemberIdAndStatusOrderByCreatedAtDesc(String memberId, String status);

    long countByMemberIdAndStatus(String memberId, String status);

    boolean existsByMemberIdAndTypeAndTitleContaining(String memberId, String type, String title);

    List<Notification> findByMemberIdAndRelatedLoanIdAndTypeAndCreatedAtAfter(String memberId, String relatedLoanId, String type, LocalDateTime createdAt);
}
