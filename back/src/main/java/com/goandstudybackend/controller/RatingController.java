package com.goandstudybackend.controller;

import com.goandstudybackend.dto.request.RatingRequest;
import com.goandstudybackend.service.RatingService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

@RestController
@RequiredArgsConstructor
@RequestMapping("/api/v1/member")
public class RatingController {

    private final RatingService ratingService;

    @PostMapping("/books/{bookId}/ratings")
    public ResponseEntity<Object> addRating(Authentication auth, @PathVariable String bookId, @RequestBody RatingRequest request) {
        String memberId = auth.getName();
        return ResponseEntity.ok(ratingService.addRating(memberId, bookId, request));
    }
}
