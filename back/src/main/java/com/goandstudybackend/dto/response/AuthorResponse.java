package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class AuthorResponse {

    private String id;
    private String name;
    private String bio;
    private String nationality;
    private List<String> genres;
    private int totalBooks;
    private double averageRating;
    private LocalDateTime createdAt;
}

