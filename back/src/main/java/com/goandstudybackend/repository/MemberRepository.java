package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Member;
import org.springframework.data.mongodb.repository.MongoRepository;
import java.util.Optional;

public interface MemberRepository extends MongoRepository<Member, String> {
    Optional<Member> findByEmail(String email);
    Optional<Member> findByEmailIgnoreCase(String email);
    long countByActiveLoanCountGreaterThan(int count);
    long countByIsActive(boolean isActive);
    long countByRegistrationDate(String registrationDate);
}

