package com.rehearse.api.infra.ai.prompt;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.infra.ai.schema.GeneratedSessionFeedbackSchema;
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

    @Test
    @DisplayName("buildJsonSchema() 는 overall.dimension_scores 14개 한국어 키를 required 로 포함하고 nullable number 로 정의한다")
    void buildJsonSchema_dimensionScores_requireAll14Keys_asNullableNumber() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema =
                (Map<String, Object>) GeneratedSessionFeedbackSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> overall = (Map<String, Object>) properties.get("overall");
        @SuppressWarnings("unchecked")
        Map<String, Object> overallProps = (Map<String, Object>) overall.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> dimensionScores = (Map<String, Object>) overallProps.get("dimension_scores");

        assertThat(dimensionScores).containsEntry("type", "object");
        assertThat(dimensionScores).containsEntry("additionalProperties", false);
        assertThat(dimensionScores.get("required"))
                .isEqualTo(List.copyOf(GeneratedSessionFeedbackSchema.DIMENSION_KEYS));

        @SuppressWarnings("unchecked")
        Map<String, Object> dimensionScoresProps =
                (Map<String, Object>) dimensionScores.get("properties");
        for (String key : GeneratedSessionFeedbackSchema.DIMENSION_KEYS) {
            @SuppressWarnings("unchecked")
            Map<String, Object> field = (Map<String, Object>) dimensionScoresProps.get(key);
            assertThat(field.get("type")).isEqualTo(List.of("number", "null"));
        }
    }

    @Test
    @DisplayName("buildJsonSchema() 의 delivery 는 anyOf 로 object 또는 null 을 허용한다 (모든 nested object 는 additionalProperties=false)")
    void buildJsonSchema_delivery_anyOfObjectOrNull() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema =
                (Map<String, Object>) GeneratedSessionFeedbackSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> delivery = (Map<String, Object>) properties.get("delivery");

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> anyOf = (List<Map<String, Object>>) delivery.get("anyOf");
        assertThat(anyOf).hasSize(2);

        Map<String, Object> deliveryObject = anyOf.get(0);
        assertThat(deliveryObject).containsEntry("type", "object");
        assertThat(deliveryObject).containsEntry("additionalProperties", false);
        assertThat(deliveryObject.get("required"))
                .isEqualTo(List.of("filler_words", "tone_pattern", "action"));

        Map<String, Object> nullOption = anyOf.get(1);
        assertThat(nullOption).containsEntry("type", "null");
    }

    @Test
    @DisplayName("buildJsonSchema() 의 strengths / gaps / week_plan 은 array<object> 이며 각 item 은 strict 객체이다")
    void buildJsonSchema_arrayItems_areStrictObjects() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema =
                (Map<String, Object>) GeneratedSessionFeedbackSchema.build();

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        assertStrictArrayItem(properties, "strengths",
                List.of("dimension", "observation", "why_matters"));
        assertStrictArrayItem(properties, "gaps",
                List.of("dimension", "observation", "level_gap", "concrete_action"));
        assertStrictArrayItem(properties, "week_plan",
                List.of("priority", "topic", "resources", "practice"));
    }

    @SuppressWarnings("unchecked")
    private static void assertStrictArrayItem(
            Map<String, Object> properties, String key, List<String> expectedRequired) {
        Map<String, Object> arr = (Map<String, Object>) properties.get(key);
        assertThat(arr).containsEntry("type", "array");
        Map<String, Object> item = (Map<String, Object>) arr.get("items");
        assertThat(item).containsEntry("type", "object");
        assertThat(item).containsEntry("additionalProperties", false);
        assertThat(item.get("required")).isEqualTo(expectedRequired);
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
