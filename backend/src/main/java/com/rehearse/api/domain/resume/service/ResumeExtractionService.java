package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Service;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractionService {

    private static final String CALL_TYPE = "resume_extractor";
    private static final double TEMPERATURE = 0.2;
    private static final int MAX_TOKENS = 12000;
    private static final String SYSTEM_PROMPT_PATH = "prompts/template/resume/resume-extractor.txt";

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;

    @Value("${ai.resume.extractor.timeout-ms:60000}")
    private long timeoutMs;

    private String systemPrompt;

    @PostConstruct
    void init() {
        try {
            systemPrompt = StreamUtils.copyToString(
                    new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("resume-extractor 프롬프트 로드 실패", e);
        }
    }

    public ResumeSkeleton extract(String normalizedText, String fileHash) {
        ChatRequest request = ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.SYSTEM, systemPrompt),
                        ChatMessage.of(ChatMessage.Role.USER,
                                "<<<RESUME_TEXT>>>\n" + normalizedText + "\n<<<END_RESUME_TEXT>>>")))
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(request);
        GeneratedResumeSkeleton parsed = aiResponseParser.parseOrRetry(
                response, GeneratedResumeSkeleton.class, aiClient, request);

        return mapToSkeleton(parsed, fileHash);
    }

    private ResumeSkeleton mapToSkeleton(GeneratedResumeSkeleton parsed, String fileHash) {
        CandidateLevel level = parseCandidateLevel(parsed.candidateLevel());
        List<Project> projects = parsed.projects().stream()
                .map(p -> new Project(
                        p.projectId(),
                        p.projectName() == null ? "" : p.projectName(),
                        p.techStack(),
                        p.role(),
                        p.architecture(),
                        p.decisions()))
                .toList();
        return new ResumeSkeleton(parsed.resumeId(), fileHash, level, parsed.targetDomain(), projects);
    }

    private CandidateLevel parseCandidateLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return CandidateLevel.JUNIOR;
        }
        try {
            return CandidateLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 candidate_level={} → JUNIOR fallback", raw);
            return CandidateLevel.JUNIOR;
        }
    }

    public long timeoutMs() {
        return timeoutMs;
    }
}
