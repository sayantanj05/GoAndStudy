package com.goandstudybackend.repository;

import com.goandstudybackend.entity.ChatbotFeedback;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface ChatbotFeedbackRepository extends MongoRepository<ChatbotFeedback, String> {
}
