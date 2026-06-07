package com.goandstudybackend.jobs;

import com.goandstudybackend.service.MemberAnalyticsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class MemberAnalyticsJob {

    private final MemberAnalyticsService memberAnalyticsService;

    @Scheduled(cron = "0 0 2 * * *")
    public void recomputeMemberAnalytics() {
        memberAnalyticsService.recomputeAllMembers();
        log.info("Member analytics recomputed");
    }
}
