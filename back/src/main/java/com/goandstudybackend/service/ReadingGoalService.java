package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.SetReadingGoalRequest;
import com.goandstudybackend.entity.ReadingGoal;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.ReadingGoalRepository;
import com.goandstudybackend.util.DateUtil;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class ReadingGoalService {

    private final ReadingGoalRepository readingGoalRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    public Map<String, Object> getMyReadingGoal(String memberId) {
        ReadingGoal readingGoal = readingGoalRepository.findByMemberId(memberId).orElse(null);
        if (readingGoal == null) {
            return Map.of("hasGoal", false);
        }
        return refreshAndMap(readingGoal);
    }

    public Map<String, Object> setReadingGoal(String memberId, SetReadingGoalRequest request) {
        int year = request.getYear() == null ? LocalDate.now().getYear() : request.getYear();
        ReadingGoal readingGoal = readingGoalRepository.findByMemberId(memberId)
                .orElse(ReadingGoal.builder().memberId(memberId).createdAt(LocalDateTime.now()).build());

        int booksReadSoFar = countReturnedBooks(memberId, year);
        double progressPercent = request.getTargetBooks() == 0 ? 0.0 : (booksReadSoFar * 100.0) / request.getTargetBooks();

        readingGoal.setTargetBooks(request.getTargetBooks());
        readingGoal.setYear(year);
        readingGoal.setBooksReadSoFar(booksReadSoFar);
        readingGoal.setProgressPercent(progressPercent);
        readingGoal.setCompleted(booksReadSoFar >= request.getTargetBooks());
        readingGoal.setUpdatedAt(LocalDateTime.now());
        readingGoalRepository.save(readingGoal);

        memberRepository.findById(memberId).ifPresent(member -> {
            member.setReadingGoalBooks(request.getTargetBooks());
            member.setUpdatedAt(LocalDateTime.now());
            memberRepository.save(member);
        });

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("targetBooks", readingGoal.getTargetBooks());
        response.put("booksReadSoFar", readingGoal.getBooksReadSoFar());
        response.put("progressPercent", readingGoal.getProgressPercent());
        response.put("year", readingGoal.getYear());
        response.putAll(buildGoalPacing(readingGoal));
        return response;
    }

    public Map<String, Object> deleteReadingGoal(String memberId) {
        ReadingGoal readingGoal = readingGoalRepository.findByMemberId(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Reading goal not found"));
        readingGoalRepository.delete(readingGoal);
        memberRepository.findById(memberId).ifPresent(member -> {
            member.setReadingGoalBooks(0);
            member.setUpdatedAt(LocalDateTime.now());
            memberRepository.save(member);
        });
        return Map.of("message", "Reading goal removed");
    }

    @Async
    public void refreshProgress(String memberId) {
        readingGoalRepository.findByMemberId(memberId).ifPresent(this::refreshAndPersist);
    }

    private Map<String, Object> refreshAndMap(ReadingGoal readingGoal) {
        ReadingGoal updated = refreshAndPersist(readingGoal);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("hasGoal", true);
        response.put("targetBooks", updated.getTargetBooks());
        response.put("booksReadSoFar", updated.getBooksReadSoFar());
        response.put("progressPercent", updated.getProgressPercent());
        response.put("year", updated.getYear());
        response.put("isCompleted", updated.isCompleted());
        response.put("completedAt", updated.getCompletedAt());
        response.putAll(buildGoalPacing(updated));
        return response;
    }

    private ReadingGoal refreshAndPersist(ReadingGoal readingGoal) {
        int booksReadSoFar = countReturnedBooks(readingGoal.getMemberId(), readingGoal.getYear());
        boolean wasCompleted = readingGoal.isCompleted();
        readingGoal.setBooksReadSoFar(booksReadSoFar);
        readingGoal.setProgressPercent(readingGoal.getTargetBooks() == 0 ? 0.0 : (booksReadSoFar * 100.0) / readingGoal.getTargetBooks());
        readingGoal.setCompleted(booksReadSoFar >= readingGoal.getTargetBooks());
        if (!wasCompleted && readingGoal.isCompleted()) {
            readingGoal.setCompletedAt(LocalDateTime.now());
            notificationService.createNotification(
                    readingGoal.getMemberId(),
                    "GENERAL",
                    "Reading goal completed",
                    "Congratulations! You have completed your reading goal for " + readingGoal.getYear() + ".",
                    null,
                    null,
                    "app",
                    "Sent",
                    null,
                    true
            );
        }
        readingGoal.setUpdatedAt(LocalDateTime.now());
        return readingGoalRepository.save(readingGoal);
    }

    private int countReturnedBooks(String memberId, int year) {
        return (int) (
                loanRepository.countByMemberIdAndStatusAndReturnedAtBetween(memberId, "RETURNED", DateUtil.startOfYear(year), DateUtil.endOfYear(year))
                        + loanRepository.countByMemberIdAndStatusAndReturnedAtBetween(memberId, "Returned", DateUtil.startOfYear(year), DateUtil.endOfYear(year))
        );
    }

    private Map<String, Object> buildGoalPacing(ReadingGoal readingGoal) {
        LocalDate today = LocalDate.now();
        LocalDate yearEnd = LocalDate.of(readingGoal.getYear(), 12, 31);
        long daysRemaining = readingGoal.getYear() < today.getYear() ? 0 : Math.max(0, ChronoUnit.DAYS.between(today, yearEnd) + 1);
        int remainingBooks = Math.max(0, readingGoal.getTargetBooks() - readingGoal.getBooksReadSoFar());
        double booksPerWeekNeeded = daysRemaining == 0
                ? remainingBooks
                : remainingBooks / Math.max(1.0, daysRemaining / 7.0);
        Map<String, Object> pacing = new LinkedHashMap<>();
        pacing.put("remainingBooks", remainingBooks);
        pacing.put("daysRemaining", daysRemaining);
        pacing.put("booksPerWeekNeeded", booksPerWeekNeeded);
        pacing.put("paceStatus", readingGoal.isCompleted() ? "COMPLETED" : booksPerWeekNeeded <= 1 ? "ON_TRACK" : booksPerWeekNeeded <= 2 ? "FOCUSED" : "AT_RISK");
        return pacing;
    }
}
