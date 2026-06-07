package com.goandstudybackend.repository;

import com.goandstudybackend.entity.LoanEvent;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.List;

public interface LoanEventRepository extends MongoRepository<LoanEvent, String> {
    List<LoanEvent> findByLoanIdOrderByTimestampAsc(String loanId);
List<LoanEvent> findByEventTypeOrderByTimestampDesc(com.goandstudybackend.enumeration.LoanEventType eventType);
}
