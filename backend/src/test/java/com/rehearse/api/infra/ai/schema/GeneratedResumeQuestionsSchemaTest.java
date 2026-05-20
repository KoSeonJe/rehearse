package com.rehearse.api.infra.ai.schema;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("GeneratedResumeQuestionsSchema — opener / main item 분리 schema")
class GeneratedResumeQuestionsSchemaTest {

    @Test
    @DisplayName("mains item 은 depth_type 을 required + enum 5종 으로 강제한다")
    void mainsItem_requires_depthType_withEnum() {
        Map<String, Object> mainItem = mainItem();

        assertThat(mainItem).containsEntry("type", "object");
        assertThat(mainItem).containsEntry("additionalProperties", false);
        assertThat(mainItem.get("required"))
                .isEqualTo(List.of("question", "tts_question", "best_answer", "depth_type"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) mainItem.get("properties");
        @SuppressWarnings("unchecked")
        Map<String, Object> depthType = (Map<String, Object>) props.get("depth_type");
        assertThat(depthType).containsEntry("type", "string");
        assertThat(depthType.get("enum"))
                .isEqualTo(List.of("TRADEOFF", "LIMITATION", "QUANTITATIVE", "ALTERNATIVE", "PRINCIPLE"));
    }

    @Test
    @DisplayName("openers item 은 depth_type 미포함 — opener 는 깊이 유형 없음")
    void openersItem_excludes_depthType() {
        Map<String, Object> openerItem = openerItem();

        assertThat(openerItem.get("required"))
                .isEqualTo(List.of("question", "tts_question", "best_answer"));

        @SuppressWarnings("unchecked")
        Map<String, Object> props = (Map<String, Object>) openerItem.get("properties");
        assertThat(props).doesNotContainKey("depth_type");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> mainItem() {
        Map<String, Object> schema = GeneratedResumeQuestionsSchema.build();
        Map<String, Object> rootProps = (Map<String, Object>) schema.get("properties");
        Map<String, Object> mains = (Map<String, Object>) rootProps.get("mains");
        return (Map<String, Object>) mains.get("items");
    }

    @SuppressWarnings("unchecked")
    private static Map<String, Object> openerItem() {
        Map<String, Object> schema = GeneratedResumeQuestionsSchema.build();
        Map<String, Object> rootProps = (Map<String, Object>) schema.get("properties");
        Map<String, Object> openers = (Map<String, Object>) rootProps.get("openers");
        return (Map<String, Object>) openers.get("items");
    }
}
