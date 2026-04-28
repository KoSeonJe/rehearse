package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import com.rehearse.api.domain.feedback.rubric.repository.RubricScoreRepository;
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
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class SessionFeedbackInputAssemblerTest {

    @InjectMocks
    private SessionFeedbackInputAssembler assembler;

    @Mock
    private RubricScoreRepository rubricScoreRepository;

    @Mock
    private InterviewFinder interviewFinder;

    private Interview mockInterview;

    @BeforeEach
    void setUp() {
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

        RubricScoreEntity okEntity = RubricScoreEntity.builder()
                .interviewId(interviewId)
                .turnId(1L)
                .rubricId("cs-v1")
                .scoresJson(Map.of("D1", DimensionScore.of(3, "명확함", "turn 1에서 증명")))
                .levelFlag("MID")
                .build();

        RubricScoreEntity failedEntity = RubricScoreEntity.builder()
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

        RubricScoreEntity entity = RubricScoreEntity.builder()
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
}
