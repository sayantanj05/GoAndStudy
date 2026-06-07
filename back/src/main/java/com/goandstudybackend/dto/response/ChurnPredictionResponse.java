package com.goandstudybackend.dto.response;

import lombok.Builder;
import lombok.Data;
import java.util.List;
import java.util.Map;

@Data
@Builder
public class ChurnPredictionResponse {
    private String userId;
    private String userName;
    private String membershipType;
    private double churnProbability;
    private String riskLevel;
    private double engagementScore;
    private int totalLoans;
    private int daysSinceLastLoan;
    private List<Map<String, Object>> topFeatures;
    private String note;
}
