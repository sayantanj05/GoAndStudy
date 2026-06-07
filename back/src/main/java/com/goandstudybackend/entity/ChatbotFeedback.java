package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "chatbot_feedback")
public class ChatbotFeedback {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String sessionId;
    private int messageIndex;
    private String rating;
    private String feedbackText;
    @Indexed
    private LocalDateTime createdAt;
}
