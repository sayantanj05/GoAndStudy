package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.BookRequestRequest;
import com.goandstudybackend.dto.request.ReviewRequest;
import com.goandstudybackend.entity.BookRequest;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookRequestRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class BookRequestService {

    private final BookRequestRepository bookRequestRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;

    public Map<String, Object> requestBook(String memberId, String memberName, BookRequestRequest request) {
        LocalDateTime yearStart = LocalDate.now().withDayOfYear(1).atStartOfDay();
        LocalDateTime yearEnd = LocalDate.now().withDayOfYear(LocalDate.now().lengthOfYear()).atTime(23, 59, 59);
        if (bookRequestRepository.existsByMemberIdAndTitleIgnoreCaseAndCreatedAtBetween(memberId, request.getTitle(), yearStart, yearEnd)) {
            throw new DuplicateResourceException("You already requested this title this year");
        }

        BookRequest bookRequest = bookRequestRepository.save(BookRequest.builder()
                .memberId(memberId)
                .memberName(memberName)
                .title(request.getTitle())
                .author(request.getAuthor())
                .isbn(request.getIsbn())
                .reason(request.getReason())
                .status("Pending")
                .createdAt(LocalDateTime.now())
                .build());
        activityLogService.log("BOOK_REQUESTED", memberId, "ROLE_MEMBER", "book_requests", bookRequest.getId());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", bookRequest.getId());
        response.put("title", bookRequest.getTitle());
        response.put("status", bookRequest.getStatus());
        response.put("createdAt", bookRequest.getCreatedAt());
        return response;
    }

    public Map<String, Object> getMyBookRequests(String memberId) {
        return Map.of("requests", bookRequestRepository.findByMemberIdOrderByCreatedAtDesc(memberId));
    }

    public Map<String, Object> cancelBookRequest(String memberId, String requestId) {
        BookRequest bookRequest = bookRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Book request not found"));
        if (!bookRequest.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Book request does not belong to this member");
        }
        if (!"Pending".equalsIgnoreCase(bookRequest.getStatus())) {
            throw new IllegalArgumentException("Only pending requests can be cancelled");
        }
        bookRequestRepository.delete(bookRequest);
        return Map.of("message", "Request cancelled");
    }

    public Map<String, Object> getAllBookRequests(String status, int page) {
        List<BookRequest> requests = (status == null || status.isBlank())
                ? bookRequestRepository.findAllByOrderByCreatedAtDesc(PageRequest.of(Math.max(page - 1, 0), 20)).getContent()
                : bookRequestRepository.findByStatusOrderByCreatedAtDesc(status, PageRequest.of(Math.max(page - 1, 0), 20)).getContent();
        long total = status == null || status.isBlank() ? bookRequestRepository.count() : requests.size();
        return Map.of("requests", requests, "total", total);
    }

    public Map<String, Object> reviewBookRequest(String requestId, ReviewRequest request, String adminId) {
        BookRequest bookRequest = bookRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Book request not found"));
        bookRequest.setStatus(request.getStatus());
        bookRequest.setAdminNote(request.getAdminNote());
        bookRequest.setReviewedByAdminId(adminId);
        bookRequest.setReviewedAt(LocalDateTime.now());
        bookRequestRepository.save(bookRequest);

        notificationService.createNotification(
                bookRequest.getMemberId(),
                "GENERAL",
                "Book request update",
                "Your book request for '" + bookRequest.getTitle() + "' has been " + request.getStatus()
                        + (request.getAdminNote() == null || request.getAdminNote().isBlank() ? "" : ". " + request.getAdminNote()),
                null,
                null,
                "app",
                "Sent",
                null,
                false
        );
        activityLogService.log("BOOK_REQUEST_REVIEWED", adminId, "ROLE_ADMIN", "book_requests", requestId);

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("status", bookRequest.getStatus());
        response.put("adminNote", bookRequest.getAdminNote());
        return response;
    }
}
