package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotMessageResponse {

    private String sessionId;
    private int messageIndex;
    private String userMessage;
    private String assistantReply;
    private String intent;
    private String retrievedContext;
    private int tokensUsed;
    private int latencyMs;
    private LocalDateTime timestamp;
}
