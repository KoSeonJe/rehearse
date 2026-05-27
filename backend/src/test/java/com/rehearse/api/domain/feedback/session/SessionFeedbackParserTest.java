package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackParser;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class SessionFeedbackParserTest {

    private SessionFeedbackParser parser;

    @BeforeEach
    void setUp() {
        parser = new SessionFeedbackParser(new ObjectMapper());
    }

    private SessionFeedbackInput emptyInput() {
        return new SessionFeedbackInput(
                metadata(), Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), null, null, null, null,
                "all turns scored", InterviewLevel.MID
        );
    }

    private SessionFeedbackInput.SessionMetadata metadata() {
        return new SessionFeedbackInput.SessionMetadata(
                1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 0, 30);
    }

    private SessionFeedbackInput inputWithCategories(String... categories) {
        Map<String, Map<String, Double>> scores = new java.util.LinkedHashMap<>();
        for (String cat : categories) {
            scores.put(cat, Map.of("problem_framing", 2.5));
        }
        return new SessionFeedbackInput(
                metadata(), Collections.emptyList(), scores,
                Collections.emptyList(), null, null, null, null,
                "all turns scored", InterviewLevel.MID
        );
    }

    private String validPayloadJson(String narrative) {
        return """
                {
                  "overall": {
                    "dimension_scores": {"problem_framing": 2.5},
                    "level_assessment": "주니어 기대치 충족",
                    "narrative": "%s",
                    "coverage": "all turns scored"
                  },
                  "strengths": [
                    {"dimension": "problem_framing", "observation": "turn 1에서 명확한 설명", "why_matters": "소통 능력"}
                  ],
                  "gaps": [
                    {
                      "dimension": "technical_depth",
                      "observation": "turn 2에서 근거 부족",
                      "level_gap": "미드 레벨 기대치 미달",
                      "concrete_action": "CS 기초 복습 후 예제 코드 작성"
                    }
                  ],
                  "delivery": {"filler_words": "없음", "tone_pattern": "안정적", "action": "유지"},
                  "week_plan": [
                    {"priority": 1, "topic": "자료구조", "resources": ["CTCI"], "practice": "매일 1문제"}
                  ]
                }
                """.formatted(narrative);
    }

    // --- 기존 테스트 (회귀 방지) ---

    @Test
    @DisplayName("추상 표현 '더 공부하세요' 포함 시 SessionFeedbackParseException 발생")
    void parse_throwsException_when_abstractPhrase_found() {
        String json = validPayloadJson("전반적으로 좋습니다. 더 공부하세요.");
        assertThatThrownBy(() -> parser.parse(json, emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("추상 표현 감지");
    }

    @Test
    @DisplayName("추상 표현 없는 유효한 JSON은 정상 파싱 (false-positive 회귀)")
    void parse_succeeds_when_no_abstract_phrases() {
        String json = validPayloadJson("CS 개념에선 conceptual_accuracy 평균 2.9로 탄탄하지만 경험 질문에서 구체화 부족한 패턴.");
        assertThatCode(() -> parser.parse(json, emptyInput()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("잘못된 JSON 구조는 SessionFeedbackParseException 발생")
    void parse_throwsException_when_invalid_json() {
        assertThatThrownBy(() -> parser.parse("{invalid json}", emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class);
    }

    // --- F5-4: 5섹션 cardinality ---

    @Test
    @DisplayName("overall 누락 시 section=overall 에러 발생")
    void parse_throwsException_when_overall_missing() {
        String json = """
                {
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"없음","tone_pattern":"안정","action":"유지"},
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=overall");
    }

    @Test
    @DisplayName("strengths 빈 배열 시 section=strengths 에러 발생")
    void parse_throwsException_when_strengths_empty() {
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"없음","tone_pattern":"안정","action":"유지"},
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=strengths");
    }

    @Test
    @DisplayName("week_plan 누락 시 section=week_plan 에러 발생")
    void parse_throwsException_when_week_plan_missing() {
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"없음","tone_pattern":"안정","action":"유지"}
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=week_plan");
    }

    // --- F5-5: cross-category 검증 ---

    @Test
    @DisplayName("scoresByCategory 2개 이상이면 narrative에 카테고리명 2개 이상 포함 필요")
    void parse_throwsException_when_cross_category_missing_in_narrative() {
        SessionFeedbackInput input = inputWithCategories("CS 개념", "경험 질문");
        String json = validPayloadJson("전반적으로 좋은 면접이었습니다.");
        assertThatThrownBy(() -> parser.parse(json, input))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=overall.narrative cross-category");
    }

    @Test
    @DisplayName("narrative에 카테고리명 2개 이상 포함 시 cross-category 검증 통과")
    void parse_succeeds_when_narrative_contains_two_category_names() {
        SessionFeedbackInput input = inputWithCategories("CS 개념", "경험 질문");
        String json = validPayloadJson("CS 개념에서 탄탄하지만 경험 질문에서 구체화 부족한 패턴입니다.");
        assertThatCode(() -> parser.parse(json, input))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("scoresByCategory 1개면 cross-category 검증 skip")
    void parse_skips_cross_category_validation_when_single_category() {
        SessionFeedbackInput input = inputWithCategories("CS 개념");
        String json = validPayloadJson("전반적으로 좋은 면접이었습니다.");
        assertThatCode(() -> parser.parse(json, input))
                .doesNotThrowAnyException();
    }

    // --- F5-3: 단어 경계 dimension 매칭 ---

    @Test
    @DisplayName("delivery 섹션에 D1 차원 코드 포함 시 예외 발생 (단어 경계 매칭)")
    void parse_throwsException_when_delivery_contains_dimension_code() {
        SessionFeedbackInput inputWithDelivery = new SessionFeedbackInput(
                metadata(), Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), "빠른 말투", null, null, null,
                "all turns scored", InterviewLevel.MID
        );
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"D1 차원에서 문제","tone_pattern":"안정","action":"유지"},
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, inputWithDelivery))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=delivery");
    }

    @Test
    @DisplayName("delivery 섹션에 XD1 같은 부분 문자열은 차원 코드로 오탐하지 않는다")
    void parse_does_not_false_positive_on_partial_dimension_match() {
        SessionFeedbackInput inputWithDelivery = new SessionFeedbackInput(
                metadata(), Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), "빠른 말투", null, null, null,
                "all turns scored", InterviewLevel.MID
        );
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"XD1처럼 들림","tone_pattern":"안정","action":"유지"},
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatCode(() -> parser.parse(json, inputWithDelivery))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("nonverbalAggregate만 있어도 delivery 섹션이 누락되면 예외 발생")
    void parse_throwsException_when_nonverbalAggregate_exists_but_delivery_missing() {
        SessionFeedbackInput inputWithNonverbalAggregate = inputWithNonverbalAggregate();
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": null,
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, inputWithNonverbalAggregate))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=delivery");
    }

    @Test
    @DisplayName("nonverbalAggregate만 있어도 delivery 섹션의 D11~D14 내부 차원 코드 노출은 예외 발생")
    void parse_throwsException_when_nonverbalOnlyDeliveryContainsDimensionCode() {
        SessionFeedbackInput inputWithNonverbalAggregate = inputWithNonverbalAggregate();
        String json = """
                {
                  "overall": {"dimension_scores":{"problem_framing":2.5},"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"dimension":"problem_framing","observation":"turn 1","why_matters":"ok"}],
                  "gaps": [{"dimension":"technical_depth","observation":"turn 2","level_gap":"x","concrete_action":"y"}],
                  "delivery": {"filler_words":"양호","tone_pattern":"D13이 낮음","action":"카메라 보기"},
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;

        assertThatThrownBy(() -> parser.parse(json, inputWithNonverbalAggregate))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=delivery Rubric 차원 코드");
    }

    private SessionFeedbackInput inputWithNonverbalAggregate() {
        return new SessionFeedbackInput(
                metadata(), Collections.emptyList(), Collections.emptyMap(),
                Collections.emptyList(), null, null,
                new SessionFeedbackInput.NonverbalDeliveryAggregate(
                        "nonverbal_score",
                        List.of(new SessionFeedbackInput.NonverbalTurnAggregate(
                                "1-1", Map.of("fluency", 2, "confidence_tone", 3, "eye_contact_posture", 1, "composure", 2), 1.0
                        )),
                        Map.of("fluency", 2.0, "confidence_tone", 3.0, "eye_contact_posture", 1.0, "composure", 2.0),
                        new SessionFeedbackInput.LowestDimension("eye_contact_posture", 1.0),
                        1.0,
                        List.of(new SessionFeedbackInput.RecommendedAction(
                                "eye_contact_posture", List.of("카메라를 보고 말하기")
                        ))
                ),
                null,
                "all turns scored", InterviewLevel.MID
        );
    }
}
