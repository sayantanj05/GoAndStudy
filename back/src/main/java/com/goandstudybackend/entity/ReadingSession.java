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
@Document(collection = "reading_sessions")
public class ReadingSession {

    @Id
    private String id;
    @Indexed
    private String memberId;
    private String bookId;
    @Indexed(unique = true)
    private String loanId;
    private int daysHeld;
    private double daysHeldVsPageCount;
    private boolean returnedEarly;
    private boolean renewedLoan;
    private String genre;
    private double completionLikelihood;
    @Indexed
    private LocalDateTime createdAt;
}
