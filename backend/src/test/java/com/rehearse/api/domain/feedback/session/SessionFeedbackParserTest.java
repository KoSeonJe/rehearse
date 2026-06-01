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
                metadata(), Collections.emptyList(), null, null,
                "all turns scored", InterviewLevel.MID
        );
    }

    private SessionFeedbackInput inputWithDelivery() {
        return new SessionFeedbackInput(
                metadata(), Collections.emptyList(), "빠른 말투", null,
                "all turns scored", InterviewLevel.MID
        );
    }

    private SessionFeedbackInput.SessionMetadata metadata() {
        return new SessionFeedbackInput.SessionMetadata(
                1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 0, 30);
    }

    private String validPayloadJson(String narrative) {
        return """
                {
                  "overall": {
                    "level_assessment": "주니어 기대치 충족",
                    "narrative": "%s",
                    "coverage": "all turns scored"
                  },
                  "strengths": [
                    {"observation": "1-1 답변에서 명확한 설명", "why_matters": "소통 능력"}
                  ],
                  "gaps": [
                    {"observation": "2-1 답변에서 근거 부족", "concrete_action": "CS 기초 복습 후 예제 코드 작성"}
                  ],
                  "delivery": {"filler_words": "없음", "tone_pattern": "안정적", "action": "유지"},
                  "week_plan": [
                    {"priority": 1, "topic": "자료구조", "resources": ["CTCI"], "practice": "매일 1문제"}
                  ]
                }
                """.formatted(narrative);
    }

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
        String json = validPayloadJson("CS 개념에선 탄탄하지만 경험 질문에서 구체화 부족한 패턴.");
        assertThatCode(() -> parser.parse(json, emptyInput()))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("잘못된 JSON 구조는 SessionFeedbackParseException 발생")
    void parse_throwsException_when_invalid_json() {
        assertThatThrownBy(() -> parser.parse("{invalid json}", emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class);
    }

    @Test
    @DisplayName("overall 누락 시 section=overall 에러 발생")
    void parse_throwsException_when_overall_missing() {
        String json = """
                {
                  "strengths": [{"observation":"1-1 답변","why_matters":"ok"}],
                  "gaps": [{"observation":"2-1 답변","concrete_action":"y"}],
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
                  "overall": {"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [],
                  "gaps": [{"observation":"2-1 답변","concrete_action":"y"}],
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
                  "overall": {"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"observation":"1-1 답변","why_matters":"ok"}],
                  "gaps": [{"observation":"2-1 답변","concrete_action":"y"}],
                  "delivery": {"filler_words":"없음","tone_pattern":"안정","action":"유지"}
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, emptyInput()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=week_plan");
    }

    @Test
    @DisplayName("delivery 입력이 있는데 payload.delivery 가 null 이면 section=delivery 에러 발생")
    void parse_throwsException_when_delivery_input_exists_but_section_missing() {
        String json = """
                {
                  "overall": {"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"observation":"1-1 답변","why_matters":"ok"}],
                  "gaps": [{"observation":"2-1 답변","concrete_action":"y"}],
                  "delivery": null,
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatThrownBy(() -> parser.parse(json, inputWithDelivery()))
                .isInstanceOf(SessionFeedbackParseException.class)
                .hasMessageContaining("section=delivery");
    }

    @Test
    @DisplayName("delivery 입력이 없으면 payload.delivery 가 null 이어도 정상 파싱")
    void parse_succeeds_when_no_delivery_input_and_section_null() {
        String json = """
                {
                  "overall": {"level_assessment":"ok","narrative":"좋음","coverage":"all"},
                  "strengths": [{"observation":"1-1 답변","why_matters":"ok"}],
                  "gaps": [{"observation":"2-1 답변","concrete_action":"y"}],
                  "delivery": null,
                  "week_plan": [{"priority":1,"topic":"자료구조","resources":["CTCI"],"practice":"1문제"}]
                }
                """;
        assertThatCode(() -> parser.parse(json, emptyInput()))
                .doesNotThrowAnyException();
    }
}
