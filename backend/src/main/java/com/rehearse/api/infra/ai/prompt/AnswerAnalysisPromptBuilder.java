package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.ReferenceType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnswerAnalysisPromptBuilder {

    private static final String CALL_TYPE = PromptTemplateLoader.ANSWER_ANALYZER;
    private static final int USER_CAP = 800;

    private final PromptTemplateLoader templateLoader;
    private final TokenEstimator tokenEstimator;

    public PromptPair build(
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            QuestionCategory category
    ) {
        String personaDepthHint = PromptFormatters.toReferenceLabel(questionReferenceType);
        String system = templateLoader.system(CALL_TYPE);
        String user = buildUserFragment(mainQuestion, userAnswer, personaDepthHint, category);

        int estimated = tokenEstimator.estimate(user);
        if (estimated > USER_CAP) {
            log.warn("[{}] user fragment cap 초과(절단 안 함): estimated={}, cap={}", CALL_TYPE, estimated, USER_CAP);
        }
        return new PromptPair(system, user);
    }

    private String buildUserFragment(String mainQuestion, String userAnswer, String personaDepthHint, QuestionCategory category) {
        return "CATEGORY: " + category.name() + "\n\n" +
               "<<<MAIN_QUESTION>>>\n" + nz(mainQuestion) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(userAnswer) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "PERSONA_DEPTH: " + nz(personaDepthHint) + "\n\n" +
               "위 답변을 분석해 JSON 한 객체로만 응답하세요.";
    }

    private static String nz(String v) {
        if (v == null) return "(없음)";
        String s = v.strip();
        return s.isEmpty() ? "(없음)" : s;
    }

    public record PromptPair(String system, String user) {}
}
