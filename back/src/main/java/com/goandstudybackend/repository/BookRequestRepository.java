package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookRequest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BookRequestRepository extends MongoRepository<BookRequest, String> {

    boolean existsByMemberIdAndTitleIgnoreCaseAndCreatedAtBetween(String memberId, String title, LocalDateTime from, LocalDateTime to);

    List<BookRequest> findByMemberIdOrderByCreatedAtDesc(String memberId);

    Page<BookRequest> findByStatusOrderByCreatedAtDesc(String status, Pageable pageable);

    Page<BookRequest> findAllByOrderByCreatedAtDesc(Pageable pageable);
}
