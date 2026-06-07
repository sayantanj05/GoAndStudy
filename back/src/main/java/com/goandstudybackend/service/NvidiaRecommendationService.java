package com.goandstudybackend.service;

import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.goandstudybackend.dto.response.AiRecommendationResponse;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpEntity;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NvidiaRecommendationService {

    private final RestTemplate restTemplate;
    private final ObjectMapper objectMapper;

    @Value("${nvidia.nim.api.key}")
    private String apiKey;

    @Value("${nvidia.nim.base.url}")
    private String baseUrl;

    @Value("${nvidia.nim.recommendation.model}")
    private String recommendationModel;

    @Value("${nvidia.nim.llm.max.tokens}")
    private int maxTokens;

    public RecommendationPayload recommend(String prompt) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", recommendationModel);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("max_tokens", maxTokens);

            Map<?, ?> response = restTemplate.postForObject(
                    baseUrl + "/chat/completions",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            String text = "";
            int tokensUsed = 0;
            if (response != null && response.get("choices") instanceof List<?> choices && !choices.isEmpty()) {
                Map<?, ?> first = (Map<?, ?>) choices.getFirst();
                Map<?, ?> message = (Map<?, ?>) first.get("message");
                Object content = message == null ? null : message.get("content");
                text = content == null ? "" : String.valueOf(content);
            }
            if (response != null && response.get("usage") instanceof Map<?, ?> usage) {
                tokensUsed = toInt(usage.get("prompt_tokens")) + toInt(usage.get("completion_tokens"));
            }

            List<AiRecommendationResponse.RecommendationItem> parsedRecommendations = parseRecommendations(text);
            return RecommendationPayload.builder()
                    .recommendations(parsedRecommendations)
                    .rawText(text)
                    .tokensUsed(tokensUsed)
                    .latencyMs((int) (System.currentTimeMillis() - start))
                    .build();
        } catch (Exception exception) {
            log.warn("Recommendation request failed: {}", exception.getMessage());
            return RecommendationPayload.builder()
                    .recommendations(new ArrayList<>())
                    .rawText("")
                    .tokensUsed(0)
                    .latencyMs((int) (System.currentTimeMillis() - start))
                    .build();
        }
    }

    private List<AiRecommendationResponse.RecommendationItem> parseRecommendations(String text) {
        String normalizedPayload = normalizeJsonPayload(text);
        if (normalizedPayload.isBlank()) {
            return new ArrayList<>();
        }
        try {
            JsonNode root = objectMapper.readTree(normalizedPayload);
            JsonNode itemsNode = extractRecommendationArray(root);
            if (itemsNode == null || !itemsNode.isArray()) {
                return new ArrayList<>();
            }

            List<Map<String, Object>> items = objectMapper.convertValue(itemsNode, new TypeReference<>() {});
            List<AiRecommendationResponse.RecommendationItem> results = new ArrayList<>();
            for (Map<String, Object> item : items) {
                results.add(AiRecommendationResponse.RecommendationItem.builder()
                        .bookId(stringValue(item.get("bookId")))
                        .title(stringValue(item.get("title")))
                        .author(stringValue(item.get("author")))
                        .coverImageUrl(stringValue(item.get("coverImageUrl")))
                        .matchScore(numberValue(item.get("matchScore")))
                        .reason(stringValue(item.get("reason")))
                        .genre(stringValue(item.get("genre")))
                        .availableCopies((int) numberValue(item.get("availableCopies")))
                        .build());
            }
            return results;
        } catch (Exception exception) {
            log.warn("Failed to parse recommendation payload: {}", exception.getMessage());
            return new ArrayList<>();
        }
    }

    private JsonNode extractRecommendationArray(JsonNode root) {
        if (root == null) {
            return null;
        }
        if (root.isArray()) {
            return root;
        }
        if (root.isObject()) {
            if (root.has("books") && root.get("books").isArray()) {
                return root.get("books");
            }
            if (root.has("recommendations") && root.get("recommendations").isArray()) {
                return root.get("recommendations");
            }
        }
        return null;
    }

    private String normalizeJsonPayload(String text) {
        if (text == null) {
            return "";
        }
        String normalized = text.trim();
        if (normalized.startsWith("```")) {
            normalized = normalized.replaceFirst("^```(?:json)?\\s*", "");
            normalized = normalized.replaceFirst("\\s*```$", "");
        }

        int arrayStart = normalized.indexOf('[');
        int objectStart = normalized.indexOf('{');
        int start = firstNonNegative(arrayStart, objectStart);
        if (start < 0) {
            return normalized;
        }

        char startChar = normalized.charAt(start);
        int end = startChar == '[' ? normalized.lastIndexOf(']') : normalized.lastIndexOf('}');
        if (end >= start) {
            return normalized.substring(start, end + 1).trim();
        }
        return normalized.substring(start).trim();
    }

    private int firstNonNegative(int left, int right) {
        if (left < 0) {
            return right;
        }
        if (right < 0) {
            return left;
        }
        return Math.min(left, right);
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private double numberValue(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    private String stringValue(Object value) {
        return value == null ? "" : String.valueOf(value);
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class RecommendationPayload {
        private List<AiRecommendationResponse.RecommendationItem> recommendations;
        private String rawText;
        private int tokensUsed;
        private int latencyMs;
    }
}
