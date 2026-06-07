package com.goandstudybackend.seed;

import com.goandstudybackend.entity.Admin;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.Staff;
import com.goandstudybackend.repository.AdminRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.StaffRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Component
@RequiredArgsConstructor
public class AuthSeeder implements CommandLineRunner {

    private static final Logger log = LoggerFactory.getLogger(AuthSeeder.class);
    
    private final AdminRepository adminRepository;
    private final StaffRepository staffRepository;
    private final MemberRepository memberRepository;
    private final PasswordEncoder passwordEncoder;

    @Override
    public void run(String... args) throws Exception {
        seedDemoUsers();
    }

    private void seedDemoUsers() {
        if (adminRepository.count() == 0) {
            Admin admin = Admin.builder()
                .id("AD001")
                .username("admin")
                .email("admin@gmail.com")
                .passwordHash(passwordEncoder.encode("admin123"))
                .plainPassword("admin123")
                .role("ROLE_ADMIN")
                .isActive(true)
                .lastLogin(LocalDateTime.now())
                .loginCount(0)
                .build();
            adminRepository.save(admin);
            log.info("Seeded demo admin: {} with id {}", admin.getEmail(), admin.getId());
        }

        // Seed demo Staff users (including Analytics Engineer)
        if (staffRepository.count() == 0) {
            Staff staff = Staff.builder()
                .id("STAFF001")
                .name("Staff User")
                .email("staff0001@gmail.com")
                .phone("9876543210")
                .passwordHash(passwordEncoder.encode("staff001"))
                .plainPassword("staff001")
                .role("ROLE_STAFF")
                .isActive(true)
                .lastLogin(LocalDateTime.now())
                .loginCount(0)
                .build();
            staffRepository.save(staff);
            log.info("Seeded demo staff: {}", staff.getEmail());
        }

        // Analytics Engineer account. Keep this as an upsert so credential fixes
        // reach existing databases and do not depend on a clean seed.
        Staff analyticsEngineer = staffRepository.findByEmailIgnoreCase("analytics1@gmail.com")
            .or(() -> staffRepository.findByEmailIgnoreCase("analytics@gmail.com"))
            .or(() -> staffRepository.findByEmailIgnoreCase("analytics@gamil.com"))
            .orElseGet(() -> Staff.builder()
                .id(staffRepository.findById("STAFF002").isPresent() ? "STAFF_ANALYTICS" : "STAFF002")
                .name("Analytics Engineer")
                .phone("9876543210")
                .loginCount(0)
                .build());

        analyticsEngineer.setEmail("analytics1@gmail.com");
        analyticsEngineer.setName(analyticsEngineer.getName() == null || analyticsEngineer.getName().isBlank()
                ? "Analytics Engineer"
                : analyticsEngineer.getName());
analyticsEngineer.setPasswordHash(passwordEncoder.encode("analytics1234"));
        analyticsEngineer.setPlainPassword("analytics1234");
        analyticsEngineer.setRole("ROLE_ANALYTICS_ENGINEER");
        analyticsEngineer.setActive(true);
        analyticsEngineer.setUpdatedAt(LocalDateTime.now());
        staffRepository.save(analyticsEngineer);
        log.info("Upserted analytics engineer staff: {}", analyticsEngineer.getEmail());

        if (memberRepository.count() == 0) {
            Member member = Member.builder()
                .id("MEMBER001")
                .name("Demo Member")
                .email("member@gmail.com")
                .phone("9876543210")
                .passwordHash(passwordEncoder.encode("member123"))
                .plainPassword("member123")
                .role("ROLE_MEMBER")
                .isActive(true)
                .registrationDate("2026")
                .dailySequence(1)
                .timeCreated(LocalDateTime.now())
                .loginCount(0)
                .activeLoanCount(0)
                .totalLoans(0)
                .membershipType("BASIC")
                .maxActiveLoans(2)
                .loanDurationDays(14)
                .fineRatePerDay(10.0)
                .renewalLimit(1)
                .build();
            memberRepository.save(member);
            log.info("Seeded demo member: {}", member.getEmail());
        }
    }
}
