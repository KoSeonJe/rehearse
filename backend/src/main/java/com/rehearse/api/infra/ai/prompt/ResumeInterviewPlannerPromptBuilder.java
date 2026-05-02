package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class ResumeInterviewPlannerPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/resume/resume-interview-planner.txt";

    private final String modelOverride;
    private final double temperature;
    private final int maxTokens;
    private final ObjectMapper objectMapper;

    private String userPromptTemplate;

    public ResumeInterviewPlannerPromptBuilder(
            @Value("${rehearse.resume-planner.model:gpt-4o-mini}") String modelOverride,
            @Value("${rehearse.resume-planner.temperature:0.3}") double temperature,
            @Value("${rehearse.resume-planner.max-tokens:2048}") int maxTokens,
            ObjectMapper objectMapper
    ) {
        this.modelOverride = modelOverride;
        this.temperature = temperature;
        this.maxTokens = maxTokens;
        this.objectMapper = objectMapper;
    }

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
        log.info("Resume Interview Planner 프롬프트 템플릿 로드 완료 model={}", modelOverride);
    }

    public ChatRequest build(ResumeSkeleton skeleton, int durationMin, String userLevel, String callType) {
        String skeletonJson = serializeSkeleton(skeleton);
        String allowedChainIdsJson = buildAllowedChainIdsJson(skeleton);

        String userMessage = userPromptTemplate
                .replace("{{SKELETON_JSON}}", skeletonJson)
                .replace("{{DURATION_MIN}}", String.valueOf(durationMin))
                .replace("{{USER_LEVEL}}", userLevel != null ? userLevel : "MID")
                .replace("{{ALLOWED_CHAIN_IDS_JSON}}", allowedChainIdsJson);

        return ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, userMessage)))
                .modelOverride(modelOverride)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(callType)
                .build();
    }

    private String buildAllowedChainIdsJson(ResumeSkeleton skeleton) {
        if (skeleton == null || skeleton.projects() == null) {
            return "[]";
        }
        List<String> chainIds = skeleton.projects().stream()
                .filter(p -> p.implicitCsTopics() != null)
                .flatMap(p -> p.implicitCsTopics().stream()
                        .map(chain -> ChainReference.synthesizeChainId(p.projectId(), chain.topic())))
                .toList();
        try {
            return objectMapper.writeValueAsString(chainIds);
        } catch (JsonProcessingException e) {
            log.warn("ALLOWED_CHAIN_IDS 직렬화 실패 — 빈 배열로 폴백: {}", e.getMessage());
            return "[]";
        }
    }

    private String serializeSkeleton(ResumeSkeleton skeleton) {
        if (skeleton == null) {
            return "{}";
        }
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.warn("skeleton 직렬화 실패: {}", e.getMessage());
            return "{}";
        }
    }
}
