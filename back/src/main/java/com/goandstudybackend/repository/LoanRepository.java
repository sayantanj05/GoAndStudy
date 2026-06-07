package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Loan;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface LoanRepository extends MongoRepository<Loan, String> {

    long countByStatus(String status);

    long countByMemberIdAndStatusAndReturnedAtBetween(String memberId, String status, LocalDateTime from, LocalDateTime to);

    long countByIssuedByStaffIdAndIssuedAtGreaterThanEqual(String staffId, LocalDateTime from);

    long countByReturnedByStaffIdAndReturnedAtGreaterThanEqual(String staffId, LocalDateTime from);

    long countByBookIdAndStatusIn(String bookId, List<String> statuses);

    List<Loan> findByMemberIdAndStatusIn(String memberId, List<String> statuses);

    List<Loan> findByMemberIdAndStatusOrderByReturnedAtDesc(String memberId, String status);

    List<Loan> findTop10ByMemberIdAndStatusOrderByReturnedAtDesc(String memberId, String status);

    List<Loan> findTop5ByMemberIdOrderByIssuedAtDesc(String memberId);

    List<Loan> findByStatus(String status);

    List<Loan> findByBookIdAndStatusIn(String bookId, List<String> statuses);

    List<Loan> findByDueDateBeforeAndStatus(LocalDateTime dueDate, String status);

    List<Loan> findByMemberIdAndStatusInOrderByDueDateAsc(String memberId, List<String> statuses);

    long countByMemberIdAndBookTitleAndStatusIn(String memberId, String bookTitle, List<String> statuses);

    List<Loan> findByBookIsbn(String bookIsbn);
}
