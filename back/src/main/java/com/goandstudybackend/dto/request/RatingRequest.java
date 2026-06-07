package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Size;
import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RatingRequest {
    @NotBlank
    private String loanId;
    @Min(1)
    @Max(5)
    private int rating;
    @Size(min = 0, max = 1000)
    private String reviewText;
}
