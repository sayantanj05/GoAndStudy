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
public class LoanResponse {

    private String loanId;
    private String memberId;
    private String bookId;
    private String bookTitle;
    private String bookCoverImageUrl;
    private LocalDateTime issuedAt;
    private LocalDateTime dueDate;
    private LocalDateTime returnedAt;
    private String status;
    private boolean isOverdue;
    private double fineAmount;
    private long daysLeft;
    private int renewalCount;
}
