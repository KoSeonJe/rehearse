package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.rubric.entity.RubricDimension;
import com.rehearse.api.domain.feedback.rubric.service.RubricLoader;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricCategory;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@ActiveProfiles("test")
@DisplayName("RubricLoader")
class RubricLoaderTest extends AbstractMySqlContainerTest {

    @Autowired
    private RubricLoader rubricLoader;

    private Interview standardInterview;
    private Interview resumeInterview;

    @BeforeEach
    void setUp() {
        standardInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        resumeInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .build();
    }

    @ParameterizedTest(name = "category={0}")
    @EnumSource(InterviewType.class)
    @DisplayName("모든 InterviewType 에 대해 매핑 실패 없음 (standard track)")
    void resolveFor_allCategories_neverNull(InterviewType category) {
        QuestionSet questionSet = buildQuestionSet(category);
        Question question = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(question, questionSet, standardInterview);

        assertThat(rubric).isNotNull();
        assertThat(rubric.rubricId()).isNotBlank();
    }

    @ParameterizedTest(name = "category={0}")
    @EnumSource(InterviewType.class)
    @DisplayName("Resume Track은 category 관계없이 resume-v1 매핑")
    void resolveFor_resumeTrack_alwaysResumeRubric(InterviewType category) {
        QuestionSet questionSet = buildQuestionSet(category);
        Question question = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(question, questionSet, resumeInterview);

        assertThat(rubric.rubricId()).isEqualTo("resume-v1");
    }

    @Test
    @DisplayName("CS_FUNDAMENTAL → concept-cs-fundamental-v1 매핑")
    void resolveFor_csFundamental_correctRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.CS_FUNDAMENTAL);
        Question q = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-cs-fundamental-v1");
    }

    @Test
    @DisplayName("LANGUAGE_FRAMEWORK → concept-lang-framework-v1 매핑")
    void resolveFor_languageFramework_correctRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.LANGUAGE_FRAMEWORK);
        Question q = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-lang-framework-v1");
    }

    @Test
    @DisplayName("UI_FRAMEWORK → concept-lang-framework-v1 매핑")
    void resolveFor_uiFramework_correctRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.UI_FRAMEWORK);
        Question q = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-lang-framework-v1");
    }

    @Test
    @DisplayName("BEHAVIORAL → experience-collaboration-v1 매핑")
    void resolveFor_behavioral_correctRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.BEHAVIORAL);
        Question q = buildQuestion(RubricCategory.BEHAVIORAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-collaboration-v1");
    }

    @Test
    @DisplayName("SYSTEM_DESIGN → fallback-generic-v1 매핑")
    void resolveFor_systemDesign_fallbackRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.SYSTEM_DESIGN);
        Question q = buildQuestion(RubricCategory.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("fallback-generic-v1");
    }

    @Test
    @DisplayName("RESUME_OPENER (EXPERIENCE perspective) + standard interview + SYSTEM_DESIGN → experience-technical-v1 매핑")
    void resolveFor_experiencePerspective_correctRubric() {
        QuestionSet qs = buildQuestionSet(InterviewType.SYSTEM_DESIGN);
        Question q = buildQuestionWithType(QuestionType.RESUME_OPENER);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-technical-v1");
    }

    @Test
    @DisplayName("TECH_MAIN + CS 카테고리 → enum perspective TECHNICAL 매핑 (P0-2 회귀)")
    void resolveFor_techMain_csCategory_usesEnumPerspective() {
        QuestionSet qs = buildQuestionSet(InterviewType.CS_FUNDAMENTAL);
        Question q = buildQuestionWithType(QuestionType.TECH_MAIN);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-cs-fundamental-v1");
    }

    @Test
    @DisplayName("BEHAVIORAL_MAIN + BEHAVIORAL 카테고리 → enum perspective BEHAVIORAL 매핑 (P0-2 회귀)")
    void resolveFor_behavioralMain_behavioralCategory_usesEnumPerspective() {
        QuestionSet qs = buildQuestionSet(InterviewType.BEHAVIORAL);
        Question q = buildQuestionWithType(QuestionType.BEHAVIORAL_MAIN);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-collaboration-v1");
    }

    @Test
    @DisplayName("BEHAVIORAL_MAIN + BEHAVIORAL 카테고리 → experience-collaboration-v1 매핑")
    void resolveFor_behavioralMain_behavioralCategory_resolvesExperience() {
        QuestionSet qs = buildQuestionSet(InterviewType.BEHAVIORAL);
        Question q = buildQuestionWithType(QuestionType.BEHAVIORAL_MAIN);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-collaboration-v1");
    }

    @Test
    @DisplayName("D1~D13 차원 모두 로드되고 composure 차원은 부재")
    void getAllDimensions_allThirteenLoaded_noComposure() {
        var dimensions = rubricLoader.getAllDimensions();

        assertThat(dimensions).containsKeys(
                "problem_framing", "technical_depth", "reasoning_communication", "conceptual_accuracy",
                "practical_application", "experience_concreteness", "collaboration_awareness",
                "recovery_from_gaps", "factual_consistency", "chain_depth",
                "fluency", "confidence_tone", "eye_contact_posture");
        assertThat(dimensions).doesNotContainKey("composure");
    }

    @Test
    @DisplayName("getDimension 으로 개별 차원 조회")
    void getDimension_returnsCorrectDimension() {
        RubricDimension d2 = rubricLoader.getDimension("technical_depth");

        assertThat(d2).isNotNull();
        assertThat(d2.name()).isEqualTo("기술 깊이");
        assertThat(d2.scoring()).containsKeys(1, 2, 3);
    }

    @Test
    @DisplayName("D11~D13 은 비언어 채점 차원으로 로드되고 composure 는 부재")
    void getAllDimensions_nonverbalDimensionsLoaded_noComposure() {
        assertThat(rubricLoader.getDimension("fluency").name()).isEqualTo("유창함");
        assertThat(rubricLoader.getDimension("confidence_tone").name()).isEqualTo("자신감");
        assertThat(rubricLoader.getDimension("eye_contact_posture").name()).isEqualTo("시선");
        assertThat(rubricLoader.getDimension("composure")).isNull();
    }

    @Test
    @DisplayName("fluency description/measurement/observable 가 더듬·끊김·속도 신호 흡수 표현 포함")
    void fluency_yamlBody_absorbsPaceAndStumbleSignals() {
        RubricDimension fluency = rubricLoader.getDimension("fluency");

        assertThat(fluency).isNotNull();
        String combined = fluency.description() + " " + extractMeasurement("fluency") + " " + flattenObservables(fluency);
        assertThat(combined).contains("더듬");
        assertThat(combined).contains("끊");
        assertThat(combined).contains("속도");
    }

    @Test
    @DisplayName("confidence_tone measurement 가 톤과 발화 속도 신호 흡수 표현 포함")
    void confidenceTone_measurement_absorbsToneAndSpeedSignals() {
        String measurement = extractMeasurement("confidence_tone");

        assertThat(measurement).contains("톤");
        assertThat(measurement).contains("속도");
    }

    @Test
    @DisplayName("eye_contact_posture description/measurement 가 시선·자세·표정 신호 흡수 표현 포함")
    void eyeContactPosture_descriptionMeasurement_absorbsGazePostureExpressionSignals() {
        RubricDimension dim = rubricLoader.getDimension("eye_contact_posture");

        assertThat(dim).isNotNull();
        String combined = dim.description() + " " + extractMeasurement("eye_contact_posture");
        assertThat(combined).contains("시선");
        assertThat(combined).contains("자세");
        assertThat(combined).contains("표정");
    }

    private String flattenObservables(RubricDimension dim) {
        StringBuilder sb = new StringBuilder();
        for (var level : dim.scoring().values()) {
            for (String line : level.observable()) {
                sb.append(line).append(' ');
            }
        }
        return sb.toString();
    }

    @Test
    @DisplayName("nonverbal-v1 YAML 은 3차원 단일 default 규칙 + composure 미참조")
    @SuppressWarnings("unchecked")
    void nonverbalRubricYaml_threeDimensionsOnly_noComposure() throws java.io.IOException {
        java.util.Map<String, Object> data;
        try (var is = getClass().getResourceAsStream("/rubric/nonverbal-rubric.yaml")) {
            data = new org.yaml.snakeyaml.Yaml().load(is);
        }

        java.util.List<java.util.Map<String, Object>> usesDims =
                (java.util.List<java.util.Map<String, Object>>) data.get("uses_dimensions");
        assertThat(usesDims).hasSize(3);
        assertThat(usesDims).extracting(m -> m.get("ref"))
                .containsExactlyInAnyOrder("fluency", "confidence_tone", "eye_contact_posture");
        assertThat(usesDims).noneMatch(m -> "composure".equals(m.get("ref")));

        java.util.Map<String, Object> perTurnRules =
                (java.util.Map<String, Object>) data.get("per_turn_rules");
        assertThat(perTurnRules).containsOnlyKeys("default");
        assertThat((java.util.List<String>) perTurnRules.get("default"))
                .containsExactly("fluency", "confidence_tone", "eye_contact_posture");
        assertThat(perTurnRules).doesNotContainKey("medium_or_hard");
    }

    @SuppressWarnings("unchecked")
    private String extractMeasurement(String dimensionKey) {
        try (var is = getClass().getResourceAsStream("/rubric/_dimensions.yaml")) {
            java.util.Map<String, Object> data = new org.yaml.snakeyaml.Yaml().load(is);
            java.util.Map<String, Object> dims = (java.util.Map<String, Object>) data.get("dimensions");
            java.util.Map<String, Object> dim = (java.util.Map<String, Object>) dims.get(dimensionKey);
            return String.valueOf(dim.get("measurement"));
        } catch (java.io.IOException e) {
            throw new IllegalStateException("_dimensions.yaml 로드 실패", e);
        }
    }

    private QuestionSet buildQuestionSet(InterviewType category) {
        Interview tempInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        return QuestionSet.builder()
                .interview(tempInterview)
                .category(category)
                .orderIndex(0)
                .build();
    }

    private Question buildQuestion(RubricCategory perspective) {
        QuestionType type = perspective == RubricCategory.BEHAVIORAL
                ? QuestionType.BEHAVIORAL_MAIN
                : QuestionType.TECH_MAIN;
        return Question.builder()
                .questionType(type)
                .questionText("테스트 질문")
                .orderIndex(0)
                .build();
    }

    private Question buildQuestionWithType(QuestionType questionType) {
        return Question.builder()
                .questionType(questionType)
                .questionText("테스트 질문")
                .orderIndex(0)
                .build();
    }
}
