package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookMilestone;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.util.List;
import java.util.Optional;

@Repository
public interface BookMilestoneRepository extends MongoRepository<BookMilestone, String> {
    
    List<BookMilestone> findByMemberId(String memberId);
    
    List<BookMilestone> findByMemberIdAndStatus(String memberId, String status);
    
    List<BookMilestone> findByMemberIdAndIsActiveTrue(String memberId);
    
    Optional<BookMilestone> findByMemberIdAndId(String memberId, String id);
    
    List<BookMilestone> findByMemberIdAndTargetDateBeforeAndStatusNot(String memberId, LocalDate date, String status);
    
    void deleteByMemberIdAndId(String memberId, String id);
}
