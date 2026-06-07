package com.goandstudybackend.service;

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
public class NvidiaEmbeddingService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.nim.api.key}")
    private String apiKey;

    @Value("${nvidia.nim.base.url}")
    private String baseUrl;

    @Value("${nvidia.nim.embedding.model}")
    private String embeddingModel;

    public List<Double> generateEmbedding(String text) {
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", embeddingModel);
            body.put("input", text);
            body.put("input_type", "passage");
            body.put("encoding_format", "float");

            Map<?, ?> response = restTemplate.postForObject(
                    baseUrl + "/embeddings",
                    new HttpEntity<>(body, headers),
                    Map.class
            );
            if (response == null || response.get("data") == null) {
                return new ArrayList<>();
            }

            List<?> data = (List<?>) response.get("data");
            if (data.isEmpty()) {
                return new ArrayList<>();
            }
            Map<?, ?> item = (Map<?, ?>) data.getFirst();
            List<?> embedding = (List<?>) item.get("embedding");
            List<Double> result = new ArrayList<>();
            for (Object value : embedding) {
                if (value instanceof Number number) {
                    result.add(number.doubleValue());
                }
            }
            return result;
        } catch (Exception e) {
            System.err.println("Error generating embedding: " + e.getMessage());
            return new ArrayList<>();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }
}
