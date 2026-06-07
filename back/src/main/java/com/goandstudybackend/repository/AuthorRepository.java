package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Author;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;

public interface AuthorRepository extends MongoRepository<Author, String> {

    List<Author> findByNameContainingIgnoreCase(String name);

    java.util.Optional<Author> findByNameIgnoreCase(String name);
}
