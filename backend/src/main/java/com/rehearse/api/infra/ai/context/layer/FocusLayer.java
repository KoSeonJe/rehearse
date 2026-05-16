package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import java.util.stream.Collectors;

/**
 * L4 FocusLayer — JIT per-callType USER fragment renderer.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class FocusLayer implements ContextLayer {

    static final int CAP_ANSWER_ANALYZER = 800;
    static final int CAP_FOLLOW_UP_GENERATOR_V3 = 1400;
    static final int CAP_RESUME_QUESTION_GENERATOR = 16000;

    private static final double SAFETY_MARGIN = 0.9;
    private static final int MAX_TRUNCATE_ITERATIONS = 8;
    private static final int FALLBACK_TAIL_PRESERVE_CHARS = 200;
    private static final Pattern MARKER_BLOCK_PATTERN =
            Pattern.compile("<<<([A-Z_]+)>>>\\n([\\s\\S]*?)\\n<<<END_\\1>>>", Pattern.MULTILINE);

    private final TokenEstimator tokenEstimator;

    @Override
    public List<ChatMessage> build(ContextBuildRequest req) {
        FocusHints hints = req.focusHints();
        String callType = req.callType();
        return switch (hints) {
            case FocusHints.AnswerAnalyzerHints h -> render(buildAnswerAnalyzer(h), CAP_ANSWER_ANALYZER, callType);
            case FocusHints.FollowUpGeneratorV3Hints h -> render(buildFollowUpGeneratorV3(h), CAP_FOLLOW_UP_GENERATOR_V3, callType);
            case FocusHints.ResumeQuestionGeneratorHints h -> render(buildResumeQuestionGenerator(h), CAP_RESUME_QUESTION_GENERATOR, callType);
            case FocusHints.EmptyHints ignored -> handleEmpty(callType);
        };
    }

    private List<ChatMessage> handleEmpty(String callType) {
        log.warn("[FocusLayer] L4 미등록 callType 진입: callType={}", callType);
        return List.of();
    }

    private String buildResumeQuestionGenerator(FocusHints.ResumeQuestionGeneratorHints h) {
        return "<<<RESUME_SKELETON>>>\n" + nz(h.resumeSkeletonJson()) + "\n<<<END_RESUME_SKELETON>>>\n\n" +
               "OPENER_COUNT: " + h.openerCount() + "\n" +
               "MAIN_COUNT: " + h.mainCount() + "\n\n" +
               "위 RESUME_SKELETON 을 기반으로 opener N개 + main M개 질문을 JSON 한 객체로만 응답하세요.";
    }

    private List<ChatMessage> render(String fragment, int cap, String callType) {
        int estimated = tokenEstimator.estimate(fragment);
        if (estimated > cap) {
            log.warn("[FocusLayer] L4 cap 초과 → 본문 절단: callType={}, estimated={}, cap={}",
                    callType, estimated, cap);
            fragment = truncateBodyWithSafetyMargin(fragment, cap);
        }
        return List.of(ChatMessage.of(ChatMessage.Role.USER, fragment));
    }

    private String truncateBodyWithSafetyMargin(String fragment, int cap) {
        int targetTokens = (int) Math.floor(cap * SAFETY_MARGIN);
        int targetChars = Math.max(1, targetTokens * 4);

        String current = fragment;
        for (int i = 0; i < MAX_TRUNCATE_ITERATIONS; i++) {
            if (tokenEstimator.estimate(current) <= targetTokens) {
                return current;
            }
            int currentLen = current.length();
            if (currentLen <= targetChars) {
                return current;
            }
            int excess = currentLen - targetChars;
            String reduced = reduceMarkerBlocks(current, excess);
            if (reduced.equals(current)) {
                reduced = headTruncatePreservingTail(current, targetChars);
            }
            current = reduced;
        }
        if (tokenEstimator.estimate(current) > targetTokens) {
            log.warn("[FocusLayer] L4 절단 미수렴 → 강제 head 절단: iterations={}, targetTokens={}",
                    MAX_TRUNCATE_ITERATIONS, targetTokens);
            current = headTruncatePreservingTail(current, targetChars);
        }
        return current;
    }

    private String reduceMarkerBlocks(String fragment, int charsToRemove) {
        Matcher matcher = MARKER_BLOCK_PATTERN.matcher(fragment);
        int totalBodyLen = 0;
        int blockCount = 0;
        while (matcher.find()) {
            totalBodyLen += matcher.group(2).length();
            blockCount++;
        }
        if (blockCount == 0 || totalBodyLen == 0) {
            return fragment;
        }

        StringBuilder sb = new StringBuilder(fragment.length());
        int cursor = 0;
        matcher.reset();
        while (matcher.find()) {
            sb.append(fragment, cursor, matcher.start());
            String markerName = matcher.group(1);
            String body = matcher.group(2);
            int bodyShare = (int) Math.ceil(((double) body.length() / totalBodyLen) * charsToRemove);
            int keep = Math.max(0, body.length() - bodyShare);
            String truncated = body.length() <= keep ? body : body.substring(0, keep);
            sb.append("<<<").append(markerName).append(">>>\n")
                    .append(truncated)
                    .append("\n<<<END_").append(markerName).append(">>>");
            cursor = matcher.end();
        }
        sb.append(fragment, cursor, fragment.length());
        return sb.toString();
    }

    private String headTruncatePreservingTail(String fragment, int targetChars) {
        int tailKeep = Math.min(FALLBACK_TAIL_PRESERVE_CHARS, fragment.length());
        if (targetChars >= fragment.length()) {
            return fragment;
        }
        int headBudget = Math.max(0, targetChars - tailKeep);
        if (headBudget <= 0) {
            return fragment.substring(fragment.length() - tailKeep);
        }
        return fragment.substring(0, headBudget) + fragment.substring(fragment.length() - tailKeep);
    }

    private String buildAnswerAnalyzer(FocusHints.AnswerAnalyzerHints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(h.userAnswer()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "PERSONA_DEPTH: " + nz(h.personaDepthHint()) + "\n\n" +
               "위 답변을 분석해 JSON 한 객체로만 응답하세요.";
    }

    private String buildFollowUpGeneratorV3(FocusHints.FollowUpGeneratorV3Hints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(h.userAnswer()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "<<<CLAIMS>>>\n" + formatList(h.claims()) + "\n<<<END_CLAIMS>>>\n\n" +
               "WEAKEST_DIMENSION: " + nz(h.weakestDimension()) + "\n" +
               "DIMENSION_GAPS: " + formatMap(h.dimensionGaps()) + "\n\n" +
               "<<<UNSTATED_ASSUMPTIONS>>>\n" + formatList(h.unstatedAssumptions()) + "\n<<<END_UNSTATED_ASSUMPTIONS>>>\n\n" +
               "<<<RESUME_SKELETON>>>\n" + nz(h.resumeSkeletonJson()) + "\n<<<END_RESUME_SKELETON>>>\n\n" +
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
}
