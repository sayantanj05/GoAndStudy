package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.UpdateSystemConfigRequest;
import com.goandstudybackend.entity.FineRecord;
import com.goandstudybackend.entity.Loan;
import com.goandstudybackend.entity.SystemConfig;
import com.goandstudybackend.repository.FineRecordRepository;
import com.goandstudybackend.repository.LoanRepository;
import com.goandstudybackend.repository.SystemConfigRepository;
import com.goandstudybackend.repository.MemberRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.time.temporal.ChronoUnit;

@Service
@RequiredArgsConstructor
public class SystemConfigService {

    private final SystemConfigRepository systemConfigRepository;
    private final MemberRepository memberRepository;
    private final LoanRepository loanRepository;
    private final FineRecordRepository fineRecordRepository;
    private static final List<String> ACTIVE_LOAN_STATUSES = List.of("ISSUED", "RENEWED", "Active", "Issued", "Renewed");

    public SystemConfig getById(String id) {
        return systemConfigRepository.findById(id).orElseGet(() -> SystemConfig.builder().id(id).build());
    }

    public Map<String, Object> getMergedConfig() {
        SystemConfig loanSettings = getById("loan_settings");
        SystemConfig aiSettings = getById("ai_settings");
        SystemConfig featureFlags = getById("feature_flags");

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("loanDurationDays", loanSettings.getLoanDurationDays());
        response.put("maxActiveLoans", loanSettings.getMaxActiveLoans());
        response.put("fineRatePerDay", loanSettings.getFineRatePerDay());
        response.put("gracePeriodDays", loanSettings.getGracePeriodDays());
        response.put("aiModelId", aiSettings.getAiModelId());
        response.put("aiMaxTokens", aiSettings.getAiMaxTokens());
        response.put("chatbotEnabled", aiSettings.getChatbotEnabled());
        response.put("chatbotMaxMessagesPerSession", aiSettings.getChatbotMaxMessagesPerSession());
        response.put("chatbotMaxTokens", aiSettings.getChatbotMaxTokens());
        response.put("chatbotContextWindow", aiSettings.getChatbotContextWindow());
        response.put("featureFlags", featureFlags.getFeatureFlags());
        return response;
    }

    public Map<String, Object> updateSystemConfig(UpdateSystemConfigRequest request, String adminId) {
        SystemConfig loanSettings = getById("loan_settings");
        SystemConfig aiSettings = getById("ai_settings");
        SystemConfig featureFlags = getById("feature_flags");

        Map<String, Object> before = getMergedConfig();
        List<String> updatedFields = new ArrayList<>();
        LocalDateTime now = LocalDateTime.now();

        if (request.getLoanDurationDays() != null) {
            loanSettings.setLoanDurationDays(request.getLoanDurationDays());
            updatedFields.add("loanDurationDays");
            memberRepository.findAll().forEach(member -> {
                member.setLoanDurationDays(request.getLoanDurationDays());
                member.setUpdatedAt(now);
                memberRepository.save(member);
            });
            loanRepository.findAll().stream()
                    .filter(loan -> ACTIVE_LOAN_STATUSES.contains(loan.getStatus()))
                    .filter(loan -> loan.getIssuedAt() != null)
                    .forEach(loan -> {
                        loan.setDueDate(loan.getIssuedAt().plusDays(request.getLoanDurationDays()));
                        loan.setUpdatedAt(now);
                        loan.setUpdatedBy(adminId);
                        loanRepository.save(loan);
                    });
        }
        if (request.getMaxActiveLoans() != null) {
            loanSettings.setMaxActiveLoans(request.getMaxActiveLoans());
            updatedFields.add("maxActiveLoans");
            
            // Update existing members to reflect new max active loans
            memberRepository.findAll().forEach(member -> {
                member.setMaxActiveLoans(request.getMaxActiveLoans());
                memberRepository.save(member);
            });
        }
        if (request.getFineRatePerDay() != null) {
            loanSettings.setFineRatePerDay(request.getFineRatePerDay());
            updatedFields.add("fineRatePerDay");
            memberRepository.findAll().forEach(member -> {
                member.setFineRatePerDay(request.getFineRatePerDay());
                member.setUpdatedAt(now);
                memberRepository.save(member);
            });
        }
        if (request.getGracePeriodDays() != null) {
            loanSettings.setGracePeriodDays(request.getGracePeriodDays());
            updatedFields.add("gracePeriodDays");
        }

        if (request.getAiModelId() != null) {
            aiSettings.setAiModelId(request.getAiModelId());
            updatedFields.add("aiModelId");
        }
        if (request.getAiMaxTokens() != null) {
            aiSettings.setAiMaxTokens(request.getAiMaxTokens());
            updatedFields.add("aiMaxTokens");
        }
        if (request.getChatbotEnabled() != null) {
            aiSettings.setChatbotEnabled(request.getChatbotEnabled());
            updatedFields.add("chatbotEnabled");
        }
        if (request.getChatbotMaxMessagesPerSession() != null) {
            aiSettings.setChatbotMaxMessagesPerSession(request.getChatbotMaxMessagesPerSession());
            updatedFields.add("chatbotMaxMessagesPerSession");
        }
        if (request.getChatbotMaxTokens() != null) {
            aiSettings.setChatbotMaxTokens(request.getChatbotMaxTokens());
            updatedFields.add("chatbotMaxTokens");
        }
        if (request.getChatbotContextWindow() != null) {
            aiSettings.setChatbotContextWindow(request.getChatbotContextWindow());
            updatedFields.add("chatbotContextWindow");
        }

        if (request.getFeatureFlags() != null) {
            featureFlags.setFeatureFlags(new LinkedHashMap<>(request.getFeatureFlags()));
            updatedFields.add("featureFlags");
        }

        loanSettings.setUpdatedByAdminId(adminId);
        loanSettings.setUpdatedAt(now);
        aiSettings.setUpdatedByAdminId(adminId);
        aiSettings.setUpdatedAt(now);
        featureFlags.setUpdatedByAdminId(adminId);
        featureFlags.setUpdatedAt(now);

        systemConfigRepository.save(loanSettings);
        systemConfigRepository.save(aiSettings);
        systemConfigRepository.save(featureFlags);

        if (updatedFields.contains("fineRatePerDay") || updatedFields.contains("gracePeriodDays") || updatedFields.contains("loanDurationDays")) {
            refreshOpenFineAmounts(loanSettings, now, adminId);
        }

        Map<String, Object> response = new LinkedHashMap<>();
        response.put("updated", updatedFields);
        response.put("updatedAt", now);
        response.put("before", before);
        response.put("after", getMergedConfig());
        return response;
    }

    private void refreshOpenFineAmounts(SystemConfig loanSettings, LocalDateTime now, String adminId) {
        int gracePeriodDays = Math.max(0, loanSettings.getGracePeriodDays());
        double fineRate = loanSettings.getFineRatePerDay() > 0 ? loanSettings.getFineRatePerDay() : 10.0;
        loanRepository.findAll().stream()
                .filter(loan -> "OVERDUE".equalsIgnoreCase(loan.getStatus()))
                .filter(loan -> loan.getDueDate() != null)
                .forEach(loan -> {
                    int overdueDays = (int) Math.max(0, ChronoUnit.DAYS.between(loan.getDueDate().plusDays(gracePeriodDays), now));
                    double total = overdueDays * fineRate;
                    loan.setOverdueDays(overdueDays);
                    loan.setFineAmount(total);
                    loan.setUpdatedAt(now);
                    loan.setUpdatedBy(adminId);
                    loanRepository.save(loan);

                    fineRecordRepository.findByLoanId(loan.getId()).ifPresent(fine -> {
                        if ("Pending".equalsIgnoreCase(fine.getStatus())) {
                            fine.setOverdueDays(overdueDays);
                            fine.setRatePerDay(fineRate);
                            fine.setTotalAmount(total);
                            fineRecordRepository.save(fine);
                        }
                    });
                });
    }

    public boolean isFeatureEnabled(String featureName) {
        SystemConfig featureFlags = getById("feature_flags");
        return featureFlags.getFeatureFlags().getOrDefault(featureName, false);
    }

    public boolean isChatbotEnabled() {
        return Boolean.TRUE.equals(getById("ai_settings").getChatbotEnabled());
    }

    public int getChatbotMaxMessagesPerSession() {
        Integer value = getById("ai_settings").getChatbotMaxMessagesPerSession();
        return value == null ? 0 : value;
    }

    public int getChatbotMaxTokens() {
        Integer value = getById("ai_settings").getChatbotMaxTokens();
        return value == null ? 0 : value;
    }

    public int getChatbotContextWindow() {
        Integer value = getById("ai_settings").getChatbotContextWindow();
        return value == null ? 0 : value;
    }
}
