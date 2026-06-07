package com.goandstudybackend.repository;

import com.goandstudybackend.entity.RenewalRequest;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface RenewalRequestRepository extends MongoRepository<RenewalRequest, String> {

    boolean existsByLoanIdAndStatus(String loanId, String status);

    List<RenewalRequest> findByMemberIdOrderByRequestedAtDesc(String memberId);

    List<RenewalRequest> findByStatusOrderByRequestedAtDesc(String status);
}
