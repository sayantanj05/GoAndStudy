package com.goandstudybackend.controller;

import com.goandstudybackend.dto.response.ApiResponse;
import com.goandstudybackend.service.AnalyticsEngineerService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@RestController
@RequestMapping("/api/v1/analytics-engineer")
@RequiredArgsConstructor
@Tag(name = "Analytics Engineer", description = "Analytics Engineer portal APIs")
public class AnalyticsEngineerController {

    private final AnalyticsEngineerService analyticsEngineerService;

    @GetMapping("/dashboard")
    @Operation(summary = "Get analytics engineer dashboard data")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getDashboard(Authentication authentication) {
        Map<String, Object> dashboard = analyticsEngineerService.getDashboardData();
        return ResponseEntity.ok(ApiResponse.success("Dashboard data retrieved successfully", dashboard));
    }

    @GetMapping("/member-metrics")
    @Operation(summary = "Get member analytics summary")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getMemberAnalytics(Authentication authentication) {
        Map<String, Object> metrics = analyticsEngineerService.getMemberAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Member analytics retrieved successfully", metrics));
    }

    @GetMapping("/genre-metrics")
    @Operation(summary = "Get genre analytics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getGenreAnalytics(Authentication authentication) {
        Map<String, Object> metrics = analyticsEngineerService.getGenreAnalytics();
        return ResponseEntity.ok(ApiResponse.success("Genre analytics retrieved successfully", metrics));
    }

    @GetMapping("/ai-usage")
    @Operation(summary = "Get AI usage statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getAiUsage(
            @RequestParam(defaultValue = "7") int days,
            Authentication authentication) {
        Map<String, Object> usage = analyticsEngineerService.getAiUsage(days);
        return ResponseEntity.ok(ApiResponse.success("AI usage data retrieved successfully", usage));
    }

    @PostMapping("/etl/sync")
    @Operation(summary = "Trigger batch sync")
    public ResponseEntity<ApiResponse<String>> triggerBatchSync(
            @RequestBody Map<String, Object> payload,
            Authentication authentication) {
        analyticsEngineerService.triggerBatchSync(payload);
        return ResponseEntity.ok(ApiResponse.success("Sync operation started", "Batch sync triggered"));
    }

    @PostMapping("/etl/change-stream")
    @Operation(summary = "Toggle change stream")
    public ResponseEntity<ApiResponse<String>> toggleChangeStream(
            @RequestBody Map<String, Boolean> request,
            Authentication authentication) {
        boolean enabled = request.get("enabled");
        analyticsEngineerService.toggleChangeStream(enabled);
        return ResponseEntity.ok(ApiResponse.success("Change stream updated", 
            "Change stream is now " + (enabled ? "enabled" : "disabled")));
    }

    @GetMapping("/recommendations/impersonate")
    @Operation(summary = "Impersonate user for recommendation testing")
    public ResponseEntity<ApiResponse<Map<String, Object>>> impersonateUser(
            @RequestParam String userId,
            Authentication authentication) {
        Map<String, Object> recommendations = analyticsEngineerService.impersonateUser(userId);
        return ResponseEntity.ok(ApiResponse.success("User recommendations retrieved", recommendations));
    }

    @PostMapping("/pipeline/start")
    @Operation(summary = "Start data pipeline")
    public ResponseEntity<ApiResponse<String>> startPipeline(Authentication authentication) {
        analyticsEngineerService.startPipeline();
        return ResponseEntity.ok(ApiResponse.success("Data pipeline is now running", "Pipeline started"));
    }

    @PostMapping("/pipeline/stop")
    @Operation(summary = "Stop data pipeline")
    public ResponseEntity<ApiResponse<String>> stopPipeline(Authentication authentication) {
        analyticsEngineerService.stopPipeline();
        return ResponseEntity.ok(ApiResponse.success("Data pipeline has been stopped", "Pipeline stopped"));
    }

    @PostMapping("/etl/trigger-sync")
    @Operation(summary = "Trigger manual sync")
    public ResponseEntity<ApiResponse<String>> triggerSync(
            @RequestParam(defaultValue = "false") boolean force,
            Authentication authentication) {
        analyticsEngineerService.triggerSync(force);
        return ResponseEntity.ok(ApiResponse.success("Manual sync has been initiated", "Sync triggered"));
    }

    @GetMapping("/reports")
    @Operation(summary = "Get analytics reports")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getReports(
            @RequestParam(defaultValue = "daily") String period,
            Authentication authentication) {
        Map<String, Object> reports = analyticsEngineerService.getReports(period);
        return ResponseEntity.ok(ApiResponse.success("Reports retrieved successfully", reports));
    }

    @GetMapping("/profile")
    @Operation(summary = "Get analytics engineer profile")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfile(Authentication authentication) {
        Map<String, Object> profile = analyticsEngineerService.getProfile(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile retrieved successfully", profile));
    }

    @PutMapping("/profile")
    @Operation(summary = "Update analytics engineer profile")
    public ResponseEntity<ApiResponse<String>> updateProfile(
            @RequestBody Map<String, Object> profileData,
            Authentication authentication) {
        analyticsEngineerService.updateProfile(authentication.getName(), profileData);
        return ResponseEntity.ok(ApiResponse.success("Your profile has been updated successfully", "Profile updated"));
    }

    @GetMapping("/profile/stats")
    @Operation(summary = "Get analytics engineer profile statistics")
    public ResponseEntity<ApiResponse<Map<String, Object>>> getProfileStats(Authentication authentication) {
        Map<String, Object> stats = analyticsEngineerService.getProfileStats(authentication.getName());
        return ResponseEntity.ok(ApiResponse.success("Profile statistics retrieved successfully", stats));
    }
}
