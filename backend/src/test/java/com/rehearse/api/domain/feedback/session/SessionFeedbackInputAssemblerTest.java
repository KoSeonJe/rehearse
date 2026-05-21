package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.rubric.service.NonverbalImprovementActionsLoader;
import com.rehearse.api.domain.feedback.rubric.service.RubricLoader;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
import com.rehearse.api.domain.feedback.session.synthesis.TurnScoreView;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackInputAssemblerTest {

    private SessionFeedbackInputAssembler assembler;

    @Mock
    private QuestionScoreRepository questionScoreRepository;

    @Mock
    private QuestionScoreDimensionRepository questionScoreDimensionRepository;

    @Mock
    private InterviewFinder interviewFinder;

    private Interview mockInterview;

    @BeforeEach
    void setUp() {
        RubricLoader rubricLoader = new RubricLoader();
        ReflectionTestUtils.invokeMethod(rubricLoader, "init");
        assembler = new SessionFeedbackInputAssembler(
                questionScoreRepository,
                questionScoreDimensionRepository,
                interviewFinder,
                new NonverbalImprovementActionsLoader(),
                rubricLoader
        );
        mockInterview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
    }

    @Test
    @DisplayName("scoresJson이 비어있는 턴은 FAILED로 매핑되고 coverage에 반영된다")
    void assemble_mapsEmptyScoresToFailedStatus() {
        Long interviewId = 1L;

        QuestionScore okScore = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(1L)
                .rubricId("cs-v1")
                .levelFlag("MID")
                .build();
        ReflectionTestUtils.setField(okScore, "id", 1L);

        QuestionScore failedScore = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(2L)
                .rubricId("cs-v1")
                .levelFlag(null)
                .build();
        ReflectionTestUtils.setField(failedScore, "id", 2L);

        QuestionScoreDimension dim = QuestionScoreDimension.builder()
                .questionScoreId(okScore.getId())
                .dimensionRef("problem_framing")
                .score(3)
                .observation("명확함")
                .evidenceQuote("turn 1에서 증명")
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of(okScore, failedScore));
        given(questionScoreDimensionRepository.findByQuestionScoreIdIn(List.of(okScore.getId(), failedScore.getId())))
                .willReturn(List.of(dim));

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.turnScores()).hasSize(2);

        TurnScoreView okTurn = input.turnScores().get(0);
        assertThat(okTurn.status()).isEqualTo(TurnScoreView.TurnStatus.OK);
        assertThat(okTurn.scoredDimensions()).containsExactly("문제 정의");
        assertThat(okTurn.dimensionScores()).containsKey("문제 정의");
        assertThat(okTurn.dimensionScores()).doesNotContainKey("problem_framing");

        TurnScoreView failedTurn = input.turnScores().get(1);
        assertThat(failedTurn.status()).isEqualTo(TurnScoreView.TurnStatus.FAILED);
        assertThat(failedTurn.scoredDimensions()).isEmpty();

        assertThat(input.coverage()).isEqualTo("1/2 turns scored");
    }

    @Test
    @DisplayName("모든 턴이 OK이면 coverage는 'all turns scored'")
    void assemble_allOkTurns_returnsAllTurnsScoredCoverage() {
        Long interviewId = 2L;

        QuestionScore score = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(1L)
                .rubricId("cs-v1")
                .levelFlag("MID")
                .build();
        ReflectionTestUtils.setField(score, "id", 10L);

        QuestionScoreDimension dim = QuestionScoreDimension.builder()
                .questionScoreId(score.getId())
                .dimensionRef("problem_framing")
                .score(2)
                .observation("보통")
                .evidenceQuote("turn 1")
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of(score));
        given(questionScoreDimensionRepository.findByQuestionScoreIdIn(List.of(score.getId())))
                .willReturn(List.of(dim));

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.coverage()).isEqualTo("all turns scored");
        assertThat(input.userLevel()).isEqualTo(InterviewLevel.MID);
    }

    @Test
    @DisplayName("sessionMetadata는 타입 있는 record로 생성된다")
    void assemble_createsTypedSessionMetadata() {
        Long interviewId = 20L;

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of());

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.sessionMetadata().position()).isEqualTo("BACKEND");
        assertThat(input.sessionMetadata().level()).isEqualTo("MID");
        assertThat(input.sessionMetadata().interviewTypes()).containsExactly("CS_FUNDAMENTAL");
        assertThat(input.sessionMetadata().totalTurns()).isZero();
        assertThat(input.sessionMetadata().durationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("모든 dim 이 NOT_EVALUABLE 인 turn 은 TurnStatus.NOT_EVALUABLE 로 매핑되고 coverage 분자에서 제외된다")
    void assemble_mapsAllNotEvaluableDimensions_toNotEvaluableStatus() {
        Long interviewId = 30L;

        QuestionScore okScore = QuestionScore.builder()
                .interviewId(interviewId).questionId(1L).rubricId("cs-v1").levelFlag("MID").build();
        ReflectionTestUtils.setField(okScore, "id", 30L);

        QuestionScore notEvaluableScore = QuestionScore.builder()
                .interviewId(interviewId).questionId(2L).rubricId("cs-v1").levelFlag("MID").build();
        ReflectionTestUtils.setField(notEvaluableScore, "id", 31L);

        QuestionScoreDimension okDim = QuestionScoreDimension.builder()
                .questionScoreId(okScore.getId())
                .dimensionRef("problem_framing").score(3).observation("명확함").evidenceQuote("turn 1")
                .status(DimensionStatus.OK)
                .build();
        QuestionScoreDimension neDim1 = QuestionScoreDimension.builder()
                .questionScoreId(notEvaluableScore.getId())
                .dimensionRef("problem_framing").score(null).observation("관련 발언 없음").evidenceQuote(null)
                .status(DimensionStatus.NOT_EVALUABLE)
                .build();
        QuestionScoreDimension neDim2 = QuestionScoreDimension.builder()
                .questionScoreId(notEvaluableScore.getId())
                .dimensionRef("technical_depth").score(null).observation("관련 발언 없음").evidenceQuote(null)
                .status(DimensionStatus.NOT_EVALUABLE)
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of(okScore, notEvaluableScore));
        given(questionScoreDimensionRepository.findByQuestionScoreIdIn(List.of(okScore.getId(), notEvaluableScore.getId())))
                .willReturn(List.of(okDim, neDim1, neDim2));

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.turnScores()).hasSize(2);
        assertThat(input.turnScores().get(0).status()).isEqualTo(TurnScoreView.TurnStatus.OK);
        assertThat(input.turnScores().get(1).status()).isEqualTo(TurnScoreView.TurnStatus.NOT_EVALUABLE);
        assertThat(input.coverage()).isEqualTo("1/2 turns scored");
        assertThat(input.scoresByCategory().get("cs-v1")).containsOnlyKeys("문제 정의");
    }

    @Test
    @DisplayName("DB nonverbal_score가 있으면 D11~D14 aggregate와 최저 차원 개선 액션을 Delivery 입력으로 사용한다")
    void assembleWithDelivery_usesDbNonverbalScoresForTypedAggregate() {
        Long interviewId = 3L;

        QuestionScore contentScore = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(1L)
                .rubricId("cs-v1")
                .levelFlag("MID")
                .build();
        ReflectionTestUtils.setField(contentScore, "id", 100L);

        QuestionScoreDimension contentDim = QuestionScoreDimension.builder()
                .questionScoreId(contentScore.getId())
                .dimensionRef("problem_framing")
                .score(3)
                .observation("명확함")
                .evidenceQuote("turn 1")
                .build();

        QuestionScore nonverbalScore1 = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(1L)
                .rubricId("nonverbal-v1")
                .levelFlag(null)
                .build();
        ReflectionTestUtils.setField(nonverbalScore1, "id", 101L);

        QuestionScore nonverbalScore2 = QuestionScore.builder()
                .interviewId(interviewId)
                .questionId(2L)
                .rubricId("nonverbal-v1")
                .levelFlag(null)
                .build();
        ReflectionTestUtils.setField(nonverbalScore2, "id", 102L);

        QuestionScoreDimension nv1D11 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore1.getId())
                .dimensionRef("fluency")
                .score(2).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv1D12 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore1.getId())
                .dimensionRef("confidence_tone")
                .score(3).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv1D13 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore1.getId())
                .dimensionRef("eye_contact_posture")
                .score(1).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv1D14 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore1.getId())
                .dimensionRef("composure")
                .score(2).observation("").evidenceQuote("").build();

        QuestionScoreDimension nv2D11 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore2.getId())
                .dimensionRef("fluency")
                .score(2).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv2D12 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore2.getId())
                .dimensionRef("confidence_tone")
                .score(2).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv2D13 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore2.getId())
                .dimensionRef("eye_contact_posture")
                .score(1).observation("").evidenceQuote("").build();
        QuestionScoreDimension nv2D14 = QuestionScoreDimension.builder()
                .questionScoreId(nonverbalScore2.getId())
                .dimensionRef("composure")
                .score(3).observation("").evidenceQuote("").build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of(contentScore, nonverbalScore1, nonverbalScore2));
        given(questionScoreDimensionRepository.findByQuestionScoreIdIn(
                List.of(contentScore.getId(), nonverbalScore1.getId(), nonverbalScore2.getId())))
                .willReturn(List.of(contentDim, nv1D11, nv1D12, nv1D13, nv1D14, nv2D11, nv2D12, nv2D13, nv2D14));

        SessionFeedbackInput input = assembler.assembleWithDelivery(
                interviewId,
                "{\"legacy\":\"delivery\"}",
                "{\"legacy\":\"vision\"}",
                "{\"legacy\":\"aggregate\"}"
        );

        SessionFeedbackInput.NonverbalDeliveryAggregate aggregate = input.nonverbalAggregate();

        assertThat(aggregate.source()).isEqualTo("nonverbal_score");
        assertThat(input.legacyNonverbalAggregateJson()).isNull();
        assertThat(aggregate.turns()).hasSize(2);
        assertThat(aggregate.turns().getFirst().scores()).containsEntry("시선", 1);
        assertThat(aggregate.turns().getFirst().scores()).doesNotContainKey("eye_contact_posture");
        assertThat(aggregate.averageScores()).containsEntry("유창함", 2.0);
        assertThat(aggregate.averageScores()).containsEntry("시선", 1.0);
        assertThat(aggregate.averageScores()).containsEntry("자신감", 2.5);
        assertThat(aggregate.averageScores()).doesNotContainKey("fluency");
        assertThat(aggregate.averageScores()).doesNotContainKey("confidence_tone");
        assertThat(aggregate.lowestDimension().dimension()).isEqualTo("시선");
        assertThat(aggregate.lowestDimension().averageScore()).isEqualTo(1.0);
        assertThat(aggregate.recommendedActions().getFirst().dimension()).isEqualTo("시선");
        assertThat(aggregate.recommendedActions().getFirst().actions().getFirst())
                .contains("카메라를 보고 말하기");
    }

    @Test
    @DisplayName("DB nonverbal_score가 없으면 기존 Lambda aggregate 문자열을 fallback으로 유지한다")
    void assembleWithDelivery_fallsBackToLegacyAggregateWhenNoDbScores() {
        Long interviewId = 4L;
        String legacyAggregate = "{\"legacy\":\"aggregate\"}";

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId))
                .willReturn(List.of());

        SessionFeedbackInput input = assembler.assembleWithDelivery(
                interviewId,
                null,
                null,
                legacyAggregate
        );

        assertThat(input.nonverbalAggregate()).isNull();
        assertThat(input.legacyNonverbalAggregateJson()).isEqualTo(legacyAggregate);
    }
}
