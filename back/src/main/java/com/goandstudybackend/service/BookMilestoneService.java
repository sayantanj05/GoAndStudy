package com.goandstudybackend.service;

import com.goandstudybackend.entity.Book;
import com.goandstudybackend.entity.BookMilestone;
import com.goandstudybackend.entity.GoalPeriod;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookMilestoneRepository;
import com.goandstudybackend.repository.BookRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class BookMilestoneService {

    private final BookMilestoneRepository milestoneRepository;
    private final BookRepository bookRepository;
    private final NotificationService notificationService;

    public Map<String, Object> getMemberMilestones(String memberId) {
        List<BookMilestone> milestones = milestoneRepository.findByMemberIdAndIsActiveTrue(memberId);
        
        // Update status for each milestone
        milestones.forEach(this::updateMilestoneStatus);
        
        List<Map<String, Object>> milestoneList = milestones.stream()
                .map(this::mapMilestoneToResponse)
                .collect(Collectors.toList());
        
        return Map.of("milestones", milestoneList);
    }

    public Map<String, Object> createMilestone(String memberId, String bookId, LocalDate targetDate, String notes) {
        Book book = bookRepository.findById(bookId)
                .orElseThrow(() -> new ResourceNotFoundException("Book not found"));

        BookMilestone milestone = BookMilestone.builder()
                .memberId(memberId)
                .bookId(bookId)
                .bookTitle(book.getTitle())
                .author(getAuthorName(book))
                .targetDate(targetDate)
                .status("ON_TRACK")
                .progressPercent(0)
                .notes(notes)
                .isActive(true)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();

        BookMilestone saved = milestoneRepository.save(milestone);
        return mapMilestoneToResponse(saved);
    }

    public Map<String, Object> updateMilestone(String memberId, String milestoneId, LocalDate targetDate, String notes) {
        BookMilestone milestone = milestoneRepository.findByMemberIdAndId(memberId, milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        if (targetDate != null) {
            milestone.setTargetDate(targetDate);
        }
        if (notes != null) {
            milestone.setNotes(notes);
        }
        
        updateMilestoneStatus(milestone);
        milestone.setUpdatedAt(LocalDateTime.now());
        
        BookMilestone saved = milestoneRepository.save(milestone);
        return mapMilestoneToResponse(saved);
    }

    public void deleteMilestone(String memberId, String milestoneId) {
        milestoneRepository.deleteByMemberIdAndId(memberId, milestoneId);
    }

    public Map<String, Object> markMilestoneCompleted(String memberId, String milestoneId) {
        BookMilestone milestone = milestoneRepository.findByMemberIdAndId(memberId, milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        milestone.setStatus("COMPLETED");
        milestone.setCompletedDate(LocalDate.now());
        milestone.setProgressPercent(100);
        milestone.setUpdatedAt(LocalDateTime.now());
        
        BookMilestone saved = milestoneRepository.save(milestone);
        
        notificationService.createNotification(
                memberId,
                "MILESTONE_COMPLETED",
                "Milestone Achieved!",
                "Congratulations! You completed '" + milestone.getBookTitle() + "' on time!",
                null,
                milestone.getBookId(),
                "IN_APP",
                "Sent",
                null,
                true
        );
        
        return mapMilestoneToResponse(saved);
    }

    public Map<String, Object> updateProgress(String memberId, String milestoneId, int progressPercent) {
        BookMilestone milestone = milestoneRepository.findByMemberIdAndId(memberId, milestoneId)
                .orElseThrow(() -> new ResourceNotFoundException("Milestone not found"));

        milestone.setProgressPercent(Math.min(100, Math.max(0, progressPercent)));
        updateMilestoneStatus(milestone);
        milestone.setUpdatedAt(LocalDateTime.now());
        
        BookMilestone saved = milestoneRepository.save(milestone);
        return mapMilestoneToResponse(saved);
    }

    private void updateMilestoneStatus(BookMilestone milestone) {
        if ("COMPLETED".equals(milestone.getStatus())) {
            return;
        }

        LocalDate today = LocalDate.now();
        LocalDate targetDate = milestone.getTargetDate();
        
        if (targetDate == null) {
            milestone.setStatus("ON_TRACK");
            return;
        }

        if (today.isAfter(targetDate)) {
            milestone.setStatus("OVERDUE");
        } else {
            long daysUntilTarget = ChronoUnit.DAYS.between(today, targetDate);
            
            if (daysUntilTarget <= 3) {
                milestone.setStatus("AT_RISK");
                // Send at-risk notification
                if (daysUntilTarget == 3 && milestone.getProgressPercent() < 70) {
                    notificationService.createNotification(
                            milestone.getMemberId(),
                            "MILESTONE_AT_RISK",
                            "Milestone at Risk",
                            "You have 3 days left to complete '" + milestone.getBookTitle() + "'. You're at " + milestone.getProgressPercent() + "% progress.",
                            null,
                            milestone.getBookId(),
                            "IN_APP",
                            "Sent",
                            null,
                            true
                    );
                }
            } else {
                milestone.setStatus("ON_TRACK");
            }
        }
    }

    @Scheduled(cron = "0 0 9 * * ?") // Run daily at 9 AM
    public void checkOverdueMilestones() {
        log.info("Checking for overdue milestones...");
        
        List<BookMilestone> allMilestones = milestoneRepository.findAll();
        
        for (BookMilestone milestone : allMilestones) {
            if (!"COMPLETED".equals(milestone.getStatus()) && milestone.isActive()) {
                String oldStatus = milestone.getStatus();
                updateMilestoneStatus(milestone);
                
                // If status changed to OVERDUE, send notification
                if ("OVERDUE".equals(milestone.getStatus()) && !"OVERDUE".equals(oldStatus)) {
                    notificationService.createNotification(
                            milestone.getMemberId(),
                            "MILESTONE_OVERDUE",
                            "Milestone Overdue",
                            "Your deadline for '" + milestone.getBookTitle() + "' has passed. Don't give up!",
                            null,
                            milestone.getBookId(),
                            "IN_APP",
                            "Sent",
                            null,
                            true
                    );
                }
                
                milestoneRepository.save(milestone);
            }
        }
    }

    private Map<String, Object> mapMilestoneToResponse(BookMilestone milestone) {
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("id", milestone.getId());
        response.put("bookId", milestone.getBookId());
        response.put("bookTitle", milestone.getBookTitle());
        response.put("author", milestone.getAuthor());
        response.put("targetDate", milestone.getTargetDate());
        response.put("completedDate", milestone.getCompletedDate());
        response.put("status", milestone.getStatus());
        response.put("progressPercent", milestone.getProgressPercent());
        response.put("notes", milestone.getNotes());
        response.put("daysRemaining", milestone.getTargetDate() != null ? 
                ChronoUnit.DAYS.between(LocalDate.now(), milestone.getTargetDate()) : null);
        response.put("createdAt", milestone.getCreatedAt());
        return response;
    }

    private String getAuthorName(Book book) {
        if (book.getAuthorIds() == null || book.getAuthorIds().isEmpty()) {
            return "Unknown Author";
        }
        return String.join(", ", book.getAuthorIds());
    }
}
