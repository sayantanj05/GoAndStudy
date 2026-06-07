package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Book;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends MongoRepository<Book, String> {

    Optional<Book> findByIsbn(String isbn);

    long countByIsDeletedFalse();

    List<Book> findTop6ByIsDeletedFalseOrderByAverageRatingDesc();

    List<Book> findTop6ByIsDeletedFalseOrderByCreatedAtDesc();

    Page<Book> findByIsDeletedFalse(Pageable pageable);

    List<Book> findByAuthorIdsContainingAndIsDeletedFalseOrderByAverageRatingDesc(String authorId);

    List<Book> findByCategoryIdsContainingAndIsDeletedFalse(String categoryId);

    long countByCategoryIdsContainingAndIsDeletedFalse(String categoryId);

    long countByAuthorIdsContainingAndIsDeletedFalse(String authorId);
}

