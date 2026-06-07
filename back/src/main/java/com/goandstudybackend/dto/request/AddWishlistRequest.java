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
public class AddWishlistRequest {

    @NotBlank
    private String bookId;

    private String addedFrom;

    @Builder.Default
    private Boolean notifyOnAvailable = false;
}
