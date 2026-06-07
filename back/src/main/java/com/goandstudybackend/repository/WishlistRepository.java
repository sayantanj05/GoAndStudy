package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Wishlist;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface WishlistRepository extends MongoRepository<Wishlist, String> {

    List<Wishlist> findByMemberIdOrderByAddedAtDesc(String memberId);

    Optional<Wishlist> findByMemberIdAndBookId(String memberId, String bookId);

    boolean existsByMemberIdAndBookId(String memberId, String bookId);

    long countByMemberId(String memberId);

    List<Wishlist> findByBookId(String bookId);
}
