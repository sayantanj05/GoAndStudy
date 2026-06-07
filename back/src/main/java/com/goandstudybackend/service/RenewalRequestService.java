package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.RejectRequest;
import com.goandstudybackend.dto.request.RenewalRequestRequest;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.RenewalRequest;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.RenewalRequestRepository;
import com.goandstudybackend.repository.ReservationRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class RenewalRequestService {

    private final RenewalRequestRepository renewalRequestRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final ReservationRepository reservationRepository;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final RealtimeEventService realtimeEventService;

    public Map<String, Object> requestRenewal(String memberId, String memberName, RenewalRequestRequest request) {
        Loan loan = loanRepository.findById(request.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        if (!loan.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Loan does not belong to this member");
        }
        if (!List.of("ISSUED", "RENEWED", "Active", "Issued", "Renewed").contains(loan.getStatus())) {
            throw new IllegalArgumentException("Only active loans can be renewed");
        }
        Member member = memberRepository.findById(memberId)
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (loan.getRenewalCount() >= member.getRenewalLimit()) {
            throw new IllegalArgumentException("Renewal limit reached for your membership type");
        }
        if (renewalRequestRepository.existsByLoanIdAndStatus(loan.getId(), "Pending")) {
            throw new DuplicateResourceException("Pending renewal request already exists");
        }

        RenewalRequest renewalRequest = renewalRequestRepository.save(RenewalRequest.builder()
                .memberId(memberId)
                .loanId(loan.getId())
                .bookTitle(loan.getBookTitle())
                .memberName(memberName)
                .currentDueDate(loan.getDueDate())
                .requestedAt(LocalDateTime.now())
                .status("Pending")
                .build());

        notificationService.createNotification(
                null,
                "GENERAL",
                "Renewal request",
                "Renewal request from " + memberName + " for '" + loan.getBookTitle() + "'",
                loan.getId(),
                loan.getBookId(),
                "app",
                "Sent",
                null,
                false
        );
        realtimeEventService.broadcast(
                "RENEWAL_REQUEST_CREATED",
                "staff",
                Map.of(
                        "requestId", renewalRequest.getId(),
                        "memberId", memberId,
                        "bookTitle", loan.getBookTitle(),
                        "hasReservationQueue", reservationRepository.existsByBookIdAndStatus(loan.getBookId(), "Pending")
                )
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", renewalRequest.getId());
        response.put("loanId", loan.getId());
        response.put("bookTitle", loan.getBookTitle());
        response.put("currentDueDate", loan.getDueDate());
        response.put("status", renewalRequest.getStatus());
        return response;
    }

    public Map<String, Object> getMyRenewalRequests(String memberId) {
        return Map.of("requests", renewalRequestRepository.findByMemberIdOrderByRequestedAtDesc(memberId));
    }

    public Map<String, Object> cancelRenewalRequest(String memberId, String requestId) {
        RenewalRequest renewalRequest = renewalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Renewal request not found"));
        if (!renewalRequest.getMemberId().equals(memberId)) {
            throw new IllegalArgumentException("Renewal request does not belong to this member");
        }
        if (!"Pending".equalsIgnoreCase(renewalRequest.getStatus())) {
            throw new IllegalArgumentException("Only pending renewal requests can be cancelled");
        }
        renewalRequestRepository.delete(renewalRequest);
        return Map.of("message", "Renewal request cancelled");
    }

    public Map<String, Object> getPendingRenewalRequests() {
        List<RenewalRequest> requests = renewalRequestRepository.findByStatusOrderByRequestedAtDesc("Pending");
        return Map.of("requests", requests);
    }

    public Map<String, Object> approveRenewal(String requestId, String staffId) {
        RenewalRequest renewalRequest = renewalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Renewal request not found"));
        Loan loan = loanRepository.findById(renewalRequest.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        Member member = memberRepository.findById(renewalRequest.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (loan.getRenewalCount() >= member.getRenewalLimit()) {
            throw new IllegalArgumentException("Renewal limit reached for your membership type");
        }

        loan.setDueDate(loan.getDueDate().plusDays(member.getLoanDurationDays()));
        loan.setRenewalCount(loan.getRenewalCount() + 1);
        loan.setUpdatedAt(LocalDateTime.now());
        loanRepository.save(loan);

        renewalRequest.setStatus("Approved");
        renewalRequest.setProcessedByStaffId(staffId);
        renewalRequest.setProcessedAt(LocalDateTime.now());
        renewalRequestRepository.save(renewalRequest);

        notificationService.createNotification(
                member.getId(),
                "GENERAL",
                "Renewal approved",
                "Your renewal for '" + renewalRequest.getBookTitle() + "' was approved. New due date: " + loan.getDueDate(),
                loan.getId(),
                loan.getBookId(),
                "app",
                "Sent",
                staffId,
                false
        );
        activityLogService.log("LOAN_RENEWED", staffId, "ROLE_STAFF", "renewal_requests", requestId);
        realtimeEventService.broadcast(
                "LOAN_RENEWED",
                "member-" + member.getId(),
                Map.of("requestId", requestId, "loanId", loan.getId(), "newDueDate", loan.getDueDate())
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("loanId", loan.getId());
        response.put("newDueDate", loan.getDueDate());
        response.put("status", "Approved");
        return response;
    }

    public Map<String, Object> rejectRenewal(String requestId, RejectRequest request, String staffId) {
        RenewalRequest renewalRequest = renewalRequestRepository.findById(requestId)
                .orElseThrow(() -> new ResourceNotFoundException("Renewal request not found"));
        renewalRequest.setStatus("Rejected");
        renewalRequest.setRejectionReason(request.getReason());
        renewalRequest.setProcessedByStaffId(staffId);
        renewalRequest.setProcessedAt(LocalDateTime.now());
        renewalRequestRepository.save(renewalRequest);

        notificationService.createNotification(
                renewalRequest.getMemberId(),
                "GENERAL",
                "Renewal rejected",
                "Your renewal for '" + renewalRequest.getBookTitle() + "' was not approved. Reason: " + request.getReason(),
                renewalRequest.getLoanId(),
                null,
                "app",
                "Sent",
                staffId,
                false
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("requestId", requestId);
        response.put("status", "Rejected");
        response.put("rejectionReason", request.getReason());
        return response;
    }
}
