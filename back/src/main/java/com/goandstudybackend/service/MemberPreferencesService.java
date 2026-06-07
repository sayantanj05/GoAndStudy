package com.goandstudybackend.service;

import com.goandstudybackend.entity.Author;
import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.BookReview;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.MemberPreferences;
import com.goandstudybackend.entity.ReadingSession;
import com.goandstudybackend.entity.Wishlist;
import com.goandstudybackend.repository.AuthorRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberPreferencesRepository;
import com.goandstudybackend.repository.ReadingSessionRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class MemberPreferencesService {

    private final LoanRepository loanRepository;
    private final BookRepository bookRepository;
    private final AuthorRepository authorRepository;
    private final BookCategoryRepository bookCategoryRepository;
    private final WishlistRepository wishlistRepository;
    private final BookReviewRepository bookReviewRepository;
    private final ReadingSessionRepository readingSessionRepository;
    private final MemberPreferencesRepository memberPreferencesRepository;
    private final NvidiaEmbeddingService nvidiaEmbeddingService;

    @Async
    public void recomputePreferences(String memberId) {
        List<Loan> returnedLoans = new ArrayList<>();
        returnedLoans.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "RETURNED"));
        returnedLoans.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "Returned"));
        List<BookReview> reviews = bookReviewRepository.findByMemberIdOrderByCreatedAtDesc(memberId);
        List<Wishlist> wishlists = wishlistRepository.findByMemberIdOrderByAddedAtDesc(memberId);
        List<ReadingSession> readingSessions = readingSessionRepository.findByMemberId(memberId);

        Map<String, GenreAccumulator> genreStats = new HashMap<>();
        Map<String, AuthorAccumulator> authorStats = new HashMap<>();
        List<String> recentDescriptions = new ArrayList<>();
        LinkedHashSet<String> distinctGenres = new LinkedHashSet<>();
        double totalPages = 0.0;
        int pageSamples = 0;

        for (Loan loan : returnedLoans) {
            Book book = bookRepository.findById(loan.getBookId()).orElse(null);
            if (book == null) {
                continue;
            }
            String genre = getCategoryName(book.getCategoryIds());
            distinctGenres.add(genre);
            BookReview matchingReview = reviews.stream().filter(review -> loan.getId().equals(review.getLoanId())).findFirst().orElse(null);
            double rating = matchingReview == null ? 0.0 : matchingReview.getRating();

            genreStats.computeIfAbsent(genre, ignored -> new GenreAccumulator()).add(rating);
            for (String authorId : book.getAuthorIds()) {
                Author author = authorRepository.findById(authorId).orElse(null);
                authorStats.computeIfAbsent(authorId, ignored -> new AuthorAccumulator(author == null ? "" : author.getName())).add(rating);
            }

            if (book.getDescription() != null && !book.getDescription().isBlank() && recentDescriptions.size() < 5) {
                recentDescriptions.add(book.getDescription());
            }
            if (book.getPageCount() > 0) {
                totalPages += book.getPageCount();
                pageSamples++;
            }
        }

        double avgRatingGiven = reviews.stream().mapToInt(BookReview::getRating).average().orElse(0.0);
        double readingSpeedProxy = readingSessions.stream().mapToDouble(ReadingSession::getDaysHeldVsPageCount).average().orElse(0.0);
        List<String> recentKeywords = extractKeywords(recentDescriptions);
        List<String> wishlistGenres = wishlists.stream()
                .map(Wishlist::getBookGenre)
                .filter(value -> value != null && !value.isBlank())
                .distinct()
                .toList();
        double diversityScore = returnedLoans.isEmpty() ? 0.0 : (double) distinctGenres.size() / returnedLoans.size();

        List<Map<String, Object>> topGenres = genreStats.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "genre", entry.getKey(),
                        "score", entry.getValue().average(),
                        "loanCount", entry.getValue().count,
                        "avgRating", entry.getValue().average()
                ))
                .sorted((left, right) -> Double.compare((double) right.get("score"), (double) left.get("score")))
                .toList();

        List<Map<String, Object>> topAuthors = authorStats.entrySet().stream()
                .map(entry -> Map.<String, Object>of(
                        "authorId", entry.getKey(),
                        "name", entry.getValue().name,
                        "loanCount", entry.getValue().count,
                        "avgRating", entry.getValue().average()
                ))
                .sorted((left, right) -> Double.compare((double) right.get("avgRating"), (double) left.get("avgRating")))
                .toList();

        String preferredPageLength = pageSamples == 0 ? "" : totalPages / pageSamples < 200 ? "SHORT" : totalPages / pageSamples < 400 ? "MEDIUM" : "LONG";
        List<Double> genreEmbedding = nvidiaEmbeddingService.generateEmbedding(
                topGenres.stream().map(entry -> String.valueOf(entry.get("genre"))).reduce("", (left, right) -> left + " " + right).trim()
        );

        MemberPreferences preferences = memberPreferencesRepository.findById(memberId)
                .orElse(MemberPreferences.builder().id(memberId).build());
        preferences.setTopGenres(new ArrayList<>(topGenres));
        preferences.setTopAuthors(new ArrayList<>(topAuthors));
        preferences.setAvgRatingGiven(avgRatingGiven);
        preferences.setReadingSpeedProxy(readingSpeedProxy);
        preferences.setPreferredPageLength(preferredPageLength);
        preferences.setGenreEmbedding(new ArrayList<>(genreEmbedding));
        preferences.setRecentKeywords(new ArrayList<>(recentKeywords));
        preferences.setWishlistGenres(new ArrayList<>(wishlistGenres));
        preferences.setDiversityScore(diversityScore);
        preferences.setLastComputedAt(LocalDateTime.now());
        memberPreferencesRepository.save(preferences);
    }

    private List<String> extractKeywords(List<String> descriptions) {
        Map<String, Integer> wordFrequency = new HashMap<>();
        Pattern splitter = Pattern.compile("[^A-Za-z]+");
        for (String description : descriptions) {
            for (String token : splitter.split(description.toLowerCase())) {
                if (token.length() <= 3) {
                    continue;
                }
                wordFrequency.put(token, wordFrequency.getOrDefault(token, 0) + 1);
            }
        }
        return wordFrequency.entrySet().stream()
                .sorted(Map.Entry.<String, Integer>comparingByValue(Comparator.reverseOrder()))
                .limit(10)
                .map(Map.Entry::getKey)
                .toList();
    }

    private static class GenreAccumulator {
        private int count;
        private double totalRating;

        void add(double rating) {
            count++;
            totalRating += rating;
        }

        double average() {
            return count == 0 ? 0.0 : totalRating / count;
        }
    }

    private static class AuthorAccumulator {
        private final String name;
        private int count;
        private double totalRating;

        private AuthorAccumulator(String name) {
            this.name = name;
        }

        void add(double rating) {
            count++;
            totalRating += rating;
        }

        double average() {
            return count == 0 ? 0.0 : totalRating / count;
        }
    }

    private String getCategoryName(List<String> categoryIds) {
        if (categoryIds == null || categoryIds.isEmpty()) {
            return "Uncategorized";
        }
        try {
            BookCategory category = bookCategoryRepository.findById(categoryIds.getFirst()).orElse(null);
            return category != null ? category.getName() : "Uncategorized";
        } catch (Exception e) {
            return "Uncategorized";
        }
    }
}
