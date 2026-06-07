package com.goandstudybackend.service;

import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.Notification;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.NotificationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
public class LoanNotificationScheduler {

    private final LoanRepository loanRepository;
    private final NotificationRepository notificationRepository;
    private final NotificationService notificationService;
    private final MemberRepository memberRepository;
    private final FineRecordRepository fineRecordRepository;
    private final SystemConfigService systemConfigService;

    /**
     * Check for loans due in 2 days and send reminder notifications
     * Runs every day at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendDueDateReminders() {
        LocalDateTime now = LocalDateTime.now();
        LocalDateTime twoDaysFromNow = now.plusDays(2);
        LocalDateTime startOfDay = twoDaysFromNow.toLocalDate().atStartOfDay();
        LocalDateTime endOfDay = twoDaysFromNow.toLocalDate().atTime(23, 59, 59);

        // Find loans due in exactly 2 days with active status
        List<Loan> loansDueSoon = loanRepository.findAll().stream()
                .filter(loan -> List.of("ISSUED", "RENEWED", "Issued", "Renewed").contains(loan.getStatus()))
                .filter(loan -> loan.getDueDate() != null)
                .filter(loan -> !loan.getDueDate().isBefore(startOfDay) && !loan.getDueDate().isAfter(endOfDay))
                .toList();

        DateTimeFormatter formatter = DateTimeFormatter.ofPattern("MMM dd, yyyy");

        for (Loan loan : loansDueSoon) {
            // Check if reminder was already sent today
            String notificationKey = "DUE_REMINDER_" + loan.getId() + "_" + LocalDate.now();
            boolean alreadySent = notificationRepository.existsByMemberIdAndTypeAndTitleContaining(
                    loan.getMemberId(),
                    "REMINDER",
                    "Due in 2 days"
            );

            if (!alreadySent) {
                String dueDateStr = loan.getDueDate().format(formatter);
                notificationService.createNotification(
                        loan.getMemberId(),
                        "REMINDER",
                        "Book Due in 2 Days",
                        "Reminder: Your borrowed book \"" + loan.getBookTitle() + "\" is due on " + dueDateStr + ". Please return it on time to avoid late fees.",
                        loan.getId(),
                        loan.getBookId(),
                        "app",
                        "Sent",
                        null,
                        true
                );
            }
        }

        System.out.println("Due date reminders sent for " + loansDueSoon.size() + " loans");
    }

    /**
     * Check for overdue loans and send daily overdue notifications
     * Runs every day at 9 AM
     */
    @Scheduled(cron = "0 0 9 * * ?")
    public void sendOverdueNotifications() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = LocalDate.now();

            // Find all overdue loans that haven't been returned
            List<Loan> overdueLoans = loanRepository.findAll().stream()
                    .filter(loan -> List.of("ISSUED", "RENEWED", "OVERDUE", "Issued", "Renewed", "Overdue", "Active").contains(loan.getStatus()))
                    .filter(loan -> loan.getReturnedAt() == null)  // Only unreturned books
                    .filter(loan -> loan.getDueDate() != null && loan.getDueDate().isBefore(now))
                    .toList();

            System.out.println("Sending overdue notifications for " + overdueLoans.size() + " loans");

            for (Loan loan : overdueLoans) {
                try {
                    // Get member to determine fine rate
                    Member member = memberRepository.findById(loan.getMemberId()).orElse(null);
                    double finePerDay = member != null && member.getFineRatePerDay() > 0 
                        ? member.getFineRatePerDay() 
                        : 10.0;

                    // Apply grace period if configured
                    int gracePeriodDays = 0;
                    try {
                        gracePeriodDays = systemConfigService.getById("loan_settings").getGracePeriodDays();
                    } catch (Exception e) {
                        gracePeriodDays = 0;
                    }

                    LocalDateTime fineStartDate = loan.getDueDate().plusDays(Math.max(0, gracePeriodDays));
                    long overdueDays = ChronoUnit.DAYS.between(fineStartDate, now);
                    double totalFine = Math.max(0, overdueDays * finePerDay);

                    // Check if notification was already sent today for this loan
                    boolean alreadySentToday = notificationRepository.findByMemberIdAndRelatedLoanIdAndTypeAndCreatedAtAfter(
                                    loan.getMemberId(),
                                    loan.getId(),
                                    "OVERDUE",
                                    today.atStartOfDay()
                            ).size() > 0;

                    if (!alreadySentToday && overdueDays > 0) {
                        notificationService.createNotification(
                                loan.getMemberId(),
                                "OVERDUE",
                                "Overdue Book - Fine Increasing",
                                "Your book \"" + loan.getBookTitle() + "\" is overdue by " + overdueDays + " day(s). " +
                                        "Current fine amount: ₹" + String.format("%.2f", totalFine) + ". " +
                                        "Please return immediately to avoid further penalties. Fine increases by ₹" + String.format("%.2f", finePerDay) + " daily.",
                                loan.getId(),
                                loan.getBookId(),
                                "app",
                                "Sent",
                                null,
                                true
                        );
                    }

                    // Update loan status to OVERDUE if not already
                    if (!loan.getStatus().equalsIgnoreCase("OVERDUE")) {
                        loan.setStatus("OVERDUE");
                        loan.setIsOverdue(true);
                    }
                    
                    // Always update overdue days and fine amount
                    loan.setOverdueDays((int) Math.max(0, overdueDays));
                    loan.setFineAmount(totalFine);
                    loan.setUpdatedAt(now);
                    loanRepository.save(loan);
                } catch (Exception e) {
                    System.err.println("Error processing overdue notification for loan " + loan.getId() + ": " + e.getMessage());
                }
            }

            System.out.println("Completed overdue notifications for " + overdueLoans.size() + " loans");
        } catch (Exception e) {
            System.err.println("ERROR in sendOverdueNotifications scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }

    /**
     * Update overdue status and fines daily (runs at 6 AM every day)
     * Ensures all overdue books have their status updated and fines calculated
     */
    @Scheduled(cron = "0 0 6 * * ?")
    public void updateOverdueStatus() {
        try {
            LocalDateTime now = LocalDateTime.now();
            LocalDate today = LocalDate.now();
            
            System.out.println("=== Starting scheduled overdue status update at " + now + " ===");

            // Get all loans with active statuses (case-insensitive)
            List<Loan> allActiveLoans = loanRepository.findAll().stream()
                    .filter(loan -> List.of("ISSUED", "RENEWED", "OVERDUE", "Issued", "Renewed", "Overdue", "Active").contains(loan.getStatus()))
                    .filter(loan -> loan.getReturnedAt() == null)  // Only unreturned books
                    .filter(loan -> loan.getDueDate() != null)
                    .toList();

            System.out.println("Total active loans to check: " + allActiveLoans.size());

            int updatedCount = 0;
            int fineRecordsUpdated = 0;

            for (Loan loan : allActiveLoans) {
                if (loan.getDueDate().isBefore(now)) {
                    // Book is overdue
                    
                    // Get member to determine fine rate
                    Member member = memberRepository.findById(loan.getMemberId()).orElse(null);
                    double finePerDay = member != null && member.getFineRatePerDay() > 0 
                        ? member.getFineRatePerDay() 
                        : 10.0;

                    // Apply grace period if configured
                    int gracePeriodDays = 0;
                    try {
                        gracePeriodDays = systemConfigService.getById("loan_settings").getGracePeriodDays();
                    } catch (Exception e) {
                        gracePeriodDays = 0;
                    }

                    LocalDateTime fineStartDate = loan.getDueDate().plusDays(Math.max(0, gracePeriodDays));
                    long overdueDays = ChronoUnit.DAYS.between(fineStartDate, now);
                    double totalFine = Math.max(0, overdueDays * finePerDay);

                    // Update loan status if needed
                    if (!loan.getStatus().equalsIgnoreCase("OVERDUE")) {
                        loan.setStatus("OVERDUE");
                        loan.setIsOverdue(true);
                        updatedCount++;
                    }
                    
                    // Always update overdue days and fine amount
                    loan.setOverdueDays((int) Math.max(0, overdueDays));
                    loan.setFineAmount(totalFine);
                    loan.setUpdatedAt(now);
                    loanRepository.save(loan);

                    // Create or update FineRecord
                    Optional<FineRecord> existingFineRecord = fineRecordRepository.findByLoanId(loan.getId());
                    if (existingFineRecord.isPresent()) {
                        // Update existing fine record
                        FineRecord fineRecord = existingFineRecord.get();
                        if ("Pending".equalsIgnoreCase(fineRecord.getStatus())) {
                            fineRecord.setOverdueDays((int) Math.max(0, overdueDays));
                            fineRecord.setTotalAmount(totalFine);
                            fineRecord.setRatePerDay(finePerDay);
                            fineRecordRepository.save(fineRecord);
                            fineRecordsUpdated++;
                        }
                    } else {
                        // Create new fine record
                        FineRecord newFineRecord = FineRecord.builder()
                            .id("FINE" + System.currentTimeMillis() + Math.abs(loan.getId().hashCode()))
                            .loanId(loan.getId())
                            .memberId(loan.getMemberId())
                            .bookId(loan.getBookId())
                            .bookTitle(loan.getBookTitle())
                            .overdueDays((int) Math.max(0, overdueDays))
                            .ratePerDay(finePerDay)
                            .totalAmount(totalFine)
                            .status("Pending")
                            .createdAt(now)
                            .build();
                        fineRecordRepository.save(newFineRecord);
                        fineRecordsUpdated++;
                    }
                }
            }

            System.out.println("Updated " + updatedCount + " loan statuses to OVERDUE");
            System.out.println("Updated/Created " + fineRecordsUpdated + " fine records");
            System.out.println("=== Completed scheduled overdue status update ===");
        } catch (Exception e) {
            System.err.println("ERROR in updateOverdueStatus scheduler: " + e.getMessage());
            e.printStackTrace();
        }
    }
}
