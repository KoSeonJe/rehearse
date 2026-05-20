package com.rehearse.api.domain.interview.service;

import com.rehearse.api.infra.ai.schema.GeneratedFollowUpSchema;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedFollowUpSchema — strict JSON Schema 정의 검증")
class FollowUpQuestionWriterSchemaTest {

    @Test
    @DisplayName("build() 는 9개 필드 required + nullable 필드는 type 배열로 표현한다")
    void buildJsonSchema_requiresAllFields_withNullableTypes() {
        @SuppressWarnings("unchecked")
        Map<String, Object> schema = (Map<String, Object>) GeneratedFollowUpSchema.build();

        assertThat(schema).containsEntry("type", "object");
        assertThat(schema).containsEntry("additionalProperties", false);
        assertThat(schema.get("required")).isEqualTo(List.of(
                "skip", "skip_reason", "question", "tts_question",
                "reason", "type", "best_answer", "answer_text", "target_claim_idx"));

        @SuppressWarnings("unchecked")
        Map<String, Object> properties = (Map<String, Object>) schema.get("properties");

        @SuppressWarnings("unchecked")
        Map<String, Object> skip = (Map<String, Object>) properties.get("skip");
        assertThat(skip.get("type")).isEqualTo("boolean");

        @SuppressWarnings("unchecked")
        Map<String, Object> question = (Map<String, Object>) properties.get("question");
        assertThat(question.get("type")).isEqualTo(List.of("string", "null"));

        @SuppressWarnings("unchecked")
        Map<String, Object> targetClaimIdx = (Map<String, Object>) properties.get("target_claim_idx");
        assertThat(targetClaimIdx.get("type")).isEqualTo(List.of("integer", "null"));
    }
}
