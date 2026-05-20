package com.rehearse.api.infra.ai.schema;

import com.rehearse.api.infra.ai.dto.JsonSchemaSpec;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("infra/ai/schema 클래스들의 spec() 진입점 검증")
class AiSchemaSpecTest {

    @Test
    @DisplayName("GeneratedQuestionsWrapperSchema.spec() 는 NAME 과 build() 결과를 묶는다")
    void generatedQuestionsWrapperSchema_spec_combinesNameAndBuild() {
        JsonSchemaSpec spec = GeneratedQuestionsWrapperSchema.spec();

        assertThat(spec.name()).isEqualTo("generated_questions");
        assertThat(spec.schema()).containsEntry("additionalProperties", false);
        assertThat(spec.schema().get("required")).isEqualTo(List.of("questions"));
    }

    @Test
    @DisplayName("GeneratedFollowUpSchema.spec() 는 단일 NAME 으로 두 호출 site 가 공유한다")
    void generatedFollowUpSchema_spec_sharedName() {
        JsonSchemaSpec spec = GeneratedFollowUpSchema.spec();

        assertThat(spec.name()).isEqualTo("generated_follow_up");
        assertThat(spec.schema()).containsEntry("additionalProperties", false);
    }

    @Test
    @DisplayName("GeneratedSessionFeedbackSchema.spec() name = session_feedback")
    void generatedSessionFeedbackSchema_spec() {
        JsonSchemaSpec spec = GeneratedSessionFeedbackSchema.spec();

        assertThat(spec.name()).isEqualTo("session_feedback");
        assertThat(spec.schema().get("required"))
                .isEqualTo(List.of("overall", "strengths", "gaps", "delivery", "week_plan"));
    }

    @Test
    @DisplayName("GeneratedResumeQuestionsSchema.spec() name = generated_resume_questions")
    void generatedResumeQuestionsSchema_spec() {
        JsonSchemaSpec spec = GeneratedResumeQuestionsSchema.spec();

        assertThat(spec.name()).isEqualTo("generated_resume_questions");
        assertThat(spec.schema().get("required")).isEqualTo(List.of("openers", "mains"));
    }

    @Test
    @DisplayName("GeneratedAnswerAnalysisSchema.spec() name = answer_analysis")
    void generatedAnswerAnalysisSchema_spec() {
        JsonSchemaSpec spec = GeneratedAnswerAnalysisSchema.spec();

        assertThat(spec.name()).isEqualTo("answer_analysis");
        assertThat(spec.schema()).containsEntry("additionalProperties", false);
    }

    @Test
    @DisplayName("GeneratedRubricScoringSchema.spec(dims) 은 dimension 리스트를 required 로 포함한다")
    void generatedRubricScoringSchema_spec_withDimensions() {
        List<String> dims = List.of("technical_depth", "reasoning_communication");
        JsonSchemaSpec spec = GeneratedRubricScoringSchema.spec(dims);

        assertThat(spec.name()).isEqualTo("rubric_score");
        assertThat(spec.schema().get("required")).isEqualTo(dims);
    }
}
