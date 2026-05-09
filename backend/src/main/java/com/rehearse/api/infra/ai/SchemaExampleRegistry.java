package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import com.rehearse.api.infra.ai.dto.GeneratedCompactionSummary;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import org.springframework.stereotype.Component;

import java.util.Map;

/**
 * 1차 JSON 파싱 실패 시 LLM 에게 재시도용으로 보낼 스키마 예시 모음.
 * Jackson 의 exception message 만으로는 LLM 이 정확한 객체 형태를 복원하지 못하는 사례가 잦아
 * (특히 nested 객체 배열을 string 배열로 잘못 반환하는 케이스) 명시적 예시를 함께 보낸다.
 */
@Component
public class SchemaExampleRegistry {

    private static final String ANSWER_ANALYSIS_EXAMPLE = """
            {
              "claims": [
                {"text": "한 문장 요약", "depth_score": 3, "evidence_strength": "WEAK", "topic_tag": "topic"}
              ],
              "missing_perspectives": ["TRADEOFF"],
              "unstated_assumptions": ["..."],
              "answer_quality": 3,
              "recommended_next_action": "DEEP_DIVE"
            }
            """;

    private static final String GENERATED_FOLLOW_UP_EXAMPLE = """
            {
              "skip": false,
              "skipReason": null,
              "answerText": "원문 그대로 복사",
              "target_claim_idx": 0,
              "selected_perspective": null,
              "question": "키워드를 녹인 후속 질문",
              "ttsQuestion": "TTS 변환된 질문",
              "reason": "선택 근거 한 줄",
              "type": "DEEP_DIVE",
              "best_answer": "참고 답변 2~4문장"
            }
            """;

    private static final String GENERATED_SESSION_FEEDBACK_EXAMPLE = """
            {
              "overall": {
                "dimension_scores": {"problem_framing": 2.5},
                "level_assessment": "주니어 기대치 충족",
                "narrative": "CS 개념에서 탄탄하지만 경험 질문에서 구체화 부족",
                "coverage": "all turns scored"
              },
              "strengths": [
                {"dimension": "problem_framing", "observation": "turn 1에서 명확", "why_matters": "소통"}
              ],
              "gaps": [
                {"dimension": "technical_depth", "observation": "turn 2 근거 부족",
                 "level_gap": "미드 미달", "concrete_action": "CS 기초 복습"}
              ],
              "delivery": {"filler_words": "없음", "tone_pattern": "안정", "action": "유지"},
              "week_plan": [
                {"priority": 1, "topic": "자료구조", "resources": ["CTCI"], "practice": "1문제"}
              ]
            }
            """;

    private static final String GENERATED_COMPACTION_SUMMARY_EXAMPLE = """
            {
              "covered_topics": ["GC"],
              "key_insights": ["STW 발생 시 처리량 손실"],
              "progression": ["기본 → 트레이드오프"],
              "perspectives_covered": ["TRADEOFF"],
              "open_questions": []
            }
            """;

    private final Map<Class<?>, String> examples = Map.of(
            GeneratedAnswerAnalysis.class, ANSWER_ANALYSIS_EXAMPLE,
            GeneratedFollowUp.class, GENERATED_FOLLOW_UP_EXAMPLE,
            GeneratedSessionFeedback.class, GENERATED_SESSION_FEEDBACK_EXAMPLE,
            GeneratedCompactionSummary.class, GENERATED_COMPACTION_SUMMARY_EXAMPLE
    );

    public String exampleFor(Class<?> clazz) {
        return examples.get(clazz);
    }
}
