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
@Document(collection = "reading_goals")
public class ReadingGoal {

    @Id
    private String id;
    @Indexed(unique = true)
    private String memberId;
    private int targetBooks;
    private int year;
    private int booksReadSoFar;
    private double progressPercent;
    @Builder.Default
    private boolean isCompleted = false;
    private LocalDateTime completedAt;
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
