package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

/**
 * L4 FocusLayer — JIT per-callType USER fragment renderer.
 *
 * focusHints keys consumed (all optional — missing keys render as empty placeholder):
 *   mainQuestion        : String  — the current interview question text
 *   userUtterance       : String  — raw utterance for intent/clarify/giveup paths
 *   userAnswer          : String  — answer text for analyzer and follow-up paths
 *   personaDepthHint    : String  — persona depth descriptor injected by caller
 *   answerAnalysisJson  : String  — pre-serialized AnswerAnalysis JSON for follow_up_generator_v3
 *   askedPerspectives   : List<String> — perspectives already asked, for deduplication
 */
@Component
@RequiredArgsConstructor
public class FocusLayer implements ContextLayer {

    static final int CAP_INTENT_CLASSIFIER = 300;
    static final int CAP_ANSWER_ANALYZER = 800;
    static final int CAP_FOLLOW_UP_GENERATOR_V3 = 1000;
    static final int CAP_CLARIFY_RESPONSE = 400;
    static final int CAP_GIVEUP_RESPONSE = 400;

    private final TokenEstimator tokenEstimator;

    @Override
    public List<ChatMessage> build(ContextBuildRequest req) {
        return switch (req.callType()) {
            case "intent_classifier"      -> render(buildIntentClassifier(req.focusHints()), CAP_INTENT_CLASSIFIER);
            case "answer_analyzer"        -> render(buildAnswerAnalyzer(req.focusHints()), CAP_ANSWER_ANALYZER);
            case "follow_up_generator_v3" -> render(buildFollowUpGeneratorV3(req.focusHints()), CAP_FOLLOW_UP_GENERATOR_V3);
            case "clarify_response"       -> render(buildClarifyResponse(req.focusHints()), CAP_CLARIFY_RESPONSE);
            case "giveup_response"        -> render(buildGiveUpResponse(req.focusHints()), CAP_GIVEUP_RESPONSE);
            case "compaction_summarizer"  -> List.of();
            default                       -> List.of();
        };
    }

    private List<ChatMessage> render(String fragment, int cap) {
        int estimated = tokenEstimator.estimate(fragment);
        if (estimated > cap) {
            throw new IllegalStateException(
                "L4 fragment exceeds " + cap + " tokens (estimated " + estimated + ")"
            );
        }
        return List.of(ChatMessage.of(ChatMessage.Role.USER, fragment));
    }

    private String buildIntentClassifier(Map<String, Object> hints) {
        String mainQuestion = str(hints, "mainQuestion");
        String userUtterance = str(hints, "userUtterance");

        return "<<<MAIN_QUESTION>>>\n" + mainQuestion + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + userUtterance + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "위 답변의 의도를 분류하세요.";
    }

    private String buildAnswerAnalyzer(Map<String, Object> hints) {
        String mainQuestion = str(hints, "mainQuestion");
        String userAnswer = str(hints, "userAnswer");
        String personaDepthHint = str(hints, "personaDepthHint");

        return "<<<MAIN_QUESTION>>>\n" + mainQuestion + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + userAnswer + "\n<<<END_USER_ANSWER>>>\n\n" +
               "PERSONA_DEPTH: " + personaDepthHint + "\n\n" +
               "위 답변을 분석해 JSON 한 객체로만 응답하세요.";
    }

    private String buildFollowUpGeneratorV3(Map<String, Object> hints) {
        String answerAnalysisJson = str(hints, "answerAnalysisJson");
        String askedPerspectives = formatList(hints, "askedPerspectives");

        return "ANSWER_ANALYSIS:\n" + answerAnalysisJson + "\n\n" +
               "asked_perspectives: " + askedPerspectives + "\n\n" +
               "위 ANSWER_ANALYSIS 를 바탕으로 새 후속 질문을 생성하세요.";
    }

    private String buildClarifyResponse(Map<String, Object> hints) {
        String mainQuestion = str(hints, "mainQuestion");
        String userUtterance = str(hints, "userUtterance");

        return "<<<MAIN_QUESTION>>>\n" + mainQuestion + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + userUtterance + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "위 응시자가 질문 의미를 이해하지 못했습니다. 질문을 더 쉬운 말로 재설명하고 힌트 1개를 제공하세요.";
    }

    private String buildGiveUpResponse(Map<String, Object> hints) {
        String mainQuestion = str(hints, "mainQuestion");
        String userUtterance = str(hints, "userUtterance");
        String personaDepthHint = str(hints, "personaDepthHint");

        return "<<<MAIN_QUESTION>>>\n" + mainQuestion + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + userUtterance + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "PERSONA_GREETING_HINT: " + personaDepthHint + "\n\n" +
               "응시자가 포기 의사를 밝혔습니다. SCAFFOLD 또는 REVEAL_AND_MOVE_ON 모드를 선택하여 적절히 응답하세요.";
    }

    private static String str(Map<String, Object> hints, String key) {
        if (hints == null) {
            return "(없음)";
        }
        Object val = hints.get(key);
        if (val == null) {
            return "(없음)";
        }
        String s = val.toString().strip();
        return s.isEmpty() ? "(없음)" : s;
    }

    @SuppressWarnings("unchecked")
    private static String formatList(Map<String, Object> hints, String key) {
        if (hints == null) {
            return "(없음)";
        }
        Object val = hints.get(key);
        if (val instanceof List<?> list && !list.isEmpty()) {
            return String.join(", ", (List<String>) list);
        }
        if (val instanceof String s && !s.isBlank()) {
            return s;
        }
        return "(없음)";
    }
}
