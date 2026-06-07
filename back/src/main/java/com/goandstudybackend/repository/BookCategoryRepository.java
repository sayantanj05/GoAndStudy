package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookCategory;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface BookCategoryRepository extends MongoRepository<BookCategory, String> {

    Optional<BookCategory> findBySlug(String slug);

    List<BookCategory> findAllByOrderByNameAsc();
}
