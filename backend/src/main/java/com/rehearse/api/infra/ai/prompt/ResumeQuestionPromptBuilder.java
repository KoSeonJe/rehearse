package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionPromptBuilder {

    private static final String CALL_TYPE = "resume_question_generator";

    private final InterviewContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    public PromptPair build(ResumeSkeleton skeleton, int openerCount, int mainCount) {
        String skeletonJson = serializeSkeleton(skeleton);
        String primaryProjectName = skeleton.projects().isEmpty()
                ? null
                : skeleton.projects().get(0).projectName();
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.ResumeQuestionGeneratorHints(skeletonJson, openerCount, mainCount, primaryProjectName),
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
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.error("[ResumeQuestionPromptBuilder] skeleton 직렬화 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }

    public record PromptPair(String system, String user) {}
}
