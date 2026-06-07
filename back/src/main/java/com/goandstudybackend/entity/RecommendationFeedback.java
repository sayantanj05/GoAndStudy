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
@Document(collection = "recommendation_feedback")
public class RecommendationFeedback {

    @Id
    private String id;
    @Indexed
    private String aiLogId;
    @Indexed
    private String memberId;
    private String recommendedBookId;
    private String actionTaken;
    private LocalDateTime actionAt;
    private String resultingLoanId;
    private LocalDateTime createdAt;
}
