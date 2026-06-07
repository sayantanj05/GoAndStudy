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
@Document(collection = "book_requests")
public class BookRequest {

    @Id
    private String id;
    @Indexed
    private String memberId;
    private String memberName;
    @Indexed
    private String title;
    private String author;
    private String isbn;
    private String reason;
    @Builder.Default
    private String status = "Pending";
    private String adminNote;
    @Indexed
    private String reviewedByAdminId;
    private LocalDateTime reviewedAt;
    @Indexed
    private LocalDateTime createdAt;
}
