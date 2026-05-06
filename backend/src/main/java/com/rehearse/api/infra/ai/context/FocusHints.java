package com.rehearse.api.infra.ai.context;

import java.util.List;

/**
 * L4 FocusLayer 입력. callType 별 1:1 record 로 매핑되어 컴파일타임 exhaustive 매칭을 강제한다.
 * EmptyHints 는 compaction_summarizer 등 hint 가 없는 경우용 명시 sentinel.
 */
public sealed interface FocusHints
        permits FocusHints.IntentClassifierHints,
                FocusHints.AnswerAnalyzerHints,
                FocusHints.FollowUpGeneratorV3Hints,
                FocusHints.ClarifyResponseHints,
                FocusHints.GiveUpResponseHints,
                FocusHints.ResumePlaygroundOpenerHints,
                FocusHints.ResumePlaygroundResponderHints,
                FocusHints.ResumeChainInterrogatorHints,
                FocusHints.ResumeWrapUpHints,
                FocusHints.EmptyHints {

    record IntentClassifierHints(String mainQuestion, String userUtterance) implements FocusHints {}

    record AnswerAnalyzerHints(String mainQuestion, String userAnswer, String personaDepthHint) implements FocusHints {}

    record FollowUpGeneratorV3Hints(String answerAnalysisJson, List<String> askedPerspectives) implements FocusHints {}

    record ClarifyResponseHints(String mainQuestion, String userUtterance) implements FocusHints {}

    record GiveUpResponseHints(String mainQuestion, String userUtterance, String personaDepthHint) implements FocusHints {}

    record ResumePlaygroundOpenerHints(String projectInfo, String openerQuestion) implements FocusHints {}

    record ResumePlaygroundResponderHints(
            String projectInfo,
            String expectedClaims,
            String userAnswer,
            int playgroundTurnCount,
            int cumulativeUtteranceLength
    ) implements FocusHints {}

    record ResumeChainInterrogatorHints(
            String projectName,
            String currentChain,
            int currentLevel,
            int answerQuality,
            String userAnswer,
            int consecutiveStayCount
    ) implements FocusHints {}

    record ResumeWrapUpHints(
            String projectName,
            String sessionSummary,
            long remainingMinutes,
            boolean isRetrospective
    ) implements FocusHints {}

    record EmptyHints() implements FocusHints {
        public static final EmptyHints INSTANCE = new EmptyHints();
    }
}
