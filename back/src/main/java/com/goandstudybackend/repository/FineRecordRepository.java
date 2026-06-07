package com.goandstudybackend.repository;

import com.goandstudybackend.entity.FineRecord;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface FineRecordRepository extends MongoRepository<FineRecord, String> {

    Optional<FineRecord> findByLoanId(String loanId);

    boolean existsByLoanId(String loanId);

    List<FineRecord> findByMemberIdAndStatus(String memberId, String status);

    List<FineRecord> findByStatusOrderByCreatedAtDesc(String status);

    Page<FineRecord> findByStatus(String status, Pageable pageable);

    long countByStatus(String status);

    List<FineRecord> findByStatusAndCollectedAtGreaterThanEqual(String status, LocalDateTime from);
}
