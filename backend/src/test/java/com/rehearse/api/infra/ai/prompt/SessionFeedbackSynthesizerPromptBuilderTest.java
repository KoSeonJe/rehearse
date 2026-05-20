package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.core.io.DefaultResourceLoader;

import java.util.Collections;
import java.util.List;
import java.util.Map;

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
    @DisplayName("typed SessionFeedbackInput을 프롬프트 placeholder JSON으로 직렬화한다")
    void build_serializesTypedInputToPromptJson() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList(),
                "{\"vocal\":{\"speechPace\":\"fast\"}}",
                null,
                aggregate(),
                null,
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("\"position\":\"BACKEND\"");
        assertThat(prompt).contains("\"source\":\"nonverbal_score\"");
        assertThat(prompt).contains("\"lowestDimension\":{\"dimension\":\"eye_contact_posture\",\"averageScore\":1.0}");
        assertThat(prompt).contains("\"recommendedActions\":[{\"dimension\":\"eye_contact_posture\"");
        assertThat(prompt).doesNotContain("legacyAggregate");
        assertThat(pair.system()).contains("JSON");
    }

    @Test
    @DisplayName("typed aggregate가 없고 legacy 문자열도 없으면 nonverbal placeholder는 null로 유지된다")
    void build_keepsNullForMissingNonverbalInput() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList(),
                null,
                null,
                null,
                null,
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("### Delivery Analysis (Lambda");
        assertThat(prompt).contains("### Nonverbal Aggregate\nnull");
    }

    @Test
    @DisplayName("DB aggregate가 없으면 legacy nonverbal JSON 문자열을 그대로 프롬프트에 넣는다")
    void build_usesLegacyNonverbalJsonWhenTypedAggregateIsMissing() {
        SessionFeedbackInput input = new SessionFeedbackInput(
                metadata(),
                Collections.emptyList(),
                Collections.emptyMap(),
                Collections.emptyList(),
                null,
                null,
                null,
                "{\"legacy\":\"aggregate\"}",
                "all turns scored",
                InterviewLevel.MID
        );

        SessionFeedbackSynthesizerPromptBuilder.PromptPair pair = builder.build(input);
        String prompt = pair.user();

        assertThat(prompt).contains("### Nonverbal Aggregate\n{\"legacy\":\"aggregate\"}");
    }

    private SessionFeedbackInput.SessionMetadata metadata() {
        return new SessionFeedbackInput.SessionMetadata(
                1L, "BACKEND", "MID", List.of("CS_FUNDAMENTAL"), 2, 30);
    }

    private SessionFeedbackInput.NonverbalDeliveryAggregate aggregate() {
        return new SessionFeedbackInput.NonverbalDeliveryAggregate(
                "nonverbal_score",
                List.of(new SessionFeedbackInput.NonverbalTurnAggregate(
                        1L, Map.of("fluency", 2, "confidence_tone", 3, "eye_contact_posture", 1, "composure", 2), 1.0
                )),
                Map.of("fluency", 2.0, "confidence_tone", 3.0, "eye_contact_posture", 1.0, "composure", 2.0),
                new SessionFeedbackInput.LowestDimension("eye_contact_posture", 1.0),
                1.0,
                List.of(new SessionFeedbackInput.RecommendedAction(
                        "eye_contact_posture", List.of("카메라를 보고 말하기")
                ))
        );
    }
}
