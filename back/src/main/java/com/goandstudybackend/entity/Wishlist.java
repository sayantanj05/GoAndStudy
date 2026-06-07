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
@Document(collection = "wishlists")
public class Wishlist {

    @Id
    private String id;
    @Indexed
    private String memberId;
    @Indexed
    private String bookId;
    private String bookTitle;
    private String bookGenre;
    @Indexed
    private LocalDateTime addedAt;
    private String addedFrom;
    @Builder.Default
    private boolean notifyOnAvailable = false;
    private LocalDateTime updatedAt;
    private String createdBy;
    private String updatedBy;
}
