package com.goandstudybackend.jobs;

import com.goandstudybackend.entity.Member;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.service.NotificationService;
import com.goandstudybackend.util.MembershipType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;

@Slf4j
@Component
@RequiredArgsConstructor
public class MembershipExpiryJob {

    private final MemberRepository memberRepository;
    private final NotificationService notificationService;

    @Scheduled(cron = "0 0 2 * * *")
    public void expireMemberships() {
        LocalDateTime now = LocalDateTime.now();
        for (Member member : memberRepository.findAll()) {
            if (member.getMembershipExpiryDate() != null
                    && member.getMembershipExpiryDate().isBefore(now)
                    && !"BASIC".equalsIgnoreCase(member.getMembershipType())) {
                String previousType = member.getMembershipType();
                member.setMembershipType(MembershipType.BASIC.name());
                member.setMaxActiveLoans(MembershipType.BASIC.maxActiveLoans);
                member.setLoanDurationDays(MembershipType.BASIC.loanDurationDays);
                member.setFineRatePerDay(MembershipType.BASIC.fineRatePerDay);
                member.setRenewalLimit(MembershipType.BASIC.renewalLimit);
                member.setMembershipExpiryDate(null);
                member.setMembershipStartDate(now);
                member.setMembershipFeePaid(0.0);
                member.setUpdatedAt(now);
                memberRepository.save(member);

                notificationService.createNotification(
                        member.getId(),
                        "GENERAL",
                        "Membership expired",
                        "Your " + previousType + " membership has expired. You have been moved to Basic. Renew at the library counter.",
                        null,
                        null,
                        "app",
                        "Sent",
                        null,
                        true
                );
            }
        }
        log.info("Membership expiry job completed");
    }
}
