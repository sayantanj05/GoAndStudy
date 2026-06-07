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
public class StaffResponse {

    private String staffId;
    private String name;
    private String email;
    private String phone;
    private boolean isActive;
    private String createdByAdminId;
    private LocalDateTime createdAt;
    private LocalDateTime lastLogin;
    private int loginCount;
    private LocalDateTime updatedAt;
    private LocalDateTime deactivatedAt;
}
