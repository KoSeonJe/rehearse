package com.rehearse.api.infra.ai.context.layer;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.token.TokenEstimator;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * L4 FocusLayer — JIT per-callType USER fragment renderer.
 * FocusHints sealed pattern 매칭으로 컴파일타임 callType ↔ hint type 정합성 보장.
 */
@Component
@RequiredArgsConstructor
public class FocusLayer implements ContextLayer {

    static final int CAP_INTENT_CLASSIFIER = 300;
    static final int CAP_ANSWER_ANALYZER = 800;
    static final int CAP_FOLLOW_UP_GENERATOR_V3 = 1000;
    static final int CAP_CLARIFY_RESPONSE = 400;
    static final int CAP_GIVEUP_RESPONSE = 400;
    static final int CAP_RESUME_PLAYGROUND_OPENER = 600;
    static final int CAP_RESUME_PLAYGROUND_RESPONDER = 1000;
    static final int CAP_RESUME_CHAIN_INTERROGATOR = 1200;
    static final int CAP_RESUME_WRAP_UP = 600;

    private final TokenEstimator tokenEstimator;

    @Override
    public List<ChatMessage> build(ContextBuildRequest req) {
        FocusHints hints = req.focusHints();
        return switch (hints) {
            case FocusHints.IntentClassifierHints h -> render(buildIntentClassifier(h), CAP_INTENT_CLASSIFIER);
            case FocusHints.AnswerAnalyzerHints h -> render(buildAnswerAnalyzer(h), CAP_ANSWER_ANALYZER);
            case FocusHints.FollowUpGeneratorV3Hints h -> render(buildFollowUpGeneratorV3(h), CAP_FOLLOW_UP_GENERATOR_V3);
            case FocusHints.ClarifyResponseHints h -> render(buildClarifyResponse(h), CAP_CLARIFY_RESPONSE);
            case FocusHints.GiveUpResponseHints h -> render(buildGiveUpResponse(h), CAP_GIVEUP_RESPONSE);
            case FocusHints.ResumePlaygroundOpenerHints h -> render(buildResumePlaygroundOpener(h), CAP_RESUME_PLAYGROUND_OPENER);
            case FocusHints.ResumePlaygroundResponderHints h -> render(buildResumePlaygroundResponder(h), CAP_RESUME_PLAYGROUND_RESPONDER);
            case FocusHints.ResumeChainInterrogatorHints h -> render(buildResumeChainInterrogator(h), CAP_RESUME_CHAIN_INTERROGATOR);
            case FocusHints.ResumeWrapUpHints h -> render(buildResumeWrapUp(h), CAP_RESUME_WRAP_UP);
            case FocusHints.EmptyHints ignored -> handleEmpty(req.callType());
        };
    }

    private List<ChatMessage> handleEmpty(String callType) {
        if ("compaction_summarizer".equals(callType)) {
            return List.of();
        }
        throw new IllegalStateException("L4 unregistered callType: " + callType);
    }

    private String buildResumePlaygroundOpener(FocusHints.ResumePlaygroundOpenerHints h) {
        return "<<<PROJECT_INFO>>>\n" + nz(h.projectInfo()) + "\n<<<END_PROJECT_INFO>>>\n\n" +
               "<<<OPENER_QUESTION>>>\n" + nz(h.openerQuestion()) + "\n<<<END_OPENER_QUESTION>>>\n\n" +
               "위 정보를 기반으로 Playground 오프너 질문을 JSON 한 객체로만 응답하세요.";
    }

    private String buildResumePlaygroundResponder(FocusHints.ResumePlaygroundResponderHints h) {
        return "<<<EXPECTED_CLAIMS>>>\n" + nz(h.expectedClaims()) + "\n<<<END_EXPECTED_CLAIMS>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(h.userAnswer()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "PLAYGROUND_TURN_COUNT: " + h.playgroundTurnCount() + "\n" +
               "CUMULATIVE_UTTERANCE_LENGTH: " + h.cumulativeUtteranceLength() + "\n\n" +
               "위 입력으로 Responder 결정과 다음 질문을 JSON 한 객체로만 응답하세요.";
    }

    private String buildResumeChainInterrogator(FocusHints.ResumeChainInterrogatorHints h) {
        return "<<<CURRENT_CHAIN>>>\n" + nz(h.currentChain()) + "\n<<<END_CURRENT_CHAIN>>>\n\n" +
               "CURRENT_LEVEL: " + h.currentLevel() + "\n" +
               "ANSWER_QUALITY: " + h.answerQuality() + "\n" +
               "CONSECUTIVE_STAY_COUNT: " + h.consecutiveStayCount() + "\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(h.userAnswer()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "위 chain 상태에서 LEVEL_UP/LEVEL_STAY/CHAIN_SWITCH 결정과 다음 질문을 JSON 한 객체로만 응답하세요.";
    }

    private String buildResumeWrapUp(FocusHints.ResumeWrapUpHints h) {
        return "<<<SESSION_SUMMARY>>>\n" + nz(h.sessionSummary()) + "\n<<<END_SESSION_SUMMARY>>>\n\n" +
               "REMAINING_MINUTES: " + h.remainingMinutes() + "\n" +
               "IS_RETROSPECTIVE: " + h.isRetrospective() + "\n\n" +
               "WRAP_UP 단계 회고/마무리 질문을 JSON 한 객체로만 응답하세요.";
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

    private String buildIntentClassifier(FocusHints.IntentClassifierHints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + nz(h.userUtterance()) + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "위 답변의 의도를 분류하세요.";
    }

    private String buildAnswerAnalyzer(FocusHints.AnswerAnalyzerHints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_ANSWER>>>\n" + nz(h.userAnswer()) + "\n<<<END_USER_ANSWER>>>\n\n" +
               "PERSONA_DEPTH: " + nz(h.personaDepthHint()) + "\n\n" +
               "위 답변을 분석해 JSON 한 객체로만 응답하세요.";
    }

    private String buildFollowUpGeneratorV3(FocusHints.FollowUpGeneratorV3Hints h) {
        return "ANSWER_ANALYSIS:\n" + nz(h.answerAnalysisJson()) + "\n\n" +
               "asked_perspectives: " + formatList(h.askedPerspectives()) + "\n\n" +
               "위 ANSWER_ANALYSIS 를 바탕으로 새 후속 질문을 생성하세요.";
    }

    private String buildClarifyResponse(FocusHints.ClarifyResponseHints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + nz(h.userUtterance()) + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "위 응시자가 질문 의미를 이해하지 못했습니다. 질문을 더 쉬운 말로 재설명하고 힌트 1개를 제공하세요.";
    }

    private String buildGiveUpResponse(FocusHints.GiveUpResponseHints h) {
        return "<<<MAIN_QUESTION>>>\n" + nz(h.mainQuestion()) + "\n<<<END_MAIN_QUESTION>>>\n\n" +
               "<<<USER_UTTERANCE>>>\n" + nz(h.userUtterance()) + "\n<<<END_USER_UTTERANCE>>>\n\n" +
               "PERSONA_GREETING_HINT: " + nz(h.personaDepthHint()) + "\n\n" +
               "응시자가 포기 의사를 밝혔습니다. SCAFFOLD 또는 REVEAL_AND_MOVE_ON 모드를 선택하여 적절히 응답하세요.";
    }

    private static String nz(String v) {
        if (v == null) return "(없음)";
        String s = v.strip();
        return s.isEmpty() ? "(없음)" : s;
    }

    private static String formatList(List<String> list) {
        if (list == null || list.isEmpty()) return "(없음)";
        return String.join(", ", list);
    }
}
