package com.goandstudybackend.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class DashboardResponse {

    private long totalMembers;
    private long totalBooks;
    private long activeLoans;
    private long overdueLoans;
    @Builder.Default
    private List<Map<String, Object>> loansThisWeek = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> topGenres = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> recentActivity = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> membershipBreakdown = new ArrayList<>();
    @Builder.Default
    private List<Map<String, Object>> fineCollectionTrend = new ArrayList<>();
    @Builder.Default
    private Map<String, Object> aiUsage = new LinkedHashMap<>();
}
