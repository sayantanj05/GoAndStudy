package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ChatbotFeedbackRequest {

    @NotBlank
    private String sessionId;

    @NotNull
    @Min(0)
    private Integer messageIndex;

    @NotBlank
    private String rating;

    private String feedbackText;

    public String getMessageId() {
        return String.valueOf(messageIndex);
    }

    public String getFeedback() {
        return feedbackText;
    }
}
