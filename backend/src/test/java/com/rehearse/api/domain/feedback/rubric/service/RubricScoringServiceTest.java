package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionRef;
import com.rehearse.api.domain.feedback.rubric.entity.Rubric;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.models.service.RubricScorer;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;

@ExtendWith(MockitoExtension.class)
@DisplayName("RubricScoringService - 무응답 가드 (3자 이하 답변은 LLM 호출 없이 NOT_EVALUABLE 반환)")
class RubricScoringServiceTest {

    private static final String RUBRIC_ID = "resume-v1";

    @Mock
    private RubricCatalog rubricCatalog;

    @Mock
    private RubricScorer rubricScorer;

    private RubricScoringService service;

    private Question question;
    private QuestionSet questionSet;
    private Interview interview;

    @BeforeEach
    void setUp() {
        service = new RubricScoringService(rubricCatalog, rubricScorer);

        question = TestFixtures.createResumeQuestion(QuestionType.RESUME_OPENER);
        ReflectionTestUtils.setField(question, "id", 42L);

        interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 99L);

        questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
    }

    @Nested
    @DisplayName("무응답 가드 발동 (LLM 호출 없이 NOT_EVALUABLE)")
    class BlankAnswerGuard {

        @Test
        @DisplayName("userAnswer 가 null 이면 전 차원 NOT_EVALUABLE 반환 + LLM 미호출")
        void should_return_notEvaluable_when_userAnswer_null() {
            givenRubric(List.of("technical_depth", "conceptual_accuracy"));

            RubricScoringResult result = service.score(question, questionSet, interview, null, AnswerAnalysis.empty());

            assertAllNotEvaluable(result, "technical_depth", "conceptual_accuracy");
            verify(rubricScorer, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("userAnswer 가 빈 문자열이면 NOT_EVALUABLE 반환")
        void should_return_notEvaluable_when_userAnswer_empty() {
            givenRubric(List.of("technical_depth"));

            RubricScoringResult result = service.score(question, questionSet, interview, "", AnswerAnalysis.empty());

            assertAllNotEvaluable(result, "technical_depth");
            verify(rubricScorer, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("userAnswer strip 후 3자 이하이면 NOT_EVALUABLE 반환 (경계 - 3자)")
        void should_return_notEvaluable_when_userAnswer_length_exactly_3() {
            givenRubric(List.of("technical_depth"));

            RubricScoringResult result = service.score(question, questionSet, interview, "잘몰라", AnswerAnalysis.empty());

            assertAllNotEvaluable(result, "technical_depth");
            verify(rubricScorer, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }

        @Test
        @DisplayName("공백만 있는 답변은 strip 후 0자로 NOT_EVALUABLE 반환")
        void should_return_notEvaluable_when_userAnswer_whitespace_only() {
            givenRubric(List.of("technical_depth"));

            RubricScoringResult result = service.score(question, questionSet, interview, "   \n\t  ", AnswerAnalysis.empty());

            assertAllNotEvaluable(result, "technical_depth");
            verify(rubricScorer, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("정상 채점 경로")
    class NormalScoring {

        @Test
        @DisplayName("userAnswer 가 4자 이상이면 LLM 채점기 호출 (경계 - 4자)")
        void should_call_LLM_when_userAnswer_length_exactly_4() {
            givenRubric(List.of("technical_depth"));
            RubricScoringResult scorerResult =
                    new RubricScoringResult(RUBRIC_ID, List.of("technical_depth"), Map.of(), null);
            given(rubricScorer.score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong()))
                    .willReturn(scorerResult);

            RubricScoringResult result = service.score(question, questionSet, interview, "잘 모르겠습니다",
                    AnswerAnalysis.empty());

            assertThat(result).isSameAs(scorerResult);
            verify(rubricScorer).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }
    }

    @Nested
    @DisplayName("채점 대상 차원 자체가 없는 케이스")
    class EmptyDimensions {

        @Test
        @DisplayName("rubric 의 selectDimensions 이 빈 리스트면 무응답 가드보다 우선해 empty 반환")
        void should_return_empty_when_no_dimensions_to_score() {
            givenRubric(List.of());

            RubricScoringResult result = service.score(question, questionSet, interview, "", AnswerAnalysis.empty());

            assertThat(result.isEmpty()).isTrue();
            verify(rubricScorer, never()).score(any(), any(), any(), any(), any(), any(), anyLong(), anyLong());
        }
    }

    private void givenRubric(List<String> dimensions) {
        Rubric rubric = new Rubric(
                RUBRIC_ID,
                "테스트 rubric",
                dimensions.stream().map(d -> new DimensionRef(d, 1.0)).toList(),
                Map.of("on_intent_answer", dimensions),
                Map.of()
        );
        given(rubricCatalog.resolveFor(question, questionSet, interview)).willReturn(rubric);
    }

    private void assertAllNotEvaluable(RubricScoringResult result, String... dimensions) {
        assertThat(result.rubricId()).isEqualTo(RUBRIC_ID);
        assertThat(result.scoredDimensions()).containsExactlyInAnyOrder(dimensions);
        for (String dim : dimensions) {
            assertThat(result.dimensionScores().get(dim).status()).isEqualTo(DimensionStatus.NOT_EVALUABLE);
            assertThat(result.dimensionScores().get(dim).score()).isNull();
        }
    }
}
