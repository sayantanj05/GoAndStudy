package com.goandstudybackend.jobs;

import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.GenreAnalytics;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Wishlist;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.GenreAnalyticsRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
@RequiredArgsConstructor
public class GenreAnalyticsJob {

    private final BookCategoryRepository bookCategoryRepository;
    private final BookRepository bookRepository;
    private final LoanRepository loanRepository;
    private final WishlistRepository wishlistRepository;
    private final GenreAnalyticsRepository genreAnalyticsRepository;

    @Scheduled(cron = "0 0 2 * * *")
    public void recomputeGenreAnalytics() {
        List<Book> books = bookRepository.findAll();
        List<Loan> loans = loanRepository.findAll();
        List<Wishlist> wishlists = wishlistRepository.findAll();
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime last7 = now.minusDays(7);
        LocalDateTime last30 = now.minusDays(30);

        for (BookCategory category : bookCategoryRepository.findAll()) {
            List<Book> categoryBooks = books.stream()
                    .filter(book -> !book.isDeleted() && book.getCategoryIds() != null && book.getCategoryIds().contains(category.getId()))
                    .toList();
            List<String> categoryBookIds = categoryBooks.stream().map(Book::getId).toList();
            List<Loan> categoryLoans = loans.stream().filter(loan -> categoryBookIds.contains(loan.getBookId())).toList();

            int loansLast7Days = (int) categoryLoans.stream().filter(loan -> loan.getIssuedAt() != null && !loan.getIssuedAt().isBefore(last7)).count();
            int loansLast30Days = (int) categoryLoans.stream().filter(loan -> loan.getIssuedAt() != null && !loan.getIssuedAt().isBefore(last30)).count();
            int totalWishlists = (int) wishlists.stream().filter(wishlist -> category.getId().equals(wishlist.getBookGenre())).count();
            double avgRating = categoryBooks.stream().mapToDouble(Book::getAverageRating).average().orElse(0.0);
            double wishlistToLoanRate = categoryLoans.isEmpty() ? 0.0 : (double) totalWishlists / categoryLoans.size();
            double trendScore = loansLast30Days == 0 ? 0.0 : ((double) loansLast7Days / loansLast30Days) * 4;

            GenreAnalytics analytics = genreAnalyticsRepository.findById(category.getSlug())
                    .orElse(GenreAnalytics.builder().id(category.getSlug()).build());
            analytics.setGenreName(category.getName());
            analytics.setTotalBooks(categoryBooks.size());
            analytics.setTotalLoans(categoryLoans.size());
            analytics.setLoansLast7Days(loansLast7Days);
            analytics.setLoansLast30Days(loansLast30Days);
            analytics.setAvgRating(avgRating);
            analytics.setTotalWishlists(totalWishlists);
            analytics.setWishlistToLoanRate(wishlistToLoanRate);
            analytics.setPopularBooks(categoryBooks.stream()
                    .sorted((left, right) -> Integer.compare(right.getTotalIssues(), left.getTotalIssues()))
                    .limit(5)
                    .map(Book::getTitle)
                    .collect(Collectors.toList()));
            analytics.setTrendScore(trendScore);
            analytics.setComputedAt(now);
            genreAnalyticsRepository.save(analytics);
        }
        log.info("Genre analytics recomputed");
    }
}
