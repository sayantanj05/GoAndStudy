package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class MemberResponse {

    private String memberId;
    private String name;
    private String email;
    private String phone;
    private String gender;
    private boolean isActive;
    private LocalDateTime timeCreated;
    private LocalDateTime lastLogin;
    private int activeLoanCount;
    private int totalLoans;
    private int loginCount;
    private String membershipType;
    private LocalDateTime membershipExpiryDate;
    private int maxActiveLoans;
    private int loanDurationDays;
    private double fineRatePerDay;
private int renewalLimit;
    private LocalDate dateOfBirth;
    private int age;
    private String plainPassword;
}

