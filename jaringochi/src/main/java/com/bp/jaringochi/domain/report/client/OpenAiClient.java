package com.bp.jaringochi.domain.report.client;

import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Component;
import org.springframework.util.StringUtils;
import org.springframework.web.client.RestClient;

import com.fasterxml.jackson.databind.JsonNode;

/**
 * OpenAI Chat Completions 호출 헬퍼.
 * 키(openai.api.key)는 환경변수 OPENAI_API_KEY 로 주입한다. 키가 없거나 호출이 실패하면
 * null 을 돌려주고, 호출 측(ReportService)이 안전한 폴백으로 처리한다.
 */
@Component
public class OpenAiClient {

    private final RestClient restClient;
    private final String apiKey;
    private final String model;

    public OpenAiClient(@Value("${openai.api.key:}") String apiKey,
                        @Value("${openai.api.base-url:https://api.openai.com/v1}") String baseUrl,
                        @Value("${openai.model:gpt-4o-mini}") String model) {
        this.apiKey = apiKey;
        this.model = model;
        this.restClient = RestClient.builder()
                .baseUrl(baseUrl)
                .build();
    }

    public boolean isConfigured() {
        return StringUtils.hasText(apiKey);
    }

    /**
     * 시스템/유저 프롬프트로 1회 응답을 받는다.
     * @param jsonMode true면 response_format=json_object 로 JSON 강제
     * @return 모델이 생성한 content 문자열, 실패 시 null
     */
    public String chat(String systemPrompt, String userPrompt, boolean jsonMode) {
        if (!isConfigured()) {
            return null;
        }

        Map<String, Object> body = new java.util.HashMap<>();
        body.put("model", model);
        body.put("temperature", 0.7);
        body.put("messages", List.of(
                Map.of("role", "system", "content", systemPrompt),
                Map.of("role", "user", "content", userPrompt)
        ));
        if (jsonMode) {
            body.put("response_format", Map.of("type", "json_object"));
        }

        try {
            JsonNode res = restClient.post()
                    .uri("/chat/completions")
                    .header("Authorization", "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(JsonNode.class);

            if (res == null) {
                return null;
            }
            JsonNode content = res.path("choices").path(0).path("message").path("content");
            return content.isMissingNode() ? null : content.asText();
        } catch (Exception e) {
            // 키 오류·네트워크·레이트리밋 등 모든 실패는 폴백으로 흡수
            return null;
        }
    }
}
