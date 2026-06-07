package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class SetReadingGoalRequest {

    @NotNull
    @Min(1)
    @Max(365)
    private Integer targetBooks;

    private Integer year;
}
