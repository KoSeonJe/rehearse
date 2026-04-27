package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class SchemaExampleRegistryTest {

    private final SchemaExampleRegistry registry = new SchemaExampleRegistry();

    @Test
    @DisplayName("AnswerAnalysis 스키마 예시는 claims 객체 배열 형태를 명시한다")
    void answerAnalysis_exampleShowsClaimsObjectShape() {
        String example = registry.exampleFor(AnswerAnalysis.class);

        assertThat(example).isNotNull();
        assertThat(example).contains("\"claims\"");
        assertThat(example).contains("\"text\"");
        assertThat(example).contains("\"depth_score\"");
        assertThat(example).contains("\"evidence_strength\"");
        assertThat(example).contains("\"topic_tag\"");
    }

    @Test
    @DisplayName("GeneratedFollowUp 스키마 예시는 question/type 등 필수 필드를 포함한다")
    void generatedFollowUp_exampleShowsRequiredFields() {
        String example = registry.exampleFor(GeneratedFollowUp.class);

        assertThat(example).isNotNull();
        assertThat(example).contains("\"question\"");
        assertThat(example).contains("\"type\"");
    }

    @Test
    @DisplayName("등록되지 않은 클래스는 null 반환")
    void unknownClass_returnsNull() {
        String example = registry.exampleFor(Object.class);

        assertThat(example).isNull();
    }

    @Test
    @DisplayName("withSchemaRetryHint(violation, schemaExample) 은 user 메시지에 예시 JSON 을 첨부한다")
    void withSchemaRetryHint_appendsSchemaExampleToUserMessage() {
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "원본")))
                .callType("answer_analyzer")
                .build();
        String example = registry.exampleFor(AnswerAnalysis.class);

        ChatRequest retryReq = req.withSchemaRetryHint("Cannot construct instance of Claim", example);

        assertThat(retryReq.messages()).hasSize(2);
        String hint = retryReq.messages().get(1).content();
        assertThat(hint).contains("이전 응답이 JSON 스키마를 위반했습니다");
        assertThat(hint).contains("Cannot construct instance of Claim");
        assertThat(hint).contains("\"claims\"");
        assertThat(hint).contains("\"depth_score\"");
    }

    @Test
    @DisplayName("schemaExample 이 null 이면 단순 hint 만 첨부 (legacy 호환)")
    void withSchemaRetryHint_nullExample_legacyHintOnly() {
        ChatRequest req = ChatRequest.builder()
                .messages(List.of(ChatMessage.of(ChatMessage.Role.USER, "원본")))
                .callType("answer_analyzer")
                .build();

        ChatRequest retryReq = req.withSchemaRetryHint("violation", null);

        assertThat(retryReq.messages()).hasSize(2);
        String hint = retryReq.messages().get(1).content();
        assertThat(hint).contains("violation");
        assertThat(hint).doesNotContain("```json");
    }
}
