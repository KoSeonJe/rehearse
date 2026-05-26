package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Claim;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpQuestionPromptBuilder {

    private static final String CALL_TYPE = PromptTemplateLoader.FOLLOW_UP_GENERATOR_V3;
    private static final int USER_CAP = 1400;

    private final PromptTemplateLoader templateLoader;
    private final TokenEstimator tokenEstimator;

    public PromptPair build(
            String mainQuestion,
            AnswerAnalysis analysis
    ) {
        String system = templateLoader.system(CALL_TYPE);
        String user = buildUserFragment(mainQuestion, analysis);

        int estimated = tokenEstimator.estimate(user);
        if (estimated > USER_CAP) {
            log.warn("[{}] user fragment cap 초과(절단 안 함): estimated={}, cap={}", CALL_TYPE, estimated, USER_CAP);
        }
        return new PromptPair(system, user);
    }

    private String buildUserFragment(String mainQuestion, AnswerAnalysis analysis) {
        List<String> claims = analysis.claims().stream().map(Claim::text).toList();
        return "<<<MAIN_QUESTION>>>\n" + nz(mainQuestion) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(analysis.transcript()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "<<<CLAIMS>>>\n" + formatList(claims) + "\n<<<END_CLAIMS>>>\n\n" +
               "WEAKEST_DIMENSION: " + nz(analysis.weakestDimension()) + "\n" +
               "DIMENSION_GAPS: " + formatMap(analysis.dimensionGaps()) + "\n\n" +
               "<<<UNSTATED_ASSUMPTIONS>>>\n" + formatList(analysis.unstatedAssumptions()) + "\n<<<END_UNSTATED_ASSUMPTIONS>>>\n\n" +
               "위 분석 결과를 바탕으로 새 후속 질문을 생성하세요.";
    }

    private static String nz(String v) {
        if (v == null) return "(없음)";
        String s = v.strip();
        return s.isEmpty() ? "(없음)" : s;
    }

    private static String formatList(List<String> list) {
        if (list == null || list.isEmpty()) return "(없음)";
        return String.join(" | ", list);
    }

    private static String formatMap(Map<String, Integer> map) {
        if (map == null || map.isEmpty()) return "(없음)";
        return map.entrySet().stream()
                .map(e -> e.getKey() + "=" + e.getValue())
                .collect(Collectors.joining(", "));
    }

    public record PromptPair(String system, String user) {}
}
