package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.ReferenceType;
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
public class AnswerAnalysisPromptBuilder {

    private static final String CALL_TYPE = "answer_analyzer";

    private final InterviewContextBuilder contextBuilder;

    public PromptPair build(
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        String personaDepthHint = PromptFormatters.toReferenceLabel(questionReferenceType);
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.AnswerAnalyzerHints(
                        mainQuestion != null ? mainQuestion : "",
                        userAnswer != null ? userAnswer : "",
                        personaDepthHint,
                        isResumeTrack
                ),
                null,
                null,
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

    public record PromptPair(String system, String user) {}
}
