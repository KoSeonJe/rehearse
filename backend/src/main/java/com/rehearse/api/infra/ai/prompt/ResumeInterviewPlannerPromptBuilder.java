package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeInterviewPlannerPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/resume/resume-interview-planner.txt";
    private static final String MODEL_OVERRIDE = "gpt-4o-mini";
    private static final double TEMPERATURE = 0.3;
    private static final int MAX_TOKENS = 2048;

    private String userPromptTemplate;

    @PostConstruct
    void init() {
        try (InputStream stream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 파일을 찾을 수 없습니다.");
            }
            userPromptTemplate = new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 로드 실패", e);
        }
        log.info("Resume Interview Planner 프롬프트 템플릿 로드 완료");
    }

    public ChatRequest build(String skeletonJson, int durationMin, String userLevel, String callType) {
        String userMessage = userPromptTemplate
                .replace("{{SKELETON_JSON}}", skeletonJson != null ? skeletonJson : "{}")
                .replace("{{DURATION_MIN}}", String.valueOf(durationMin))
                .replace("{{USER_LEVEL}}", userLevel != null ? userLevel : "MID");

        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, userMessage)))
                .modelOverride(MODEL_OVERRIDE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(callType)
                .build();
    }
}
