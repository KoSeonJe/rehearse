package com.rehearse.api.infra.ai.context;

import java.util.List;
import java.util.Map;

/**
 * L4 FocusLayer 입력. callType 별 1:1 record 로 매핑되어 컴파일타임 exhaustive 매칭을 강제한다.
 */
public sealed interface FocusHints
        permits FocusHints.AnswerAnalyzerHints,
                FocusHints.FollowUpGeneratorV3Hints,
                FocusHints.ResumeQuestionGeneratorHints,
                FocusHints.EmptyHints {

    record AnswerAnalyzerHints(
            String mainQuestion,
            String userAnswer,
            String personaDepthHint,
            boolean isResumeTrack
    ) implements FocusHints {}

    record FollowUpGeneratorV3Hints(
            String mainQuestion,
            String userAnswer,
            List<String> claims,
            Map<String, Integer> dimensionGaps,
            String weakestDimension,
            List<String> unstatedAssumptions
    ) implements FocusHints {}

    record ResumeQuestionGeneratorHints(
            String resumeSkeletonJson,
            int openerCount,
            int mainCount,
            String primaryProjectName
    ) implements FocusHints {}

    record EmptyHints() implements FocusHints {
        public static final EmptyHints INSTANCE = new EmptyHints();
    }
}
