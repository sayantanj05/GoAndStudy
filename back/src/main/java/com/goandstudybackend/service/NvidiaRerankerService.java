package com.goandstudybackend.service;

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
public class NvidiaRerankerService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.nim.api.key}")
    private String apiKey;

    @Value("${nvidia.nim.base.url}")
    private String baseUrl;

    @Value("${nvidia.nim.reranker.model}")
    private String rerankerModel;

    public List<BookRerankedResult> rerank(String query, List<String> passages) {
        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setContentType(MediaType.APPLICATION_JSON);
            headers.setBearerAuth(apiKey);

            List<Map<String, String>> rerankPassages = new ArrayList<>();
            for (String passage : passages) {
                rerankPassages.add(Map.of("text", passage));
            }

            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", rerankerModel);
            body.put("query", Map.of("text", query));
            body.put("passages", rerankPassages);

            Map<?, ?> response = restTemplate.postForObject(
                    baseUrl + "/ranking",
                    new HttpEntity<>(body, headers),
                    Map.class
            );

            List<BookRerankedResult> results = new ArrayList<>();
            if (response != null && response.get("rankings") instanceof List<?> rankings) {
                for (Object item : rankings) {
                    Map<?, ?> ranking = (Map<?, ?>) item;
                    results.add(BookRerankedResult.builder()
                            .index(toInt(ranking.get("index")))
                            .score(toDouble(ranking.get("score")))
                            .text(passages.get(Math.min(passages.size() - 1, toInt(ranking.get("index")))))
                            .build());
                }
            }
            if (results.isEmpty()) {
                for (int i = 0; i < passages.size(); i++) {
                    results.add(BookRerankedResult.builder().index(i).score(0.0).text(passages.get(i)).build());
                }
            }
            results.sort((left, right) -> Double.compare(right.getScore(), left.getScore()));
            return results;
        } catch (Exception e) {
            System.err.println("Error reranking: " + e.getMessage());
            List<BookRerankedResult> fallback = new ArrayList<>();
            for (int i = 0; i < passages.size(); i++) {
                fallback.add(BookRerankedResult.builder().index(i).score(0.0).text(passages.get(i)).build());
            }
            return fallback;
        }
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    private double toDouble(Object value) {
        return value instanceof Number number ? number.doubleValue() : 0.0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class BookRerankedResult {
        private int index;
        private double score;
        private String text;
    }
}
