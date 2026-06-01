package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInputAssembler;
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

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;

// PR1 중립화 상태 검증. PR2(코멘트 기반 재설계) 시 입력 조립 테스트 재작성 예정.
@ExtendWith(MockitoExtension.class)
@DisplayName("SessionFeedbackInputAssembler - PR1 중립화")
class SessionFeedbackInputAssemblerTest {

    @Mock
    private InterviewFinder interviewFinder;

    private SessionFeedbackInputAssembler assembler;
    private Interview interview;

    @BeforeEach
    void setUp() {
        assembler = new SessionFeedbackInputAssembler(interviewFinder);
        interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
    }

    @Test
    @DisplayName("assemble: 루브릭 점수 제거로 turnScores 없이 빈 입력을 반환한다")
    void assemble_returnsEmptyInput() {
        given(interviewFinder.findById(1L)).willReturn(interview);

        SessionFeedbackInput input = assembler.assemble(1L);

        assertThat(input.turnScores()).isEmpty();
        assertThat(input.scoresByCategory()).isEmpty();
        assertThat(input.appliedRubrics()).isEmpty();
        assertThat(input.nonverbalAggregate()).isNull();
        assertThat(input.coverage()).isEqualTo("0/0 turns scored");
        assertThat(input.userLevel()).isEqualTo(InterviewLevel.MID);
    }

    @Test
    @DisplayName("assemble: 세션 메타데이터는 인터뷰 정보로 채워진다")
    void assemble_fillsSessionMetadata() {
        given(interviewFinder.findById(1L)).willReturn(interview);

        SessionFeedbackInput input = assembler.assemble(1L);

        assertThat(input.sessionMetadata().position()).isEqualTo("BACKEND");
        assertThat(input.sessionMetadata().level()).isEqualTo("MID");
        assertThat(input.sessionMetadata().interviewTypes()).containsExactly("CS_FUNDAMENTAL");
        assertThat(input.sessionMetadata().totalTurns()).isZero();
        assertThat(input.sessionMetadata().durationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("assembleWithDelivery: Delivery 인자와 무관하게 빈 입력을 반환한다")
    void assembleWithDelivery_returnsEmptyInput() {
        given(interviewFinder.findById(1L)).willReturn(interview);

        SessionFeedbackInput input = assembler.assembleWithDelivery(1L, "{}", "{}", "{}");

        assertThat(input.turnScores()).isEmpty();
        assertThat(input.nonverbalAggregate()).isNull();
    }
}
