package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookQueue;
import org.springframework.data.mongodb.repository.MongoRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface BookQueueRepository extends MongoRepository<BookQueue, String> {
    
    List<BookQueue> findByMemberId(String memberId);
    
    Optional<BookQueue> findByMemberIdAndIsActiveTrue(String memberId);
    
    Optional<BookQueue> findByMemberIdAndId(String memberId, String id);
    
    void deleteByMemberIdAndId(String memberId, String id);
}
