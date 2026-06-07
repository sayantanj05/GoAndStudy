package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.LoginRequest;
import com.goandstudybackend.dto.request.RegisterRequest;
import com.goandstudybackend.dto.response.AuthResponse;
import com.goandstudybackend.entity.Admin;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.MemberAnalytics;
import com.goandstudybackend.entity.MemberPreferences;
import com.goandstudybackend.entity.Staff;
import com.goandstudybackend.exception.DuplicateResourceException;
import com.goandstudybackend.exception.UnauthorizedException;
import com.goandstudybackend.repository.AdminRepository;
import com.goandstudybackend.repository.MemberAnalyticsRepository;
import com.goandstudybackend.repository.MemberPreferencesRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.StaffRepository;
import com.goandstudybackend.security.JwtUtil;
import com.goandstudybackend.util.DateUtil;
import com.goandstudybackend.util.MembershipType;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;
    private final MemberPreferencesRepository memberPreferencesRepository;
    private final MemberAnalyticsRepository memberAnalyticsRepository;
    private final MemberIdGeneratorService memberIdGeneratorService;
    private final NotificationService notificationService;
    private final ActivityLogService activityLogService;
    private final RealtimeEventService realtimeEventService;
    private final PasswordEncoder passwordEncoder;
    private final JwtUtil jwtUtil;

    public AuthResponse login(LoginRequest request) {
        String email = normalizeEmail(request.getEmail());

        Admin admin = adminRepository.findByEmailIgnoreCase(email).orElse(null);
        if (admin != null) {
            validatePassword(request.getPassword(), admin.getPasswordHash());
            admin.setLastLogin(LocalDateTime.now());
            admin.setLoginCount(admin.getLoginCount() + 1);
            adminRepository.save(admin);
            activityLogService.log("LOGIN", admin.getId(), admin.getRole(), "admins", admin.getId());
            return buildAuthResponse(admin.getId(), admin.getRole(), admin.getEmail(), admin.getUsername());
        }

        Staff staff = staffRepository.findByEmailIgnoreCase(email).orElse(null);
        if (staff != null) {
            if (!staff.isActive()) {
                throw new IllegalStateException("Account deactivated. Contact admin.");
            }
            validatePassword(request.getPassword(), staff.getPasswordHash());
            staff.setLastLogin(LocalDateTime.now());
            staff.setLoginCount(staff.getLoginCount() + 1);
            staffRepository.save(staff);
            activityLogService.log("LOGIN", staff.getId(), staff.getRole(), "staff", staff.getId());
            return buildAuthResponse(staff.getId(), staff.getRole(), staff.getEmail(), staff.getName());
        }

        Member member = memberRepository.findByEmailIgnoreCase(email).orElse(null);
        if (member != null) {
            if (!member.isActive()) {
                throw new IllegalStateException("Account deactivated");
            }
            validatePassword(request.getPassword(), member.getPasswordHash());
            member.setLastLogin(LocalDateTime.now());
            member.setLoginCount(member.getLoginCount() + 1);
            memberRepository.save(member);
            activityLogService.log("LOGIN", member.getId(), member.getRole(), "members", member.getId());
            return buildAuthResponse(member.getId(), member.getRole(), member.getEmail(), member.getName());
        }

        throw new UnauthorizedException("Invalid credentials");
    }

    public AuthResponse register(RegisterRequest request) {
        String email = normalizeEmail(request.getEmail());
        ensureEmailUnique(email);

        String memberId = memberIdGeneratorService.generateMemberId();
        String registrationDate = DateUtil.todayRegistrationKey();
        int dailySequence = Integer.parseInt(memberId.substring(memberId.length() - 3));
        MembershipType membershipType = MembershipType.BASIC;
        LocalDateTime now = LocalDateTime.now();

Member member = Member.builder()
                .id(memberId)
                .name(request.getName())
                .email(email)
                .phone(request.getPhone())
                .gender(request.getGender())
                .dateOfBirth(request.getDateOfBirth())
                .passwordHash(passwordEncoder.encode(request.getPassword()))
                .plainPassword(request.getPassword())
                .role("ROLE_MEMBER")
                .isActive(true)
                .registrationDate(registrationDate)
                .dailySequence(dailySequence)
                .timeCreated(now)
                .loginCount(0)
                .activeLoanCount(0)
                .totalLoans(0)
                .totalFinesPaid(0.0)
                .preferredGenres(request.getPreferredGenres())
                .readingGoalBooks(0)
                .membershipType(membershipType.name())
                .membershipStartDate(now)
                .membershipExpiryDate(null)
                .membershipFeePaid(membershipType.annualFee)
                .maxActiveLoans(membershipType.maxActiveLoans)
                .loanDurationDays(membershipType.loanDurationDays)
                .fineRatePerDay(membershipType.fineRatePerDay)
                .renewalLimit(membershipType.renewalLimit)
                .updatedAt(now)
                .build();
        memberRepository.save(member);

        memberPreferencesRepository.save(MemberPreferences.builder().id(memberId).lastComputedAt(now).build());
        memberAnalyticsRepository.save(MemberAnalytics.builder().id(memberId).computedAt(now).build());

        notificationService.createNotification(
                memberId,
                "WELCOME",
                "Welcome to GoAndStudy!",
                "Hi " + member.getName() + ", your account " + memberId + " is ready. Happy reading!",
                null,
                null,
                "app",
                "Sent",
                null,
                true
        );
        activityLogService.log("MEMBER_REGISTERED", memberId, "ROLE_MEMBER", "members", memberId);
        realtimeEventService.broadcastMemberRegistered(memberId, member.getName());

        // Calculate and store age
        memberService.updateMemberAge(memberId);

        return buildAuthResponse(memberId, member.getRole(), member.getEmail(), member.getName());
    }

    // Add MemberService dependency
    private final MemberService memberService;

    public void logout(String userId, String role) {
        activityLogService.log("LOGOUT", userId, role, "auth", userId);
    }

    private void ensureEmailUnique(String email) {
        if (adminRepository.findByEmailIgnoreCase(email).isPresent()
                || staffRepository.findByEmailIgnoreCase(email).isPresent()
                || memberRepository.findByEmailIgnoreCase(email).isPresent()) {
            throw new DuplicateResourceException("Email already exists");
        }
    }

    private String normalizeEmail(String email) {
        if (email == null) return "";
        return email.trim().toLowerCase();
    }

    private void validatePassword(String rawPassword, String passwordHash) {
        if (!passwordEncoder.matches(rawPassword, passwordHash)) {
            throw new UnauthorizedException("Invalid credentials");
        }
    }

    private AuthResponse buildAuthResponse(String userId, String role, String email, String name) {
        return AuthResponse.builder()
                .token(jwtUtil.generateToken(userId, role, email))
                .role(role)
                .userId(userId)
                .name(name)
                .expiresIn(jwtUtil.getExpirationSeconds())
                .build();
    }
}
