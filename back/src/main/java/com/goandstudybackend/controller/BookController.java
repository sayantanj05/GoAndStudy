package com.goandstudybackend.controller;

import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.AuthorService;
import com.goandstudybackend.service.BookService;
import io.swagger.v3.oas.annotations.Operation;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/books")
@RequiredArgsConstructor
public class BookController {

    private final BookService bookService;
    private final AuthorService authorService;

    @Operation(summary = "Browse books")
    @GetMapping
    public ResponseEntity<ApiResponse<Map<String, Object>>> browseBooks(
            Authentication authentication,
            @RequestParam(required = false) String q,
            @RequestParam(required = false) String genre,
            @RequestParam(required = false) String language,
            @RequestParam(required = false) Boolean available,
            @RequestParam(required = false) String sortBy,
            @RequestParam(defaultValue = "1") int page,
            @RequestParam(defaultValue = "0") int size
    ) {
        return ResponseEntity.ok(ApiResponse.success("Books fetched", bookService.browseBooks(authentication.getName(), q, genre, language, available, sortBy, page, size)));
    }

    @Operation(summary = "Get book detail")
    @GetMapping("/{bookId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getBookDetail(Authentication authentication, @PathVariable String bookId) {
        try {
            System.out.println("Fetching book detail for bookId: " + bookId + " by member: " + authentication.getName());
            Map<String, Object> bookDetail = bookService.getBookDetail(authentication.getName(), bookId);
            return ResponseEntity.ok(ApiResponse.success("Book detail fetched", bookDetail));
        } catch (com.goandstudybackend.exception.ResourceNotFoundException e) {
            System.err.println("Book not found for ID: " + bookId + " - " + e.getMessage());
            throw e;
        } catch (NullPointerException e) {
            System.err.println("NullPointerException while fetching book detail for ID: " + bookId);
            e.printStackTrace();
            throw new com.goandstudybackend.exception.ResourceNotFoundException("Book details could not be loaded - invalid data");
        } catch (Exception e) {
            System.err.println("Error fetching book detail for ID: " + bookId);
            e.printStackTrace();
            throw new RuntimeException("Error loading book details: " + e.getMessage());
        }
    }

    @Operation(summary = "Get author detail")
    @GetMapping("/author/{authorId}")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAuthorDetail(@PathVariable String authorId) {
        return ResponseEntity.ok(ApiResponse.success("Author detail fetched", authorService.getAuthorDetail(authorId)));
    }

    @Operation(summary = "Vote a review as helpful")
    @PostMapping("/{bookId}/reviews/{reviewId}/helpful")
    public ResponseEntity<ApiResponse<Map<String, Object>>> voteReviewHelpful(
            Authentication authentication,
            @PathVariable String bookId,
            @PathVariable String reviewId
    ) {
        return ResponseEntity.ok(ApiResponse.success("Review vote recorded", bookService.voteReviewHelpful(authentication.getName(), bookId, reviewId)));
    }
}
