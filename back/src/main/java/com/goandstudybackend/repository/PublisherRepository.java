package com.goandstudybackend.repository;

import com.goandstudybackend.entity.Publisher;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface PublisherRepository extends MongoRepository<Publisher, String> {
}
