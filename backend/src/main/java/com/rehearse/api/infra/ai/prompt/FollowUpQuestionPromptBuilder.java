package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpQuestionPromptBuilder {

    private static final String CALL_TYPE = "follow_up_generator_v3";

    private final InterviewContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    public PromptPair build(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis,
            ResumeSkeleton resumeSkeleton
    ) {
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.FollowUpGeneratorV3Hints(
                        mainQuestion != null ? mainQuestion : "",
                        userAnswer != null ? userAnswer : "",
                        analysis.claims().stream().map(c -> c.text()).toList(),
                        analysis.dimensionGaps(),
                        analysis.weakestDimension(),
                        analysis.unstatedAssumptions(),
                        serializeSkeleton(resumeSkeleton)
                ),
                null
        ));

        String system = null;
        StringBuilder user = new StringBuilder();
        for (ChatMessage msg : built.messages()) {
            if (msg.role() == ChatMessage.Role.SYSTEM) {
                system = msg.content();
            } else if (msg.role() == ChatMessage.Role.USER) {
                if (!user.isEmpty()) {
                    user.append("\n\n");
                }
                user.append(msg.content());
            }
        }
        if (system == null) {
            system = "";
        }
        return new PromptPair(system, user.toString());
    }

    private String serializeSkeleton(ResumeSkeleton skeleton) {
        if (skeleton == null) {
            return "";
        }
        try {
            return objectMapper.writeValueAsString(skeleton.projects());
        } catch (JsonProcessingException e) {
            log.warn("[FollowUpQuestionPromptBuilder] skeleton 직렬화 실패 — 빈 문자열 사용", e);
            return "";
        }
    }

    public record PromptPair(String system, String user) {}
}
