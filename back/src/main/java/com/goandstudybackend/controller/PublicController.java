package com.goandstudybackend.controller;

import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDateTime;
import java.util.Map;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class PublicController {

    private final BookService bookService;

    @Operation(summary = "Health check")
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        return ResponseEntity.ok(ApiResponse.success("Service is healthy", Map.of("status", "UP", "timestamp", LocalDateTime.now())));
    }

    @Operation(summary = "Get featured books")
    @GetMapping("/books/featured")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getFeaturedBooks() {
        return ResponseEntity.ok(ApiResponse.success("Featured books fetched", bookService.getFeaturedBooks()));
    }

    @Operation(summary = "Get book categories")
    @GetMapping("/books/categories")
    public ResponseEntity<ApiResponse<Object>> getBookCategories() {
        return ResponseEntity.ok(ApiResponse.success("Book categories fetched", bookService.getBookCategories()));
    }
}
