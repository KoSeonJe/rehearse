package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionSetCategory;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;

@DisplayName("ResumeTrackPolicy — RESUME 트랙 종료 제어 외부 위임 (no-op)")
class ResumeTrackPolicyTest {

    private final ResumeTrackPolicy policy = new ResumeTrackPolicy();

    @Test
    @DisplayName("getTrack 은 RESUME 반환")
    void getTrack_returnsResume() {
        assertThat(policy.getTrack()).isEqualTo(InterviewTrack.RESUME);
    }

    @Test
    @DisplayName("getMaxFollowUpRounds 는 HARD_TURN_CAP(7) 반환 — Runtime 보고용")
    void getMaxFollowUpRounds_returnsHardTurnCap() {
        assertThat(policy.getMaxFollowUpRounds()).isEqualTo(7);
    }

    @Test
    @DisplayName("RESUME_INTERROGATION 다수 적재 상태에서도 assertCanContinue 는 예외를 던지지 않는다 (no-op)")
    void assertCanContinue_resumeInterrogations_noThrow() {
        Interview interview = resumeInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.RESUME_BASED)
                .orderIndex(0)
                .build();
        for (int i = 0; i < 10; i++) {
            qs.addQuestion(Question.builder()
                    .questionType(QuestionType.RESUME_INTERROGATION)
                    .questionText("interrogation " + i)
                    .orderIndex(i)
                    .build());
        }

        assertThatCode(() -> policy.assertCanContinue(interview, qs))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("빈 QuestionSet 도 assertCanContinue 는 통과한다")
    void assertCanContinue_emptyQuestionSet_noThrow() {
        Interview interview = resumeInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.RESUME_BASED)
                .orderIndex(0)
                .build();

        assertThatCode(() -> policy.assertCanContinue(interview, qs))
                .doesNotThrowAnyException();
    }

    private Interview resumeInterview() {
        return Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .build();
    }
}
