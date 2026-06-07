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

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
public class NvidiaLlmService {

    private final RestTemplate restTemplate = new RestTemplate();

    @Value("${nvidia.nim.api.key}")
    private String apiKey;

    @Value("${nvidia.nim.base.url}")
    private String baseUrl;

    @Value("${nvidia.nim.llm.model}")
    private String llmModel;

    @Value("${nvidia.nim.llm.max.tokens}")
    private int maxTokens;

    public String generate(String prompt) {
        return generateDetailed(prompt, maxTokens).getText();
    }

    public TextGenerationResult generateDetailed(String prompt, int requestedMaxTokens) {
        long start = System.currentTimeMillis();
        try {
            HttpHeaders headers = buildHeaders();
            Map<String, Object> body = new LinkedHashMap<>();
            body.put("model", llmModel);
            body.put("messages", List.of(Map.of("role", "user", "content", prompt)));
            body.put("max_tokens", requestedMaxTokens);

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

            return TextGenerationResult.builder()
                    .text(text == null ? "" : text.trim())
                    .tokensUsed(tokensUsed)
                    .latencyMs((int) (System.currentTimeMillis() - start))
                    .build();
} catch (Exception e) {
            log.warn("Error generating text: {}", e.getMessage());
            return TextGenerationResult.builder()
                    .text("")
                    .tokensUsed(0)
                    .latencyMs((int) (System.currentTimeMillis() - start))
                    .build();
        }
    }

    private HttpHeaders buildHeaders() {
        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setBearerAuth(apiKey);
        return headers;
    }

    private int toInt(Object value) {
        return value instanceof Number number ? number.intValue() : 0;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class TextGenerationResult {
        private String text;
        private int tokensUsed;
        private int latencyMs;
        
        // Manual builder pattern to fix compilation issues
        public static TextGenerationResultBuilder builder() {
            return new TextGenerationResultBuilder();
        }
        
        public static class TextGenerationResultBuilder {
            private String text;
            private int tokensUsed;
            private int latencyMs;
            
            public TextGenerationResultBuilder text(String text) {
                this.text = text;
                return this;
            }
            
            public TextGenerationResultBuilder tokensUsed(int tokensUsed) {
                this.tokensUsed = tokensUsed;
                return this;
            }
            
            public TextGenerationResultBuilder latencyMs(int latencyMs) {
                this.latencyMs = latencyMs;
                return this;
            }
            
            public TextGenerationResult build() {
                TextGenerationResult result = new TextGenerationResult();
                result.text = this.text;
                result.tokensUsed = this.tokensUsed;
                result.latencyMs = this.latencyMs;
                return result;
            }
        }
        
        public String getText() {
            return text;
        }
        
        public void setText(String text) {
            this.text = text;
        }
    }
}
