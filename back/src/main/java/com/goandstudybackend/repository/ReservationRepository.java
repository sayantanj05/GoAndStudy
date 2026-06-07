package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Reservation;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ReservationRepository extends MongoRepository<Reservation, String> {

    long countByBookIdAndStatus(String bookId, String status);

    boolean existsByMemberIdAndBookIdAndStatus(String memberId, String bookId, String status);

    boolean existsByBookIdAndStatus(String bookId, String status);

    List<Reservation> findByMemberIdAndStatusIn(String memberId, List<String> statuses);

    List<Reservation> findByBookIdAndStatusOrderByQueuePositionAsc(String bookId, String status);

    Optional<Reservation> findFirstByBookIdAndStatusOrderByReservedAtAsc(String bookId, String status);
}
