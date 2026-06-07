package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatbot_sessions")
public class ChatbotSession {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed(unique = true)
    private String sessionId;
    @Builder.Default
    private List<ChatbotMessage> messages = new ArrayList<>();
    @Builder.Default
    private boolean isActive = true;
    @Indexed
    private LocalDateTime createdAt;
    @Indexed
    private LocalDateTime lastMessageAt;
    private LocalDateTime updatedAt;
    private LocalDateTime endedAt;
    @Builder.Default
    private int totalMessages = 0;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ChatbotMessage {
        private String role;
        private String content;
        private String intent;
        @Builder.Default
        private List<String> retrievedDocs = new ArrayList<>();
        private int tokensUsed;
        private int latencyMs;
        private LocalDateTime timestamp;
    }
}
