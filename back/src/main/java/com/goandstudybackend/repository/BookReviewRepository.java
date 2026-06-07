package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookReview;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface BookReviewRepository extends MongoRepository<BookReview, String> {

    boolean existsByMemberIdAndBookId(String memberId, String bookId);

    List<BookReview> findByBookId(String bookId);

    List<BookReview> findByMemberIdOrderByCreatedAtDesc(String memberId);

    long countByBookId(String bookId);
}
