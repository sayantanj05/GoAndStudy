package com.goandstudybackend.service;

import com.goandstudybackend.dto.request.ChangePasswordRequest;
import com.goandstudybackend.dto.request.UpdateProfileRequest;
import com.goandstudybackend.entity.BookCategory;
import com.goandstudybackend.entity.Member;
import com.goandstudybackend.entity.MemberAnalytics;
import com.goandstudybackend.exception.ResourceNotFoundException;
import com.goandstudybackend.repository.BookCategoryRepository;
import com.goandstudybackend.repository.MemberAnalyticsRepository;
import com.goandstudybackend.repository.MemberRepository;
import com.goandstudybackend.repository.NotificationRepository;
import com.goandstudybackend.repository.WishlistRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.Period;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Service
@RequiredArgsConstructor
public class MemberService {

    private final MemberRepository memberRepository;
    private final NotificationRepository notificationRepository;
    private final MemberAnalyticsRepository memberAnalyticsRepository;
    private final WishlistRepository wishlistRepository;
    private final PasswordEncoder passwordEncoder;
    private final MemberAnalyticsService memberAnalyticsService;
    private final BookCategoryRepository bookCategoryRepository;

public Map<String, Object> getMyProfile(String memberId) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        Map<String, Object> response = new LinkedHashMap<>();
        response.put("memberId", memberId);
        response.put("name", member.getName());
        response.put("email", member.getEmail());
        response.put("phone", member.getPhone());
        response.put("membershipType", member.getMembershipType());
        response.put("gender", member.getGender());
        response.put("dateOfBirth", member.getDateOfBirth());
        response.put("timeCreated", member.getTimeCreated());
        response.put("registrationDate", member.getRegistrationDate());
        
        // Calculate or get age
        Integer age = null;
        if (member.getDateOfBirth() != null) {
            age = Period.between(member.getDateOfBirth(), LocalDate.now()).getYears();
        } else {
            // Fallback to MemberAnalytics if available
            MemberAnalytics analytics = memberAnalyticsRepository.findById(memberId).orElse(null);
            if (analytics != null) {
                age = analytics.getAge();
            }
        }
        response.put("age", age);
        
        // Include analytics summary
        Map<String, Object> analytics = new LinkedHashMap<>();
        analytics.put("age", age);
        analytics.put("totalReturned", member.getTotalLoans());
        analytics.put("activeLoans", member.getActiveLoanCount());
        response.put("analytics", analytics);
        
        return response;
    }

public Map<String, Object> updateProfile(String memberId, UpdateProfileRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        if (request.getName() != null) member.setName(request.getName());
        if (request.getPhone() != null) member.setPhone(request.getPhone());
        // Update dateOfBirth and recalculate age
        if (request.getDateOfBirth() != null) {
            member.setDateOfBirth(request.getDateOfBirth());
            updateMemberAge(memberId); // Recalculate age when DOB changes
        }
        LocalDateTime now = LocalDateTime.now();
        member.setUpdatedAt(now);
        member.setUpdatedBy(memberId);
        memberRepository.save(member);
        return Map.of("message", "Profile updated successfully");
    }

    public Map<String, Object> changePassword(String memberId, ChangePasswordRequest request) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        LocalDateTime now = LocalDateTime.now();
        member.setPasswordHash(passwordEncoder.encode(request.getNewPassword()));
        member.setPlainPassword(request.getNewPassword());
        member.setUpdatedAt(now);
        member.setUpdatedBy(memberId);
        memberRepository.save(member);
        return Map.of("message", "Password updated");
    }

    public String getMemberName(String memberId) {
        return memberRepository.findById(memberId).map(Member::getName).orElse("Unknown");
    }

    public Map<String, Object> getUnreadCount(String memberId) {
        long count = notificationRepository.countByMemberIdAndStatus(memberId, "Unread");
        return Map.of("count", count);
    }

    public void updateMemberAge(String memberId) {
        Member member = memberRepository.findById(memberId).orElse(null);
        if (member != null && member.getDateOfBirth() != null) {
            int age = Period.between(member.getDateOfBirth(), LocalDate.now()).getYears();
            MemberAnalytics analytics = memberAnalyticsRepository.findById(memberId).orElse(
                    MemberAnalytics.builder().id(memberId).build()
            );
            analytics.setAge(age);
            memberAnalyticsRepository.save(analytics);
        }
    }

    public Map<String, Object> getMyAnalytics(String memberId) {
        // Get or compute analytics
        MemberAnalytics analytics = memberAnalyticsRepository.findById(memberId).orElse(null);
        if (analytics == null || analytics.getComputedAt() == null ||
            analytics.getComputedAt().isBefore(LocalDateTime.now().minusHours(1))) {
            // Recompute if not exists or stale (> 1 hour)
            analytics = memberAnalyticsService.recomputeForMember(memberId);
        }

        Member member = memberRepository.findById(memberId).orElse(null);

        Map<String, Object> response = new LinkedHashMap<>();

        // Inference Insights metrics
        int booksRead = analytics != null ? analytics.getTotalReturned() : 0;
        double avgLoanDuration = analytics != null ? analytics.getAvgLoanDuration() : 0.0;
        int knowledgeNodes = analytics != null && analytics.getGenreBreakdown() != null
                ? analytics.getGenreBreakdown().size() : 0;

        // Calculate reading velocity based on issue and return frequency
        // Formula: (books per month) / (average loan duration in months), min 1.0x
        double readingVelocity;
        if (booksRead > 0 && avgLoanDuration > 0) {
            double avgLoanDurationInMonths = avgLoanDuration / 30.0;
            double booksPerMonth = booksRead / Math.max(1.0, avgLoanDurationInMonths * booksRead / 12.0);
            readingVelocity = Math.max(1.0, booksPerMonth);
        } else {
            readingVelocity = 1.0;
        }

        response.put("booksRead", booksRead);
        response.put("readingVelocity", String.format("%.1fx", readingVelocity));
        response.put("knowledgeNodes", knowledgeNodes);
        response.put("totalSearches", analytics != null ? analytics.getTotalSearches() : 0);
        response.put("totalAiCalls", analytics != null ? analytics.getTotalAiCalls() : 0);
        response.put("aiConversionRate", analytics != null ? analytics.getAiConversionRate() : 0.0);
        response.put("mostActiveMonth", analytics != null ? analytics.getMostActiveMonth() : "");
        response.put("avgLoanDuration", analytics != null ? Math.round(avgLoanDuration) : 0);

        // Psychographic Interest Tree (All categories with member's reading percentages)
        List<Map<String, Object>> memberGenreBreakdown = analytics != null ? analytics.getGenreBreakdown() : List.of();
        
        // Get all categories from database
        List<BookCategory> allCategories = bookCategoryRepository.findAll();
        
        // Create a map of member's genre counts for quick lookup
        Map<String, Integer> memberGenreCounts = new HashMap<>();
        if (memberGenreBreakdown != null) {
            for (Map<String, Object> genre : memberGenreBreakdown) {
                String genreName = String.valueOf(genre.getOrDefault("genre", genre.getOrDefault("category", "")));
                int count = (Integer) genre.getOrDefault("count", 0);
                memberGenreCounts.put(genreName, count);
            }
        }
        
        // Calculate total books read by member
        int totalBooksRead = memberGenreCounts.values().stream().mapToInt(Integer::intValue).sum();
        
        // Create genre tree with ALL categories and their percentages
        List<Map<String, Object>> genreTree = allCategories.stream()
                .map(category -> {
                    String categoryName = category.getName();
                    int count = memberGenreCounts.getOrDefault(categoryName, 0);
                    int percentage = totalBooksRead > 0 ? (count * 100 / totalBooksRead) : 0;
                    
                    return Map.<String, Object>of(
                            "category", categoryName,
                            "count", count,
                            "percentage", percentage
                    );
                })
                .sorted((a, b) -> Integer.compare(
                        (Integer) ((Map<String, Object>) b).get("percentage"),
                        (Integer) ((Map<String, Object>) a).get("percentage")))
                .toList();
                
        response.put("genreTree", genreTree);

        // Neural Controls - recommendation preferences (stored in member or analytics)
        Map<String, Object> neuralControls = new LinkedHashMap<>();
        neuralControls.put("inferenceLevel", member != null && member.getInferenceLevel() != null
                ? member.getInferenceLevel() : "Exploratory");
        neuralControls.put("privacyProjection", member != null && member.getPrivacyProjection() != null
                ? member.getPrivacyProjection() : "Standard");
        neuralControls.put("obfuscationMode", member != null && Boolean.TRUE.equals(member.getObfuscationMode()));
        response.put("neuralControls", neuralControls);

        return response;
    }

    public Map<String, Object> updateNeuralControls(String memberId, String inferenceLevel, String privacyProjection, Boolean obfuscationMode) {
        Member member = memberRepository.findById(memberId).orElseThrow(() -> new ResourceNotFoundException("Member not found"));
        
        if (inferenceLevel != null) {
            member.setInferenceLevel(inferenceLevel);
        }
        if (privacyProjection != null) {
            member.setPrivacyProjection(privacyProjection);
        }
        if (obfuscationMode != null) {
            member.setObfuscationMode(obfuscationMode);
        }
        
        member.setUpdatedAt(LocalDateTime.now());
        member.setUpdatedBy(memberId);
        memberRepository.save(member);
        
        return Map.of(
                "inferenceLevel", member.getInferenceLevel(),
                "privacyProjection", member.getPrivacyProjection(),
                "obfuscationMode", member.getObfuscationMode()
        );
    }
}
