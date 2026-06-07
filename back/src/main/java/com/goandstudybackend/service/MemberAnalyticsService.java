package com.goandstudybackend.service;

import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.MemberAnalytics;
import com.goandstudybackend.repository.AiLogRepository;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.BookRepository;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberAnalyticsRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.SearchLogRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.time.Month;
import java.time.format.TextStyle;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberAnalyticsService {

    private final MemberRepository memberRepository;
    private final MemberAnalyticsRepository memberAnalyticsRepository;
    private final LoanRepository loanRepository;
    private final FineRecordRepository fineRecordRepository;
    private final SearchLogRepository searchLogRepository;
    private final AiLogRepository aiLogRepository;
    private final BookRepository bookRepository;
    private final BookCategoryRepository bookCategoryRepository;

    public MemberAnalytics recomputeForMember(String memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        List<Loan> recentLoans = loanRepository.findTop5ByMemberIdOrderByIssuedAtDesc(memberId);
        List<Loan> returnedLoans = new ArrayList<>();
        returnedLoans.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "RETURNED"));
        returnedLoans.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "Returned"));
        List<Loan> overdueLoans = loanRepository.findByMemberIdAndStatusIn(memberId, List.of("OVERDUE", "Overdue"));
        List<FineRecord> pendingFines = fineRecordRepository.findByMemberIdAndStatus(memberId, "Pending");
        List<FineRecord> paidFines = fineRecordRepository.findByMemberIdAndStatus(memberId, "Paid");

        double avgLoanDuration = returnedLoans.stream()
                .mapToLong(loan -> Math.max(0, java.time.temporal.ChronoUnit.DAYS.between(loan.getIssuedAt(), loan.getReturnedAt())))
                .average()
                .orElse(0.0);

        double totalFinesIncurred = pendingFines.stream().mapToDouble(FineRecord::getTotalAmount).sum()
                + paidFines.stream().mapToDouble(FineRecord::getTotalAmount).sum();
        int totalAiCalls = (int) aiLogRepository.countByMemberId(memberId);
        long actedOnCalls = aiLogRepository.findByMemberIdOrderByCreatedAtDesc(memberId).stream().filter(log -> log.isWasActedOn()).count();

        Map<String, GenreBucket> genreBuckets = new HashMap<>();
        Map<Month, Integer> monthActivity = new HashMap<>();
        
        // Include both returned and current active loans for complete picture
        List<Loan> currentLoans = loanRepository.findByMemberIdAndStatusIn(memberId, List.of("ISSUED", "RENEWED", "OVERDUE"));
        List<Loan> allLoansForGenre = new ArrayList<>();
        allLoansForGenre.addAll(returnedLoans);
        allLoansForGenre.addAll(currentLoans);
        
        for (Loan loan : allLoansForGenre) {
            Book book = bookRepository.findById(loan.getBookId()).orElse(null);
            String genre = book == null ? "" : getCategoryName(book.getCategoryIds());
            genreBuckets.computeIfAbsent(genre, ignored -> new GenreBucket()).increment();
            if (loan.getReturnedAt() != null) {
                monthActivity.merge(loan.getReturnedAt().getMonth(), 1, Integer::sum);
            }
        }

        String mostActiveMonth = monthActivity.entrySet().stream()
                .max(Map.Entry.comparingByValue())
                .map(entry -> entry.getKey().getDisplayName(TextStyle.SHORT, Locale.ENGLISH))
                .orElse("");

        MemberAnalytics analytics = memberAnalyticsRepository.findById(memberId)
                .orElse(MemberAnalytics.builder().id(memberId).build());
        analytics.setTotalLoans(member == null ? recentLoans.size() + currentLoans.size() : member.getTotalLoans());
        analytics.setTotalReturned(returnedLoans.size());
        analytics.setTotalOverdue(overdueLoans.size());
        analytics.setOverdueRate(analytics.getTotalLoans() == 0 ? 0.0 : (double) analytics.getTotalOverdue() / analytics.getTotalLoans());
        analytics.setTotalFinesIncurred(totalFinesIncurred);
        analytics.setTotalFinesPaid(member == null ? 0.0 : member.getTotalFinesPaid());
        analytics.setAvgLoanDuration(avgLoanDuration);
        analytics.setTotalSearches((int) searchLogRepository.countByMemberId(memberId));
        analytics.setTotalAiCalls(totalAiCalls);
        analytics.setAiConversionRate(totalAiCalls == 0 ? 0.0 : (double) actedOnCalls / totalAiCalls);
        analytics.setMostActiveMonth(mostActiveMonth);
        analytics.setGenreBreakdown(genreBuckets.entrySet().stream()
                .map(entry -> Map.<String, Object>of("genre", entry.getKey(), "count", entry.getValue().count))
                .toList());
        analytics.setComputedAt(LocalDateTime.now());
        return memberAnalyticsRepository.save(analytics);
    }

    public void recomputeAllMembers() {
        for (Member member : memberRepository.findAll()) {
            recomputeForMember(member.getId());
        }
    }

    private static class GenreBucket {
        private int count;

        void increment() {
            count++;
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
