package com.goandstudybackend.repository;

import com.goandstudybackend.entity.ChatbotSession;
import org.springframework.data.mongodb.repository.MongoRepository;

import java.util.List;
import java.util.Optional;

public interface ChatbotSessionRepository extends MongoRepository<ChatbotSession, String> {

    Optional<ChatbotSession> findBySessionId(String sessionId);

    List<ChatbotSession> findByMemberIdOrderByLastMessageAtDesc(String memberId);

    List<ChatbotSession> findByMemberIdAndIsActiveTrueOrderByUpdatedAtDesc(String memberId);

    Optional<ChatbotSession> findByIdAndMemberId(String id, String memberId);
}
