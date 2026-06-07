package com.goandstudybackend.service;

import com.goandstudybackend.entity.Notification;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.exception.UnauthorizedException;
import com.goandstudybackend.repository.NotificationRepository;
import com.goandstudybackend.repository.BookReviewRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final BookReviewRepository bookReviewRepository;
    private final MemberRepository memberRepository;
    private final StaffRepository staffRepository;

    public Notification createNotification(String memberId,
                                           String type,
                                           String title,
                                           String message,
                                           String relatedLoanId,
                                           String relatedBookId,
                                           String channel,
                                           String status,
                                           String sentByStaffId,
                                           boolean autoTriggered) {
        Notification notification = Notification.builder()
                .memberId(memberId)
                .type(type)
                .title(title)
                .message(message)
                .relatedLoanId(relatedLoanId)
                .relatedBookId(relatedBookId)
                .channel(channel)
                .status(status)
                .sentByStaffId(sentByStaffId)
                .isAutoTriggered(autoTriggered)
                .createdAt(LocalDateTime.now())
                .build();
        Notification saved = notificationRepository.save(notification);
        log.info("Created notification for member: {}", memberId);
        return saved;
    }

    public Map<String, Object> getNotifications(String memberId, String status) {
        List<Notification> notifications = (status == null || status.isBlank())
                ? notificationRepository.findByMemberIdOrderByCreatedAtDesc(memberId)
                : notificationRepository.findByMemberIdAndStatusOrderByCreatedAtDesc(memberId, status);
        List<Map<String, Object>> notificationsWithReviewStatus = notifications.stream().map(notif -> {
            Map<String, Object> notifMap = new LinkedHashMap<>();
            notifMap.put("id", notif.getId());
            notifMap.put("type", notif.getType());
            notifMap.put("title", notif.getTitle());
            notifMap.put("message", notif.getMessage());
            notifMap.put("status", notif.getStatus());
            notifMap.put("relatedLoanId", notif.getRelatedLoanId());
            notifMap.put("relatedBookId", notif.getRelatedBookId());
            notifMap.put("createdAt", notif.getCreatedAt());
            notifMap.put("readAt", notif.getReadAt());

            boolean alreadyReviewed = false;
            if (notif.getRelatedBookId() != null && !notif.getRelatedBookId().isBlank()) {
                alreadyReviewed = bookReviewRepository.existsByMemberIdAndBookId(memberId, notif.getRelatedBookId());
            }
            notifMap.put("alreadyReviewed", alreadyReviewed);

            return notifMap;
        }).toList();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifications", notificationsWithReviewStatus);
        response.put("unreadCount", notificationRepository.countByMemberIdAndStatus(memberId, "Sent"));
        return response;
    }

    public Map<String, Object> markAsRead(String memberId, String notificationId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification not found"));
        if (notification.getMemberId() == null || !notification.getMemberId().equals(memberId)) {
            throw new UnauthorizedException("Notification does not belong to this member");
        }
        notification.setStatus("Read");
        notification.setReadAt(LocalDateTime.now());
        notificationRepository.save(notification);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("notifId", notification.getId());
        response.put("readAt", notification.getReadAt());
        return response;
    }

    public Notification createReturnNotification(String memberId, String bookTitle, String bookId, String loanId) {
        return createNotification(
                memberId,
                "RATE_BOOK_RETURNED",
                "Rate Book: " + bookTitle,
                "Thank you for returning '" + bookTitle + "'. Please rate the book and share your feedback!",
                loanId,
                bookId,
                "IN_APP",
                "Sent",
                null,
                true
        );
    }

    public void broadcastNotification(String message, String sentByAdminId) {
        log.info("Broadcasting notification from admin: {}", sentByAdminId);
        
        try {
            // Get all active members using findAll and filter
            List<com.goandstudybackend.entity.Member> allMembers = memberRepository.findAll();
            List<com.goandstudybackend.entity.Member> activeMembers = allMembers.stream()
                    .filter(com.goandstudybackend.entity.Member::isActive)
                    .toList();
            
            // Get all active staff using findAll and filter
            List<com.goandstudybackend.entity.Staff> allStaff = staffRepository.findAll();
            List<com.goandstudybackend.entity.Staff> activeStaff = allStaff.stream()
                    .filter(com.goandstudybackend.entity.Staff::isActive)
                    .toList();
            
            int totalNotifications = 0;
            
            // Create notifications for all active members
            for (com.goandstudybackend.entity.Member member : activeMembers) {
                createNotification(
                        member.getId(),
                        "BROADCAST",
                        "System Announcement",
                        message,
                        null,
                        null,
                        "IN_APP",
                        "Sent",
                        sentByAdminId,
                        false
                );
                totalNotifications++;
            }
            
            // Create notifications for all active staff
            for (com.goandstudybackend.entity.Staff staff : activeStaff) {
                createNotification(
                        staff.getId(),
                        "BROADCAST",
                        "System Announcement", 
                        message,
                        null,
                        null,
                        "IN_APP",
                        "Sent",
                        sentByAdminId,
                        false
                );
                totalNotifications++;
            }
            
            log.info("Broadcast notification sent to {} users ({} members, {} staff)", 
                    totalNotifications, activeMembers.size(), activeStaff.size());
                    
        } catch (Exception e) {
            log.error("Error broadcasting notification: {}", e.getMessage(), e);
            throw new RuntimeException("Failed to broadcast notification: " + e.getMessage(), e);
        }
    }
}

