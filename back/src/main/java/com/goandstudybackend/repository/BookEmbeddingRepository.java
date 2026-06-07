package com.goandstudybackend.repository;

import com.goandstudybackend.entity.BookEmbedding;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.Optional;

public interface BookEmbeddingRepository extends MongoRepository<BookEmbedding, String> {

    Optional<BookEmbedding> findByBookId(String bookId);

    Optional<BookEmbedding> findByIsbn(String isbn);
}
