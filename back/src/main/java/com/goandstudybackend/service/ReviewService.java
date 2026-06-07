package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.SubmitReviewRequest;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookReview;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import com.goandstudybackend.repository.LoanRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final BookReviewRepository bookReviewRepository;
    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final NvidiaLlmService nvidiaLlmService;
    private final ActivityLogService activityLogService;
    private final MemberPreferencesService memberPreferencesService;

    public Map<String, Object> submitReview(String memberId, SubmitReviewRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Loan does not belong to this member");
        }
        if (!"RETURNED".equalsIgnoreCase(loan.getStatus()) && !"Returned".equalsIgnoreCase(loan.getStatus())) {
            throw new IllegalArgumentException("Can only review returned books");
        }
        if (bookReviewRepository.existsByMemberIdAndBookId(memberId, request.getBookId())) {
            throw new DuplicateResourceException("Already reviewed");
        }

        double sentimentScore = 0.0;
        if (request.getReviewText() != null && !request.getReviewText().isBlank()) {
            try {
                String sentiment = nvidiaLlmService.generate(
                        "Rate the sentiment of this book review from -1.0 to 1.0. Reply with ONLY a decimal number. Review: "
                                + request.getReviewText()
                );
                if (sentiment != null && !sentiment.isBlank()) {
                    sentimentScore = Double.parseDouble(sentiment.trim());
                }
            } catch (Exception e) {
                // Fallback to neutral sentiment if AI service fails or returns invalid format
                sentimentScore = 0.0;
            }
        }

        BookReview review = bookReviewRepository.save(BookReview.builder()
                .memberId(memberId)
                .bookId(request.getBookId())
                .loanId(request.getLoanId())
                .notificationId(request.getNotificationId())
                .rating(request.getRating())
                .reviewText(request.getReviewText())
                .feedbackText(request.getFeedbackText())
                .sentimentScore(sentimentScore)
                .isVerified(true)
                .createdAt(LocalDateTime.now())
                .build());

        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));
        List<BookReview> allReviews = bookReviewRepository.findByBookId(request.getBookId());
        double averageRating = allReviews.stream().mapToInt(BookReview::getRating).average().orElse(0.0);
        book.setAverageRating(averageRating);
        book.setTotalRatings(allReviews.size());
        book.setUpdatedAt(LocalDateTime.now());
        bookRepository.save(book);

        activityLogService.log("REVIEW_SUBMITTED", memberId, "ROLE_MEMBER", "books", request.getBookId());
        memberPreferencesService.recomputePreferences(memberId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("reviewId", review.getId());
        response.put("notificationId", review.getNotificationId());
        response.put("rating", review.getRating());
        response.put("sentimentScore", review.getSentimentScore());
        response.put("isVerified", review.isVerified());
        return response;
    }

    public Map<String, Object> getMyReviews(String memberId) {
        List<BookReview> reviews = bookReviewRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        List<Map<String, Object>> items = new ArrayList<>();
        for (BookReview review : reviews) {
            Book book = bookRepository.findById(review.getBookId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("reviewId", review.getId());
            item.put("bookTitle", book == null ? "" : book.getTitle());
            item.put("bookCoverImageUrl", book == null ? "" : book.getCoverImageUrl());
            item.put("rating", review.getRating());
            item.put("reviewText", review.getReviewText());
            item.put("sentimentScore", review.getSentimentScore());
            item.put("createdAt", review.getCreatedAt());
            items.add(item);
        }
        return Map.of("reviews", items);
    }
}
