package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.CollectFineRequest;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class FineService {

    private final FineRecordRepository fineRecordRepository;
    private final LoanRepository loanRepository;
    private final MemberRepository memberRepository;
    private final ActivityLogService activityLogService;
    private final RealtimeEventService realtimeEventService;

    public Map<String, Object> getPendingFines() {
        List<FineRecord> fines = fineRecordRepository.findByStatusOrderByCreatedAtDesc("Pending");
        List<Map<String, Object>> items = new ArrayList<>();
        for (FineRecord fine : fines) {
            Member member = memberRepository.findById(fine.getMemberId()).orElse(null);
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fineId", fine.getId());
            item.put("memberId", fine.getMemberId());
            item.put("memberName", member == null ? "" : member.getName());
            item.put("bookTitle", fine.getBookTitle());
            item.put("overdueDays", fine.getOverdueDays());
            item.put("totalAmount", fine.getTotalAmount());
            item.put("createdAt", fine.getCreatedAt());
            items.add(item);
        }
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fines", items);
        response.put("total", items.size());
        return response;
    }

    public Map<String, Object> collectFine(String fineId, CollectFineRequest request, String staffId) {
        FineRecord fine = fineRecordRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        if (!"Pending".equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalArgumentException("Fine already processed");
        }
        if (Double.compare(request.getAmountCollected(), fine.getTotalAmount()) != 0) {
            throw new IllegalArgumentException("Amount mismatch");
        }

        LocalDateTime now = LocalDateTime.now();
        fine.setStatus("Paid");
        fine.setCollectedByStaffId(staffId);
        fine.setCollectedAt(now);
        fineRecordRepository.save(fine);

        Loan loan = loanRepository.findById(fine.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        loan.setFinePaid(true);
        loan.setFinePaidAt(now);
        loanRepository.save(loan);

        Member member = memberRepository.findById(fine.getMemberId())
                .orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setTotalFinesPaid(member.getTotalFinesPaid() + request.getAmountCollected());
        member.setUpdatedAt(now);
        memberRepository.save(member);

        activityLogService.log(
                "FINE_COLLECTED",
                staffId,
                "ROLE_STAFF",
                "fine_records",
                fineId,
                null,
                Map.of("memberId", fine.getMemberId(), "amount", request.getAmountCollected())
        );
        realtimeEventService.broadcastFineCollected(fine.getMemberId(), request.getAmountCollected());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fineId", fineId);
        response.put("status", "Paid");
        response.put("collectedAt", now);
        response.put("collectedByStaffId", staffId);
        return response;
    }

    public Map<String, Object> getAllFines(String status, int page) {
        List<FineRecord> fines;
        long total;
        if (status == null || status.isBlank()) {
            fines = fineRecordRepository.findAll(PageRequest.of(Math.max(page - 1, 0), 20)).getContent();
            total = fineRecordRepository.count();
        } else {
            fines = fineRecordRepository.findByStatus(status, PageRequest.of(Math.max(page - 1, 0), 20)).getContent();
            total = fineRecordRepository.countByStatus(status);
        }

        List<Map<String, Object>> items = new ArrayList<>();
        for (FineRecord fine : fines) {
            Map<String, Object> item = new LinkedHashMap<>();
            item.put("fineId", fine.getId());
            item.put("memberId", fine.getMemberId());
            // Look up member name
            String memberName = "Unknown";
            try {
                Member member = memberRepository.findById(fine.getMemberId()).orElse(null);
                if (member != null) {
                    memberName = member.getName();
                }
            } catch (Exception e) {
                // Use default name
            }
            item.put("memberName", memberName);
            item.put("bookTitle", fine.getBookTitle());
            item.put("overdueDays", fine.getOverdueDays());
            item.put("totalAmount", fine.getTotalAmount());
            item.put("status", fine.getStatus());
            item.put("createdAt", fine.getCreatedAt());
            items.add(item);
        }

        // Calculate total outstanding (Pending), total paid, and total waived
        double totalOutstanding = fineRecordRepository.findByStatusOrderByCreatedAtDesc("Pending")
            .stream().mapToDouble(FineRecord::getTotalAmount).sum();
        double totalPaid = fineRecordRepository.findByStatusOrderByCreatedAtDesc("Paid")
            .stream().mapToDouble(FineRecord::getTotalAmount).sum();
        double totalWaived = fineRecordRepository.findByStatusOrderByCreatedAtDesc("Waived")
            .stream().mapToDouble(FineRecord::getTotalAmount).sum();

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fines", items);
        response.put("total", total);
        response.put("totalOutstanding", totalOutstanding);
        response.put("totalPaid", totalPaid);
        response.put("totalWaived", totalWaived);
        return response;
    }

    public Map<String, Object> waiveFine(String fineId, String reason, String adminId) {
        FineRecord fine = fineRecordRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        if (!"Pending".equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalArgumentException("Fine already processed");
        }
        fine.setStatus("Waived");
        fine.setWaivedByAdminId(adminId);
        fine.setWaivedReason(reason);
        fineRecordRepository.save(fine);

        Loan loan = loanRepository.findById(fine.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        loan.setFinePaid(true);
        loanRepository.save(loan);

        activityLogService.log(
                "FINE_WAIVED",
                adminId,
                "ROLE_ADMIN",
                "fine_records",
                fineId,
                Map.of("before", Map.of("status", "Pending"), "after", Map.of("status", "Waived")),
                null
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fineId", fineId);
        response.put("status", "Waived");
        response.put("waivedReason", reason);
        return response;
    }

    public Map<String, Object> collectFine(String fineId, String adminId) {
        FineRecord fine = fineRecordRepository.findById(fineId)
                .orElseThrow(() -> new ResourceNotFoundException("Fine not found"));
        if (!"Pending".equalsIgnoreCase(fine.getStatus())) {
            throw new IllegalArgumentException("Fine already processed");
        }
        fine.setStatus("Paid");
        fine.setCollectedByStaffId(adminId);
        fine.setCollectedAt(LocalDateTime.now());
        fineRecordRepository.save(fine);

        // Update loan to mark fine as paid
        Loan loan = loanRepository.findById(fine.getLoanId())
                .orElseThrow(() -> new ResourceNotFoundException("Loan not found"));
        loan.setFinePaid(true);
        loanRepository.save(loan);

        // Update member's total fines paid
        Member member = memberRepository.findById(fine.getMemberId()).orElse(null);
        if (member != null) {
            member.setTotalFinesPaid(member.getTotalFinesPaid() + fine.getTotalAmount());
            memberRepository.save(member);
        }

        activityLogService.log(
                "FINE_COLLECTED",
                adminId,
                "ROLE_ADMIN",
                "fine_records",
                fineId,
                Map.of("before", Map.of("status", "Pending"), "after", Map.of("status", "Paid")),
                null
        );

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("fineId", fineId);
        response.put("status", "Paid");
        response.put("collectedAmount", fine.getTotalAmount());
        return response;
    }
}
