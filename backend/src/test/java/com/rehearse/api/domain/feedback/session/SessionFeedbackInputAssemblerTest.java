package com.rehearse.api.domain.feedback.session;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.NonverbalScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import com.rehearse.api.domain.feedback.rubric.repository.NonverbalScoreRepository;
import com.rehearse.api.domain.feedback.rubric.repository.RubricScoreRepository;
import com.rehearse.api.domain.feedback.rubric.service.NonverbalImprovementActionsLoader;
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

import java.math.BigDecimal;
import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackInputAssemblerTest {

    private SessionFeedbackInputAssembler assembler;

    @Mock
    private RubricScoreRepository rubricScoreRepository;

    @Mock
    private NonverbalScoreRepository nonverbalScoreRepository;

    @Mock
    private InterviewFinder interviewFinder;

    private final ObjectMapper objectMapper = new ObjectMapper();

    private Interview mockInterview;

    @BeforeEach
    void setUp() {
        assembler = new SessionFeedbackInputAssembler(
                rubricScoreRepository,
                nonverbalScoreRepository,
                interviewFinder,
                objectMapper,
                new NonverbalImprovementActionsLoader()
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

        RubricScore okEntity = RubricScore.builder()
                .interviewId(interviewId)
                .turnId(1L)
                .rubricId("cs-v1")
                .scoresJson(Map.of("D1", DimensionScore.of(3, "명확함", "turn 1에서 증명")))
                .levelFlag("MID")
                .build();

        RubricScore failedEntity = RubricScore.builder()
                .interviewId(interviewId)
                .turnId(2L)
                .rubricId("cs-v1")
                .scoresJson(Map.of())
                .levelFlag(null)
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of(okEntity, failedEntity));

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.turnScores()).hasSize(2);

        TurnScoreView okTurn = input.turnScores().get(0);
        assertThat(okTurn.status()).isEqualTo(TurnScoreView.TurnStatus.OK);
        assertThat(okTurn.scoredDimensions()).containsExactly("D1");

        TurnScoreView failedTurn = input.turnScores().get(1);
        assertThat(failedTurn.status()).isEqualTo(TurnScoreView.TurnStatus.FAILED);
        assertThat(failedTurn.scoredDimensions()).isEmpty();

        assertThat(input.coverage()).isEqualTo("1/2 turns scored");
    }

    @Test
    @DisplayName("모든 턴이 OK이면 coverage는 'all turns scored'")
    void assemble_allOkTurns_returnsAllTurnsScoredCoverage() {
        Long interviewId = 2L;

        RubricScore entity = RubricScore.builder()
                .interviewId(interviewId)
                .turnId(1L)
                .rubricId("cs-v1")
                .scoresJson(Map.of("D1", DimensionScore.of(2, "보통", "turn 1")))
                .levelFlag("MID")
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of(entity));

        SessionFeedbackInput input = assembler.assemble(interviewId);

        assertThat(input.coverage()).isEqualTo("all turns scored");
        assertThat(input.userLevel()).isEqualTo(InterviewLevel.MID);
    }

    @Test
    @DisplayName("DB nonverbal_score가 있으면 D11~D14 aggregate와 최저 차원 개선 액션을 Delivery 입력으로 사용한다")
    void assembleWithDelivery_usesDbNonverbalScoresForAggregate() throws Exception {
        Long interviewId = 3L;
        RubricScore contentScore = RubricScore.builder()
                .interviewId(interviewId)
                .turnId(1L)
                .rubricId("cs-v1")
                .scoresJson(Map.of("D1", DimensionScore.of(3, "명확함", "turn 1")))
                .levelFlag("MID")
                .build();
        NonverbalScore firstTurn = NonverbalScore.builder()
                .interviewId(interviewId)
                .turnId(1L)
                .fluencyScore(2)
                .toneScore(3)
                .postureScore(1)
                .composureScore(2)
                .rawSignals("{\"fillerWordCount\":5}")
                .contextMultiplier(new BigDecimal("1.10"))
                .build();
        NonverbalScore secondTurn = NonverbalScore.builder()
                .interviewId(interviewId)
                .turnId(2L)
                .fluencyScore(2)
                .toneScore(2)
                .postureScore(1)
                .composureScore(3)
                .rawSignals("{\"fillerWordCount\":2}")
                .contextMultiplier(new BigDecimal("1.00"))
                .build();

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of(contentScore));
        given(nonverbalScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of(firstTurn, secondTurn));

        SessionFeedbackInput input = assembler.assembleWithDelivery(
                interviewId,
                "{\"legacy\":\"delivery\"}",
                "{\"legacy\":\"vision\"}",
                "{\"legacy\":\"aggregate\"}"
        );

        JsonNode aggregate = objectMapper.readTree(input.nonverbalAggregate());

        assertThat(aggregate.get("source").asText()).isEqualTo("nonverbal_score");
        assertThat(aggregate.get("legacyAggregate").get("legacy").asText()).isEqualTo("aggregate");
        assertThat(aggregate.get("turns")).hasSize(2);
        assertThat(aggregate.get("turns").get(0).get("turnId").asLong()).isEqualTo(1L);
        assertThat(aggregate.get("turns").get(0).get("scores").get("D13").asInt()).isEqualTo(1);
        assertThat(aggregate.get("turns").get(0).get("contextMultiplier").asDouble()).isEqualTo(1.1);
        assertThat(aggregate.get("averageScores").get("D11").asDouble()).isEqualTo(2.0);
        assertThat(aggregate.get("averageScores").get("D13").asDouble()).isEqualTo(1.0);
        assertThat(aggregate.get("lowestDimension").get("dimension").asText()).isEqualTo("D13");
        assertThat(aggregate.get("lowestDimension").get("averageScore").asDouble()).isEqualTo(1.0);
        assertThat(aggregate.get("recommendedActions").get(0).get("dimension").asText()).isEqualTo("D13");
        assertThat(aggregate.get("recommendedActions").get(0).get("actions").get(0).asText())
                .contains("카메라를 보고 말하기");
    }

    @Test
    @DisplayName("DB nonverbal_score가 없으면 기존 Lambda aggregate 문자열을 fallback으로 유지한다")
    void assembleWithDelivery_fallsBackToLegacyAggregateWhenNoDbScores() {
        Long interviewId = 4L;
        String legacyAggregate = "{\"legacy\":\"aggregate\"}";

        given(interviewFinder.findById(interviewId)).willReturn(mockInterview);
        given(rubricScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of());
        given(nonverbalScoreRepository.findByInterviewIdOrderByTurnIdAsc(interviewId))
                .willReturn(List.of());

        SessionFeedbackInput input = assembler.assembleWithDelivery(
                interviewId,
                null,
                null,
                legacyAggregate
        );

        assertThat(input.nonverbalAggregate()).isEqualTo(legacyAggregate);
    }
}
