package com.goandstudybackend.entity;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.Id;
import org.springframework.data.mongodb.core.index.Indexed;
import org.springframework.data.mongodb.core.mapping.Document;

import java.time.LocalDate;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Document(collection = "book_milestones")
public class BookMilestone {

    @Id
    private String id;
    
    @Indexed
    private String memberId;
    
    @Indexed
    private String bookId;
    
    private String bookTitle;
    
    private String author;
    
    private LocalDate targetDate;
    
    private LocalDate completedDate;
    
    private String status; // ON_TRACK, AT_RISK, OVERDUE, COMPLETED
    
    private int progressPercent;
    
    private String notes;
    
    @Builder.Default
    private boolean isActive = true;
    
    private LocalDateTime createdAt;
    private LocalDateTime updatedAt;
}
