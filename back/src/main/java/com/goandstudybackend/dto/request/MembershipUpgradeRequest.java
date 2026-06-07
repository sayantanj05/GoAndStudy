package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
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
public class MembershipUpgradeRequest {

    @NotBlank
    private String membershipType;

    @NotNull
    @DecimalMin("0.0")
    private Double membershipFeePaid;
}
