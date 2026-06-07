package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.RatingRequest;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookReview;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.aggregation.Aggregation;
import org.springframework.data.mongodb.core.aggregation.AggregationResults;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.stereotype.Service;
import org.bson.Document;
import java.time.LocalDateTime;
import java.util.Map;

@Service
@RequiredArgsConstructor
/* @Transactional - not needed */
public class RatingService {

    private final BookReviewRepository bookReviewRepository;
    private final BookRepository bookRepository;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> addRating(String memberId, String bookId, RatingRequest request) {
        Book book = bookRepository.findById(bookId).orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        BookReview review = BookReview.builder()
            .memberId(memberId)
            .bookId(bookId)
            .loanId(request.getLoanId())
            .rating(request.getRating())
            .reviewText(request.getReviewText())
            .createdAt(LocalDateTime.now())
            .createdBy(memberId)
            .build();
        bookReviewRepository.save(review);

        // Sync book metrics
        syncBookMetrics(bookId);

        return Map.of("reviewId", review.getId(), "averageRating", getAverageRating(bookId), "totalRatings", getTotalRatings(bookId));
    }

    private void syncBookMetrics(String bookId) {
        double avg = getAverageRating(bookId);
        int total = getTotalRatings(bookId);
        Book book = bookRepository.findById(bookId).orElseThrow();
        book.setAverageRating(avg);
        book.setTotalRatings(total);
        bookRepository.save(book);
    }

    public double getAverageRating(String bookId) {
        Aggregation avgAgg = Aggregation.newAggregation(
            Aggregation.match(Criteria.where("bookId").is(bookId)),
            Aggregation.group().avg("rating").as("averageRating")
        );
        AggregationResults<Document> results = mongoTemplate.aggregate(avgAgg, BookReview.class, Document.class);
        return results.getMappedResults().stream()
            .mapToDouble(doc -> doc.containsKey("averageRating") ? doc.getDouble("averageRating") : 0.0)
            .findFirst().orElse(0.0);
    }

    public int getTotalRatings(String bookId) {
        return (int) bookReviewRepository.countByBookId(bookId);
    }
}
