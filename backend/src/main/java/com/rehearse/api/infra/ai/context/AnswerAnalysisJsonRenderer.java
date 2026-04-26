package com.rehearse.api.infra.ai.context;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.Perspective;

import java.util.List;
import java.util.stream.Collectors;

/**
 * Serializes AnswerAnalysis to the structured text format consumed by FocusLayer
 * (answerAnalysisJson hint key for follow_up_generator_v3).
 *
 * Extracted from FollowUpPromptBuilder.buildUserPromptWithAnalysis to avoid
 * coupling the prompt builder to the L4 focusHints contract.
 */
public final class AnswerAnalysisJsonRenderer {

    private AnswerAnalysisJsonRenderer() {}

    public static String render(AnswerAnalysis analysis, List<Perspective> askedPerspectives) {
        StringBuilder sb = new StringBuilder();
        sb.append(formatClaims(analysis.claims()));
        sb.append("- missing_perspectives: ").append(formatPerspectives(analysis.missingPerspectives())).append("\n");
        sb.append("- unstated_assumptions: ").append(formatStrings(analysis.unstatedAssumptions())).append("\n");
        sb.append("- recommended_next_action: ").append(analysis.recommendedNextAction().name()).append("\n");
        sb.append("- asked_perspectives: ").append(formatPerspectives(askedPerspectives)).append("\n");
        return sb.toString();
    }

    private static String formatClaims(List<Claim> claims) {
        if (claims == null || claims.isEmpty()) {
            return "- claims: (없음)\n";
        }
        StringBuilder sb = new StringBuilder("- claims:\n");
        for (int i = 0; i < claims.size(); i++) {
            Claim c = claims.get(i);
            sb.append("  [").append(i).append("] text=\"").append(c.text())
              .append("\" depth_score=").append(c.depthScore())
              .append(" evidence_strength=").append(c.evidenceStrength().name())
              .append(" topic_tag=").append(c.topicTag() == null ? "(없음)" : c.topicTag())
              .append("\n");
        }
        return sb.toString();
    }

    private static String formatPerspectives(List<Perspective> perspectives) {
        if (perspectives == null || perspectives.isEmpty()) {
            return "(없음)";
        }
        return perspectives.stream().map(Enum::name).collect(Collectors.joining(", "));
    }

    private static String formatStrings(List<String> values) {
        if (values == null || values.isEmpty()) {
            return "(없음)";
        }
        return String.join(" | ", values);
    }
}
