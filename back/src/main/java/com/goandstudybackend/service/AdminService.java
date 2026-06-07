package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.CreateStaffRequest;
import com.goandstudybackend.dto.request.MembershipUpgradeRequest;
import com.goandstudybackend.dto.request.ResetPasswordRequest;
import com.goandstudybackend.entity.Admin;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.Staff;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.ActivityLogRepository;
import com.goandstudybackend.repository.AdminRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.MemberAnalyticsRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.StaffRepository;
import com.goandstudybackend.util.MembershipType;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.mongodb.core.MongoTemplate;
import org.springframework.data.mongodb.core.query.Criteria;
import org.springframework.data.mongodb.core.query.Query;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class AdminService {
    private static final List<String> ACTIVE_LOAN_STATUSES = List.of("ISSUED", "RENEWED", "OVERDUE", "Active", "Overdue", "Issued", "Renewed");

    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;
    private final MemberAnalyticsRepository memberAnalyticsRepository;
    private final AdminRepository adminRepository;
    private final LoanRepository loanRepository;
    private final ActivityLogRepository activityLogRepository;
    private final ActivityLogService activityLogService;
    private final NotificationService notificationService;
    private final RealtimeEventService realtimeEventService;
    private final PasswordEncoder passwordEncoder;
    private final MongoTemplate mongoTemplate;

    public Map<String, Object> createStaff(CreateStaffRequest request, String adminId) {
        if (staffRepository.findByEmail(request.getEmail()).isPresent()) {
            throw new DuplicateResourceException("Staff email already exists");
        }
        Staff staff = staffRepository.save(Staff.builder()
                .id(request.getStaffId())
                .name(request.getName())
                .email(request.getEmail())
                .phone(request.getPhone())
                .passwordHash(passwordEncoder.encode(request.getTemporaryPassword()))
                .plainPassword(request.getTemporaryPassword())
                .role(request.getRole())
                .isActive(true)
                .createdByAdminId(adminId)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build());
        activityLogService.log("STAFF_CREATED", adminId, "ROLE_ADMIN", "staff", staff.getId());
        return Map.of(
                "staffId", staff.getId(),
                "name", staff.getName(),
                "email", staff.getEmail(),
                "isActive", staff.isActive(),
                "createdAt", staff.getCreatedAt()
        );
    }

    public Map<String, Object> getAllStaff(Boolean isActive, int page, int limit) {
        List<Staff> staff = isActive == null
                ? staffRepository.findAll(PageRequest.of(Math.max(page - 1, 0), limit)).getContent()
                : staffRepository.findByIsActive(isActive, PageRequest.of(Math.max(page - 1, 0), limit)).getContent();
        long total = isActive == null ? staffRepository.count() : staffRepository.countByIsActive(isActive);
        return Map.of("staff", staff, "total", total, "page", page, "pages", (int) Math.ceil((double) total / limit));
    }

    public Staff getStaffById(String staffId) {
        return staffRepository.findById(staffId).orElseThrow(() -> new ResourceNotFoundException("Staff not found"));
    }

    public Map<String, Object> deactivateStaff(String staffId, String adminId) {
        Staff staff = getStaffById(staffId);
        if (!staff.isActive()) {
            throw new IllegalArgumentException("Staff already inactive");
        }
        staff.setActive(false);
        staff.setDeactivatedAt(LocalDateTime.now());
        staff.setDeactivatedByAdminId(adminId);
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
        activityLogService.log("STAFF_DEACTIVATED", adminId, "ROLE_ADMIN", "staff", staffId);
        return Map.of("staffId", staffId, "isActive", false, "deactivatedAt", staff.getDeactivatedAt());
    }

    public Map<String, Object> reactivateStaff(String staffId, String adminId) {
        Staff staff = getStaffById(staffId);
        if (staff.isActive()) {
            throw new IllegalArgumentException("Staff already active");
        }
        staff.setActive(true);
        staff.setDeactivatedAt(null);
        staff.setDeactivatedByAdminId(null);
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
        activityLogService.log("STAFF_REACTIVATED", adminId, "ROLE_ADMIN", "staff", staffId);
        return Map.of("staffId", staffId, "isActive", true);
    }

    public Map<String, Object> resetStaffPassword(String staffId, ResetPasswordRequest request, String adminId) {
        Staff staff = getStaffById(staffId);
        staff.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        staff.setPlainPassword(request.getNewPassword());
        staff.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(staff);
        activityLogService.log("PASSWORD_RESET_BY_ADMIN", adminId, "ROLE_ADMIN", "staff", staffId);
        return Map.of("message", "Password reset successfully for " + staffId);
    }

    public Map<String, Object> getStaffActivity(String staffId, int page) {
        var pageResult = activityLogRepository.findByActorIdOrderByCreatedAtDesc(staffId, PageRequest.of(Math.max(page - 1, 0), 20));
        return Map.of("activities", pageResult.getContent(), "total", pageResult.getTotalElements(), "page", page);
    }

    public Map<String, Object> getAllMembers(Boolean isActive, String sortBy, int page) {
        Query query = new Query();
        if (isActive != null) {
            query.addCriteria(Criteria.where("isActive").is(isActive));
        }
        if (sortBy != null && !sortBy.isBlank()) {
            query.with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, sortBy));
        }
        query.with(PageRequest.of(Math.max(page - 1, 0), 20));
        query.fields().exclude("passwordHash");
        List<Member> members = mongoTemplate.find(query, Member.class);
        long total = isActive == null ? memberRepository.count() : memberRepository.countByIsActive(isActive);

        // Transform members to include createdAt and activeLoanCount
        List<Map<String, Object>> transformedMembers = members.stream().map(member -> {
            Map<String, Object> m = new LinkedHashMap<>();
            m.put("id", member.getId());
            m.put("name", member.getName());
            m.put("email", member.getEmail());
            m.put("phone", member.getPhone());
            m.put("gender", member.getGender());
            m.put("role", member.getRole());
            m.put("isActive", member.isActive());
            m.put("createdAt", member.getTimeCreated());
            m.put("dateOfBirth", member.getDateOfBirth());
            m.put("registrationDate", member.getRegistrationDate());
            m.put("loginCount", member.getLoginCount());
            m.put("membershipType", member.getMembershipType());
m.put("lastLogin", member.getLastLogin());
             m.put("maxActiveLoans", member.getMaxActiveLoans());
             m.put("totalLoans", member.getTotalLoans());
             m.put("totalFinesPaid", member.getTotalFinesPaid());
             m.put("plainPassword", member.getPlainPassword());

            // Calculate active loan count dynamically
            int activeLoanCount = loanRepository.findByMemberIdAndStatusIn(member.getId(), ACTIVE_LOAN_STATUSES).size();
            m.put("activeLoanCount", activeLoanCount);

            return m;
        }).toList();

        return Map.of("members", transformedMembers, "total", total, "page", page);
    }

    public Map<String, Object> getMemberWithAnalytics(String memberId, String adminId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        member.setPasswordHash(null);
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("member", member);
        response.put("analytics", memberAnalyticsRepository.findById(memberId).orElse(null));
        response.put("recentLoans", mongoTemplate.find(
                new Query(Criteria.where("memberId").is(memberId)).with(PageRequest.of(0, 5))
                        .with(org.springframework.data.domain.Sort.by(org.springframework.data.domain.Sort.Direction.DESC, "issuedAt")),
                com.goandstudybackend.entity.Loan.class
        ));
        activityLogService.log("MEMBER_VIEWED", adminId, "ROLE_ADMIN", "members", memberId);
        return response;
    }

    public Map<String, Object> deactivateMember(String memberId, String adminId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (!member.isActive()) {
            throw new IllegalArgumentException("Member already inactive");
        }
        member.setActive(false);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
        activityLogService.log("MEMBER_DEACTIVATED", adminId, "ROLE_ADMIN", "members", memberId);
        realtimeEventService.broadcast("MEMBER_DEACTIVATED", "admin", Map.of("memberId", memberId));
        return Map.of("memberId", memberId, "isActive", false);
    }

    public Map<String, Object> reactivateMember(String memberId, String adminId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (member.isActive()) {
            throw new IllegalArgumentException("Member already active");
        }
        member.setActive(true);
        member.setUpdatedAt(LocalDateTime.now());
        memberRepository.save(member);
        activityLogService.log("MEMBER_REACTIVATED", adminId, "ROLE_ADMIN", "members", memberId);
        realtimeEventService.broadcast("MEMBER_REACTIVATED", "admin", Map.of("memberId", memberId));
        return Map.of("memberId", memberId, "isActive", true);
    }

    public Map<String, Object> deleteMember(String memberId, String adminId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        boolean hasActiveLoans = !loanRepository.findByMemberIdAndStatusIn(memberId, ACTIVE_LOAN_STATUSES).isEmpty();
        if (hasActiveLoans) {
            throw new DuplicateResourceException("Cannot delete member with active loans");
        }
        memberRepository.deleteById(memberId);
        memberAnalyticsRepository.deleteById(memberId);
        activityLogService.log("MEMBER_DELETED", adminId, "ROLE_ADMIN", "members", memberId);
        realtimeEventService.broadcast("MEMBER_DELETED", "admin", Map.of("memberId", memberId));
        return Map.of("memberId", memberId, "deleted", true);
    }

    public Map<String, Object> upgradeMembership(String memberId, MembershipUpgradeRequest request, String adminId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        MembershipType type = MembershipType.valueOf(request.getMembershipType().toUpperCase());
        Map<String, Object> before = Map.of("membershipType", member.getMembershipType());
        LocalDateTime now = LocalDateTime.now();

        member.setMembershipType(type.name());
        member.setMaxActiveLoans(type.maxActiveLoans);
        member.setLoanDurationDays(type.loanDurationDays);
        member.setFineRatePerDay(type.fineRatePerDay);
        member.setRenewalLimit(type.renewalLimit);
        member.setMembershipStartDate(now);
        member.setMembershipExpiryDate(type == MembershipType.BASIC ? null : now.plusYears(1));
        member.setMembershipFeePaid(request.getMembershipFeePaid());
        member.setUpdatedAt(now);
        memberRepository.save(member);

        notificationService.createNotification(
                memberId,
                "GENERAL",
                "Membership upgraded",
                "Your membership has been upgraded to " + type.displayName + "! New limits: "
                        + type.maxActiveLoans + " books, " + type.loanDurationDays + "-day loans.",
                null,
                null,
                "app",
                "Sent",
                null,
                false
        );
        activityLogService.log(
                "MEMBERSHIP_UPGRADED",
                adminId,
                "ROLE_ADMIN",
                "members",
                memberId,
                Map.of("before", before, "after", Map.of("membershipType", type.name())),
                null
        );
        realtimeEventService.broadcastMembershipUpgraded(memberId, type.name());

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("memberId", memberId);
        response.put("membershipType", member.getMembershipType());
        response.put("maxActiveLoans", member.getMaxActiveLoans());
        response.put("loanDurationDays", member.getLoanDurationDays());
        response.put("fineRatePerDay", member.getFineRatePerDay());
        response.put("renewalLimit", member.getRenewalLimit());
        response.put("membershipExpiryDate", member.getMembershipExpiryDate());
        return response;
    }

    public Map<String, Object> getMembershipStats() {
        Map<String, Long> stats = new LinkedHashMap<>();
        for (Member member : memberRepository.findAll()) {
            stats.put(member.getMembershipType(), stats.getOrDefault(member.getMembershipType(), 0L) + 1);
        }
        long total = stats.values().stream().mapToLong(Long::longValue).sum();
        return Map.of("stats", stats, "total", total);
    }

    public Map<String, Object> sendBulkOverdueAlerts(String adminId) {
        List<Loan> overdueLoans = new java.util.ArrayList<>();
        overdueLoans.addAll(loanRepository.findByStatus("OVERDUE"));
        overdueLoans.addAll(loanRepository.findByStatus("Overdue"));
        Map<String, Loan> firstOverdueByMember = new LinkedHashMap<>();
        for (Loan loan : overdueLoans) {
            firstOverdueByMember.putIfAbsent(loan.getMemberId(), loan);
        }

        List<String> memberIds = new java.util.ArrayList<>();
        for (Map.Entry<String, Loan> entry : firstOverdueByMember.entrySet()) {
            Loan loan = entry.getValue();
            notificationService.createNotification(
                    entry.getKey(),
                    "OVERDUE_ALERT",
                    "Overdue book reminder",
                    "You have an overdue book: " + loan.getBookTitle() + ". Fine: Rs " + loan.getFineAmount(),
                    loan.getId(),
                    loan.getBookId(),
                    "app",
                    "Sent",
                    null,
                    false
            );
            memberIds.add(entry.getKey());
        }
        activityLogService.log("BULK_ALERT_SENT", adminId, "ROLE_ADMIN", "notifications", adminId,
                null, Map.of("alertsSent", memberIds.size()));
        return Map.of("alertsSent", memberIds.size(), "memberIds", memberIds);
    }

    public Map<String, Object> changeAdminPassword(String adminId, ChangePasswordRequest request) {
        Admin admin = adminRepository.findById(adminId).orElseThrow(() -> new ResourceNotFoundException("Admin not found"));
        if (!passwordEncoder.matches(request.getCurrentPassword(), admin.getPasswordHash())) {
            throw new IllegalArgumentException("Incorrect current password");
        }
        if (request.getNewPassword().length() < 8) {
            throw new IllegalArgumentException("New password must be at least 8 characters");
        }
        admin.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        admin.setUpdatedAt(LocalDateTime.now());
        adminRepository.save(admin);
        activityLogService.log("PASSWORD_CHANGED", adminId, "ROLE_ADMIN", "admins", adminId);
        return Map.of("message", "Password updated successfully");
    }
}
