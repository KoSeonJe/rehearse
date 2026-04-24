package com.rehearse.api.domain.interview.policy;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("ResumeTrackPolicy — Resume 트랙 7턴 skeleton (plan-07 에서 ChainStateTracker 주입으로 확장 예정)")
class ResumeTrackPolicySkeletonTest {

    private final ResumeTrackPolicy policy = new ResumeTrackPolicy();

    @Test
    @DisplayName("getTrack 은 RESUME 트랙 반환")
    void getTrack_returnsResume() {
        assertThat(policy.getTrack()).isEqualTo(InterviewTrack.RESUME);
    }

    @Test
    @DisplayName("FOLLOWUP 6개까지는 진행 허용 (skeleton 하드 상한 미도달)")
    void assertCanContinue_belowHardCap_allowed() {
        Interview interview = resumeInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 6);

        assertThatCode(() -> policy.assertCanContinue(interview, questionSet))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FOLLOWUP 7개 도달 시 MAX_FOLLOWUP_EXCEEDED 발생 (7턴 하드 상한)")
    void assertCanContinue_atHardCap_throws() {
        Interview interview = resumeInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 7);

        assertThatThrownBy(() -> policy.assertCanContinue(interview, questionSet))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("QUESTION_SET_004"));
    }

    private Interview resumeInterview() {
        return Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .build();
    }

    private QuestionSet questionSetWithFollowUps(Interview interview, int followUpCount) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();

        qs.addQuestion(Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("main")
                .orderIndex(0)
                .build());

        for (int i = 0; i < followUpCount; i++) {
            qs.addQuestion(Question.builder()
                    .questionType(QuestionType.FOLLOWUP)
                    .questionText("followup " + (i + 1))
                    .orderIndex(i + 1)
                    .build());
        }
        return qs;
    }
}
