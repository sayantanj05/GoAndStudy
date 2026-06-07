package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.SendNotificationRequest;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.Notification;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.ActivityLogRepository;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.NotificationRepository;
import com.goandstudybackend.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.data.domain.Sort;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Service
@RequiredArgsConstructor
public class StaffService {
    private static final List<String> ACTIVE_LOAN_STATUSES = List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Overdue", "Issued", "Renewed");

    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final FineRecordRepository fineRecordRepository;
    private final NotificationRepository notificationRepository;
    private final ActivityLogRepository activityLogRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> changeOwnPassword(String staffId, ChangePasswordRequest request) {
        var staff = staffRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), staff.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        staff.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
        activityLogService.log("PASSWORD_CHANGED", staffId, "ROLE_STAFF", "staff", staffId);
        return Map.of("message", "Password updated successfully");
    }

    public Map<String, Object> lookupMember(String q, String staffId) {
        Member member = memberRepository.findById(q).orElse(memberRepository.findByEmailIgnoreCase(q).orElseThrow(() -> new ResourceNotFoundException("Member not found")));
        activityLogService.log("MEMBER_VIEWED", staffId, "ROLE_STAFF", "members", member.getId());
        int activeLoanCount = loanRepository.findByMemberIdAndStatusIn(member.getId(), ACTIVE_LOAN_STATUSES).size();
        return Map.of(
                "memberId", member.getId(),
                "name", member.getName(),
                "email", member.getEmail(),
                "phone", member.getPhone(),
                "activeLoanCount", activeLoanCount,
                "maxActiveLoans", member.getMaxActiveLoans(),
                "canBorrow", activeLoanCount < member.getMaxActiveLoans()
        );
    }

    public Map<String, Object> getMemberProfile(String memberId, String staffId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        List<Loan> activeLoans = loanRepository.findByMemberIdAndStatusIn(memberId, ACTIVE_LOAN_STATUSES);
        List<Loan> recentReturns = new ArrayList<>();
        recentReturns.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "RETURNED"));
        recentReturns.addAll(loanRepository.findByMemberIdAndStatusOrderByReturnedAtDesc(memberId, "Returned"));
        recentReturns = recentReturns.stream().limit(5).toList();
        activityLogService.log("MEMBER_VIEWED", staffId, "ROLE_STAFF", "members", memberId);
        return Map.of(
                "member", Map.of(
                        "memberId", member.getId(),
                        "name", member.getName(),
                        "email", member.getEmail(),
                        "phone", member.getPhone(),
                        "isActive", member.isActive(),
                        "activeLoanCount", activeLoans.size(),
                        "totalLoans", member.getTotalLoans()
                ),
                "activeLoans", activeLoans,
                "recentReturns", recentReturns
        );
    }

    public Map<String, Object> getMembers(String q) {
        Query query = new Query();
        if (q != null && !q.isBlank()) {
            Pattern pattern = Pattern.compile(Pattern.quote(q), Pattern.CASE_INSENSITIVE);
            query.addCriteria(new Criteria().orOperator(
                    Criteria.where("name").regex(pattern),
                    Criteria.where("email").regex(pattern),
                    Criteria.where("_id").regex(pattern)
            ));
        }
        query.with(Sort.by(Sort.Direction.DESC, "timeCreated"));

        List<Map<String, Object>> members = mongoTemplate.find(query, Member.class).stream()
                .map(member -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("memberId", member.getId());
                    item.put("name", member.getName());
                    item.put("email", member.getEmail());
                    item.put("phone", member.getPhone());
                    
                    // Calculate active loan count dynamically
                    int activeLoanCount = loanRepository.findByMemberIdAndStatusIn(member.getId(), ACTIVE_LOAN_STATUSES).size();
                    item.put("activeLoanCount", activeLoanCount);
                    
                    item.put("isActive", member.isActive());
                    item.put("membershipType", member.getMembershipType());
                    item.put("timeCreated", member.getTimeCreated());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("members", members);
        response.put("total", members.size());
        return response;
    }

    public Map<String, Object> getNotifications(String staffId) {
        Query query = new Query(Criteria.where("memberId").is(staffId))
                .with(Sort.by(Sort.Direction.DESC, "createdAt"))
                .limit(50);
        List<Map<String, Object>> notifications = mongoTemplate.find(query, Notification.class).stream()
                .map(notification -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", notification.getId());
                    item.put("type", notification.getType());
                    item.put("title", notification.getTitle());
                    item.put("message", notification.getMessage());
                    item.put("memberId", notification.getMemberId());
                    item.put("status", notification.getStatus());
                    item.put("sentByStaffId", notification.getSentByStaffId());
                    item.put("isAutoTriggered", notification.isAutoTriggered());
                    item.put("createdAt", notification.getCreatedAt());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", notifications);
        response.put("total", notifications.size());
        response.put("unreadCount", notifications.stream()
                .filter(item -> "Sent".equalsIgnoreCase(String.valueOf(item.get("status"))) || "Unread".equalsIgnoreCase(String.valueOf(item.get("status"))))
                .count());
        return response;
    }

    public Map<String, Object> sendNotification(SendNotificationRequest request, String staffId) {
        Member member = memberRepository.findById(request.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getMemberId().equals(member.getId())) {
            throw new IllegalArgumentException("Loan does not belong to member");
        }

        String message = request.getMessage() == null || request.getMessage().isBlank()
                ? "Your book '" + loan.getBookTitle() + "' is overdue since " + loan.getDueDate() + ". Fine: ₹" + loan.getFineAmount() + ". Please return immediately."
                : request.getMessage();
        Notification notification = notificationService.createNotification(
                member.getId(),
                "OVERDUE_ALERT",
                "Overdue book reminder",
                message,
                loan.getId(),
                loan.getBookId(),
                "app",
                "Sent",
                staffId,
                false
        );
        activityLogService.log("NOTIFICATION_SENT", staffId, "ROLE_STAFF", "notifications", notification.getId(),
                null, Map.of("memberId", member.getId(), "loanId", loan.getId()));
        return Map.of("notificationId", notification.getId(), "memberId", member.getId(), "status", notification.getStatus(), "sentAt", notification.getCreatedAt());
    }

    public Map<String, Object> sendBulkOverdueNotifications(String staffId) {
        List<Loan> overdueLoans = new ArrayList<>();
        overdueLoans.addAll(loanRepository.findByStatus("OVERDUE"));
        overdueLoans.addAll(loanRepository.findByStatus("Overdue"));
        Map<String, Integer> overdueCountByMember = new LinkedHashMap<>();
        for (Loan loan : overdueLoans) {
            overdueCountByMember.put(loan.getMemberId(), overdueCountByMember.getOrDefault(loan.getMemberId(), 0) + 1);
        }

        List<String> memberIds = new ArrayList<>();
        for (Map.Entry<String, Integer> entry : overdueCountByMember.entrySet()) {
            notificationService.createNotification(
                    entry.getKey(),
                    "OVERDUE_ALERT",
                    "Overdue reminder",
                    "You have " + entry.getValue() + " overdue book(s). Please return immediately.",
                    null,
                    null,
                    "app",
                    "Sent",
                    staffId,
                    false
            );
            memberIds.add(entry.getKey());
        }
        activityLogService.log("BULK_NOTIFICATION_SENT", staffId, "ROLE_STAFF", "notifications", staffId,
                null, Map.of("totalSent", memberIds.size(), "memberIds", memberIds));
        return Map.of("sent", memberIds.size(), "failed", 0, "memberIds", memberIds);
    }

    public Map<String, Object> getStaffDashboard(String staffId) {
        LocalDateTime startOfToday = LocalDate.now().atStartOfDay();
        long issuedToday = loanRepository.countByIssuedByStaffIdAndIssuedAtGreaterThanEqual(staffId, startOfToday);
        long returnedToday = loanRepository.countByReturnedByStaffIdAndReturnedAtGreaterThanEqual(staffId, startOfToday);
        long activeLoans = loanRepository.countByStatus("ISSUED") + loanRepository.countByStatus("RENEWED")
                + loanRepository.countByStatus("Active");
        long overdueCount = loanRepository.countByStatus("OVERDUE") + loanRepository.countByStatus("Overdue");
        long pendingFines = fineRecordRepository.countByStatus("Pending");
        long totalMembers = memberRepository.countByIsActive(true);
        long dueToday = loanRepository.findAll().stream()
                .filter(loan -> ACTIVE_LOAN_STATUSES.contains(loan.getStatus()))
                .filter(loan -> loan.getDueDate() != null && loan.getDueDate().toLocalDate().isEqual(LocalDate.now()))
                .count();

        List<Map<String, Object>> loansThisWeek = new ArrayList<>();
        for (int i = 6; i >= 0; i--) {
            LocalDate date = LocalDate.now().minusDays(i);
            long count = loanRepository.findAll().stream()
                    .filter(loan -> staffId.equals(loan.getIssuedByStaffId()) && loan.getIssuedAt() != null && loan.getIssuedAt().toLocalDate().isEqual(date))
                    .count();
            loansThisWeek.add(Map.of("day", date.getDayOfWeek().name().substring(0, 3), "count", count));
        }

        List<Map<String, Object>> recentActivity = activityLogRepository.findTop8ByActorIdOrderByCreatedAtDesc(staffId).stream()
                .map(log -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("eventType", log.getEventType());
                    item.put("targetId", log.getTargetId());
                    item.put("createdAt", log.getCreatedAt());
                    item.put("title", buildActivityTitle(log));
                    item.put("subtitle", buildActivitySubtitle(log));
                    return item;
                })
                .toList();

        List<Map<String, Object>> reminders = mongoTemplate.find(
                        new Query().with(Sort.by(Sort.Direction.DESC, "createdAt")).limit(3),
                        Notification.class
                ).stream()
                .map(notification -> {
                    Map<String, Object> item = new LinkedHashMap<>();
                    item.put("id", notification.getId());
                    item.put("type", notification.getType());
                    item.put("title", notification.getTitle());
                    item.put("message", notification.getMessage());
                    item.put("createdAt", notification.getCreatedAt());
                    return item;
                })
                .toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("issuedToday", issuedToday);
        response.put("returnedToday", returnedToday);
        response.put("activeLoans", activeLoans);
        response.put("overdueCount", overdueCount);
        response.put("dueToday", dueToday);
        response.put("pendingFines", pendingFines);
        response.put("totalMembers", totalMembers);
        response.put("loansThisWeek", loansThisWeek);
        response.put("recentActivity", recentActivity);
        response.put("recentTransactions", recentActivity);
        response.put("reminders", reminders);
        return response;
    }

    private String buildActivityTitle(com.goandstudybackend.entity.ActivityLog log) {
        return switch (log.getEventType()) {
            case "BOOK_ISSUED" -> "Book issued";
            case "BOOK_RETURNED" -> "Book returned";
            case "LOAN_RENEWED" -> "Loan renewed";
            case "NOTIFICATION_SENT" -> "Notification sent";
            case "FINE_COLLECTED" -> "Fine collected";
            case "MEMBER_VIEWED" -> "Member viewed";
            default -> log.getEventType().replace('_', ' ').toLowerCase();
        };
    }

    private String buildActivitySubtitle(com.goandstudybackend.entity.ActivityLog log) {
        if (log.getMetadata() != null && !log.getMetadata().isEmpty()) {
            if (log.getMetadata().containsKey("memberId")) {
                return "Member " + log.getMetadata().get("memberId");
            }
            if (log.getMetadata().containsKey("bookId")) {
                return "Book " + log.getMetadata().get("bookId");
            }
        }
        return log.getTargetId() == null ? "Recorded in activity log" : "Reference " + log.getTargetId();
    }
}
