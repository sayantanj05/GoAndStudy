package com.goandstudybackend.service;

import com.goandstudybackend.dto.response.ChurnPredictionResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.util.UriComponentsBuilder;

import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class PredictionService {

    private final RestTemplate restTemplate;
    private static final String ML_SERVICE_URL = "http://localhost:8001/api/v1/predict";

    /**
     * Fetch churn prediction for a specific member from ML service.
     */
    public ChurnPredictionResponse getChurnPrediction(String userId) {
        try {
            String url = UriComponentsBuilder
                    .fromHttpUrl(ML_SERVICE_URL + "/churn/" + userId)
                    .toUriString();

            log.info("Calling ML churn prediction: {}", url);
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null) {
                return buildUnknownResponse(userId);
            }

            return ChurnPredictionResponse.builder()
                    .userId((String) response.getOrDefault("user_id", userId))
                    .userName((String) response.getOrDefault("user_name", "Unknown"))
                    .membershipType((String) response.getOrDefault("membership_type", ""))
                    .churnProbability(toDouble(response.get("churn_probability")))
                    .riskLevel((String) response.getOrDefault("risk_level", "UNKNOWN"))
                    .engagementScore(toDouble(response.get("engagement_score")))
                    .totalLoans(toInt(response.get("total_loans")))
                    .daysSinceLastLoan(toInt(response.get("days_since_last_loan")))
                    .topFeatures((List<Map<String, Object>>) response.get("top_features"))
                    .note((String) response.get("note"))
                    .build();

        } catch (Exception e) {
            log.error("ML churn prediction failed for user {}: {}", userId, e.getMessage());
            return buildUnknownResponse(userId);
        }
    }

    /**
     * Fetch churn predictions for all members.
     */
    @SuppressWarnings("unchecked")
    public List<ChurnPredictionResponse> getAllChurnPredictions() {
        try {
            String url = ML_SERVICE_URL + "/churn";
            Map<String, Object> response = restTemplate.getForObject(url, Map.class);

            if (response == null || !response.containsKey("predictions")) {
                return List.of();
            }

            List<Map<String, Object>> predictions = (List<Map<String, Object>>) response.get("predictions");
            return predictions.stream()
                    .map(this::mapToChurnResponse)
                    .toList();

        } catch (Exception e) {
            log.error("ML churn predictions failed: {}", e.getMessage());
            return List.of();
        }
    }

    private ChurnPredictionResponse mapToChurnResponse(Map<String, Object> data) {
        return ChurnPredictionResponse.builder()
                .userId((String) data.getOrDefault("user_id", ""))
                .userName((String) data.getOrDefault("user_name", "Unknown"))
                .membershipType((String) data.getOrDefault("membership_type", ""))
                .churnProbability(toDouble(data.get("churn_probability")))
                .riskLevel((String) data.getOrDefault("risk_level", "UNKNOWN"))
                .engagementScore(toDouble(data.get("engagement_score")))
                .totalLoans(toInt(data.get("total_loans")))
                .daysSinceLastLoan(toInt(data.get("days_since_last_loan")))
                .topFeatures((List<Map<String, Object>>) data.get("top_features"))
                .note((String) data.get("note"))
                .build();
    }

    private ChurnPredictionResponse buildUnknownResponse(String userId) {
        return ChurnPredictionResponse.builder()
                .userId(userId)
                .userName("Unknown")
                .churnProbability(0.0)
                .riskLevel("UNKNOWN")
                .build();
    }

    private double toDouble(Object value) {
        if (value == null) return 0.0;
        if (value instanceof Number) return ((Number) value).doubleValue();
        try {
            return Double.parseDouble(value.toString());
        } catch (NumberFormatException e) {
            return 0.0;
        }
    }

    private int toInt(Object value) {
        if (value == null) return 0;
        if (value instanceof Number) return ((Number) value).intValue();
        try {
            return Integer.parseInt(value.toString());
        } catch (NumberFormatException e) {
            return 0;
        }
    }
}
