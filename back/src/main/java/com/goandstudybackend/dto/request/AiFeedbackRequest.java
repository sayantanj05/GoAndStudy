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
public class AiFeedbackRequest {

    @NotBlank
    private String aiLogId;

    @NotBlank
    private String recommendedBookId;

    @NotBlank
    private String actionTaken;
}
