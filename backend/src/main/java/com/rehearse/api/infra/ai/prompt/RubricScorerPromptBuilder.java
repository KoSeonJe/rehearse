package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.feedback.rubric.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.Rubric;
import com.rehearse.api.domain.feedback.rubric.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.RubricLoader;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.domain.ResumeMode;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.Resource;
import org.springframework.core.io.ResourceLoader;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class RubricScorerPromptBuilder {

    private static final String CALL_TYPE = "rubric_scorer";
    private static final String TEMPLATE_PATH = "classpath:prompts/template/turn-rubric-scorer.txt";

    private final RubricLoader rubricLoader;
    private final ResourceLoader resourceLoader;
    private final ObjectMapper objectMapper;

    @Value("${rehearse.rubric-scorer.model:gpt-4o-mini}")
    private String model;

    @Value("${rehearse.rubric-scorer.temperature:0.2}")
    private double temperature;

    @Value("${rehearse.rubric-scorer.max-tokens:1536}")
    private int maxTokens;

    private String template;
    private String cachedDimensionDefinitions;

    @PostConstruct
    void init() throws IOException {
        Resource resource = resourceLoader.getResource(TEMPLATE_PATH);
        template = resource.getContentAsString(StandardCharsets.UTF_8);
        cachedDimensionDefinitions = buildDimensionDefinitions();
        log.info("RubricScorerPromptBuilder 초기화 완료");
    }

    public ChatRequest build(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel,
            ResumeMode resumeMode,
            Integer currentChainLevel,
            ResumeSkeleton resumeSkeleton
    ) {
        String systemPrompt = buildSystemPrompt();
        String userPrompt = buildUserPrompt(question, userAnswer, analysis, rubric,
                dimensionsToScore, userLevel, resumeMode, currentChainLevel, resumeSkeleton);

        List<ChatMessage> messages = new ArrayList<>();
        messages.add(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, systemPrompt));
        messages.add(ChatMessage.of(ChatMessage.Role.USER, userPrompt));

        return ChatRequest.builder()
                .messages(messages)
                .modelOverride(model)
                .temperature(temperature)
                .maxTokens(maxTokens)
                .cachePolicy(CachePolicy.defaults())
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType(CALL_TYPE)
                .build();
    }

    private String buildSystemPrompt() {
        return "You are an expert technical interview evaluator.\n\n" +
                "## All Dimension Definitions (reference only — score ONLY the dimensions listed in the user message)\n\n" +
                cachedDimensionDefinitions;
    }

    private String buildUserPrompt(
            Question question,
            String userAnswer,
            AnswerAnalysis analysis,
            Rubric rubric,
            List<String> dimensionsToScore,
            InterviewLevel userLevel,
            ResumeMode resumeMode,
            Integer currentChainLevel,
            ResumeSkeleton resumeSkeleton
    ) {
        String prompt = template
                .replace("{{USER_LEVEL}}", formatLevel(userLevel))
                .replace("{{QUESTION_TEXT}}", question.getQuestionText())
                .replace("{{USER_ANSWER}}", userAnswer != null ? userAnswer : "")
                .replace("{{ANSWER_ANALYSIS_JSON}}", serializeAnalysis(analysis))
                .replace("{{DIMENSIONS_TO_SCORE}}", String.join(", ", dimensionsToScore))
                .replace("{{DIMENSION_DEFINITIONS}}", buildSelectedDefinitions(dimensionsToScore))
                .replace("{{RESUME_CONTEXT}}", buildResumeContext(resumeSkeleton, dimensionsToScore))
                .replace("{{CHAIN_DEPTH_OVERRIDE}}", buildChainDepthOverride(currentChainLevel, dimensionsToScore));

        return prompt;
    }

    private String buildDimensionDefinitions() {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (Map.Entry<String, RubricDimension> entry : dimensions.entrySet()) {
            RubricDimension dim = entry.getValue();
            sb.append("### ").append(entry.getKey()).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildSelectedDefinitions(List<String> dimensionsToScore) {
        Map<String, RubricDimension> dimensions = rubricLoader.getAllDimensions();
        StringBuilder sb = new StringBuilder();
        for (String ref : dimensionsToScore) {
            RubricDimension dim = dimensions.get(ref);
            if (dim == null) {
                continue;
            }
            sb.append("### ").append(ref).append(" — ").append(dim.name()).append("\n");
            sb.append(dim.description()).append("\n");
            if (dim.scoring() != null) {
                for (Map.Entry<Integer, RubricDimension.ScoringLevel> se : dim.scoring().entrySet()) {
                    sb.append("- Score ").append(se.getKey()).append(" (").append(se.getValue().label()).append(")");
                    if (se.getValue().observable() != null && !se.getValue().observable().isEmpty()) {
                        sb.append(": ").append(String.join("; ", se.getValue().observable()));
                    }
                    sb.append("\n");
                }
            }
            sb.append("\n");
        }
        return sb.toString();
    }

    private String buildResumeContext(ResumeSkeleton resumeSkeleton, List<String> dimensionsToScore) {
        if (!dimensionsToScore.contains("D9") || resumeSkeleton == null) {
            return "";
        }
        StringBuilder sb = new StringBuilder("## Resume Skeleton Context (for D9 Factual Consistency)\n");
        if (resumeSkeleton.projects() != null) {
            for (var project : resumeSkeleton.projects()) {
                sb.append("### Project: ").append(project.projectId()).append("\n");
                if (project.claims() != null) {
                    for (var claim : project.claims()) {
                        sb.append("- [").append(claim.claimId()).append("] ").append(claim.text()).append("\n");
                    }
                }
            }
        }
        return sb.toString();
    }

    private String buildChainDepthOverride(Integer currentChainLevel, List<String> dimensionsToScore) {
        if (!dimensionsToScore.contains("D10") || currentChainLevel == null) {
            return "";
        }
        int d10Score = switch (currentChainLevel) {
            case 1 -> 1;
            case 2, 3 -> 2;
            default -> 3;
        };
        return "## D10 Chain Depth Override\n" +
                "The system has determined that the candidate reached chain level " + currentChainLevel +
                " in this session. Therefore D10 score MUST be " + d10Score + ". " +
                "Do not re-evaluate D10 from the answer text — use the provided value.\n";
    }

    private String formatLevel(InterviewLevel level) {
        if (level == null) return "MID";
        return switch (level) {
            case JUNIOR -> "JUNIOR";
            case MID -> "MID";
            case SENIOR -> "SENIOR";
        };
    }

    private String serializeAnalysis(AnswerAnalysis analysis) {
        if (analysis == null) return "{}";
        try {
            return objectMapper.writeValueAsString(analysis);
        } catch (JsonProcessingException e) {
            log.warn("AnswerAnalysis 직렬화 실패", e);
            return "{}";
        }
    }
}
