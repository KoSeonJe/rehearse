package com.rehearse.api.domain.feedback.rubric;

import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
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
class RubricLoaderTest {

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
    @EnumSource(QuestionSetCategory.class)
    @DisplayName("모든 QuestionSetCategory 에 대해 매핑 실패 없음 (standard track)")
    void resolveFor_allCategories_neverNull(QuestionSetCategory category) {
        QuestionSet questionSet = buildQuestionSet(category);
        Question question = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(question, questionSet, standardInterview);

        assertThat(rubric).isNotNull();
        assertThat(rubric.rubricId()).isNotBlank();
    }

    @ParameterizedTest(name = "category={0}")
    @EnumSource(QuestionSetCategory.class)
    @DisplayName("Resume Track은 category 관계없이 resume-v1 매핑")
    void resolveFor_resumeTrack_alwaysResumeRubric(QuestionSetCategory category) {
        QuestionSet questionSet = buildQuestionSet(category);
        Question question = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(question, questionSet, resumeInterview);

        assertThat(rubric.rubricId()).isEqualTo("resume-v1");
    }

    @Test
    @DisplayName("CS_FUNDAMENTAL → concept-cs-fundamental-v1 매핑")
    void resolveFor_csFundamental_correctRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.CS_FUNDAMENTAL);
        Question q = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-cs-fundamental-v1");
    }

    @Test
    @DisplayName("LANGUAGE_FRAMEWORK → concept-lang-framework-v1 매핑")
    void resolveFor_languageFramework_correctRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.LANGUAGE_FRAMEWORK);
        Question q = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-lang-framework-v1");
    }

    @Test
    @DisplayName("UI_FRAMEWORK → concept-lang-framework-v1 매핑")
    void resolveFor_uiFramework_correctRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.UI_FRAMEWORK);
        Question q = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("concept-lang-framework-v1");
    }

    @Test
    @DisplayName("BEHAVIORAL → experience-collaboration-v1 매핑")
    void resolveFor_behavioral_correctRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.BEHAVIORAL);
        Question q = buildQuestion(FeedbackPerspective.BEHAVIORAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-collaboration-v1");
    }

    @Test
    @DisplayName("SYSTEM_DESIGN → fallback-generic-v1 매핑")
    void resolveFor_systemDesign_fallbackRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.SYSTEM_DESIGN);
        Question q = buildQuestion(FeedbackPerspective.TECHNICAL);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("fallback-generic-v1");
    }

    @Test
    @DisplayName("FeedbackPerspective=EXPERIENCE → experience-technical-v1 매핑")
    void resolveFor_experiencePerspective_correctRubric() {
        QuestionSet qs = buildQuestionSet(QuestionSetCategory.SYSTEM_DESIGN);
        Question q = buildQuestion(FeedbackPerspective.EXPERIENCE);

        Rubric rubric = rubricLoader.resolveFor(q, qs, standardInterview);

        assertThat(rubric.rubricId()).isEqualTo("experience-technical-v1");
    }

    @Test
    @DisplayName("D1~D10 차원 모두 로드됨")
    void getAllDimensions_allTenLoaded() {
        var dimensions = rubricLoader.getAllDimensions();

        assertThat(dimensions).containsKeys("D1", "D2", "D3", "D4", "D5", "D6", "D7", "D8", "D9", "D10");
    }

    @Test
    @DisplayName("getDimension 으로 개별 차원 조회")
    void getDimension_returnsCorrectDimension() {
        RubricDimension d2 = rubricLoader.getDimension("D2");

        assertThat(d2).isNotNull();
        assertThat(d2.name()).isEqualTo("Technical Depth");
        assertThat(d2.scoring()).containsKeys(1, 2, 3);
    }

    private QuestionSet buildQuestionSet(QuestionSetCategory category) {
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

    private Question buildQuestion(FeedbackPerspective perspective) {
        return Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("테스트 질문")
                .feedbackPerspective(perspective)
                .orderIndex(0)
                .build();
    }
}
