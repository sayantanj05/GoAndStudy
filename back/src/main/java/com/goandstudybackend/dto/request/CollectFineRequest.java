package com.goandstudybackend.dto.request;

import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class CollectFineRequest {

    @NotNull
    @DecimalMin("0.0")
    private Double amountCollected;
    
    // Manual getters and setters to fix compilation issues
    public Double getAmountCollected() {
        return amountCollected;
    }
    
    public void setAmountCollected(Double amountCollected) {
        this.amountCollected = amountCollected;
    }
}
