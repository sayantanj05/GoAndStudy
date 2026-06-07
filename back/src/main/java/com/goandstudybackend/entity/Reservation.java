package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "reservations")
public class Reservation {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String bookId;
    private String bookTitle;
    private String status;
    private int queuePosition;
    @Indexed
    private LocalDateTime reservedAt;
    private LocalDateTime notifiedAt;
    @Indexed
    private LocalDateTime expiresAt;
    private String fulfilledLoanId;
}
