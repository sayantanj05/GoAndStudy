package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AiRecommendationRequest {

    @NotBlank
    private String query;

    private String sessionId;

    @Builder.Default
    private Boolean excludeRead = true;
}
