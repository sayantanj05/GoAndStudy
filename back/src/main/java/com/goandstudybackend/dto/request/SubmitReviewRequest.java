package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.Max;
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
public class SubmitReviewRequest {

    @NotBlank
    private String bookId;

    @NotBlank
    private String loanId;

    private String notificationId;

    @NotNull
    @Min(1)
    @Max(5)
    private Integer rating;

    private String reviewText;

    private String feedbackText;
}
