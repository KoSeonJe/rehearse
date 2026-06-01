package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.TurnScoreView;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SessionFeedbackSynthesizerPromptBuilderTest {

    private SessionFeedbackSynthesizerPromptBuilder builder;

    @BeforeEach
    void setUp() throws Exception {
        builder = new SessionFeedbackSynthesizerPromptBuilder(
                new DefaultResourceLoader(),
                new ObjectMapper()
        );
        builder.init();
    }

    @Test
    @DisplayName("세션 메타데이터와 delivery 분석을 프롬프트 placeholder 로 직렬화한다")
    void build_serializesMetadataAndDelivery() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                Collections.emptyList(),
                "{\"vocal\":{\"speechPace\":\"fast\"}}",
                null,
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("\"position\":\"BACKEND\"");
        assertThat(prompt).contains("\"speechPace\":\"fast\"");
        assertThat(prompt).doesNotContain("dimension_scores");
        assertThat(prompt).doesNotContain("NONVERBAL_AGGREGATE");
        assertThat(prompt).doesNotContain("SCORES_BY_CATEGORY");
        assertThat(pair.system()).contains("JSON");
    }

    @Test
    @DisplayName("turnScores 직렬화에 코멘트 필드와 친화 표기 turnLabel 이 노출된다")
    void build_serializesTurnScoreComments() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                List.of(new TurnScoreView(
                        "2-1",
                        "답변이 명확함",
                        "[]",
                        "STAR 구조",
                        "근거 보강 필요",
                        "시선 흔들림",
                        "발화 빠름",
                        "자세 양호",
                        "종합 코멘트"
                )),
                null,
                null,
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("\"turnLabel\":\"2-1\"");
        assertThat(prompt).contains("\"verbalComment\":\"답변이 명확함\"");
        assertThat(prompt).contains("\"coachingStructure\":\"STAR 구조\"");
        assertThat(prompt).doesNotContain("\"turnId\"");
    }

    @Test
    @DisplayName("delivery/vision 분석이 없으면 placeholder 는 null 로 유지된다")
    void build_keepsNullForMissingDeliveryInput() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                Collections.emptyList(),
                null,
                null,
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("### Delivery Analysis (Lambda");
        assertThat(prompt).contains("### Vision Analysis");
    }

    private SessionFeedbackInput.SessionMetadata metadata() {
        return new SessionFeedbackInput.SessionMetadata(
                1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 2, 30);
    }
}
