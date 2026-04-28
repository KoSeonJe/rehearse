package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
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

@DisplayName("StandardFollowUpPolicy — CS/Language 트랙 기본 2턴 정책")
class StandardFollowUpPolicyTest {

    private final StandardFollowUpPolicy policy = new StandardFollowUpPolicy(2);

    @Test
    @DisplayName("getTrack 은 CS 트랙 반환")
    void getTrack_returnsCs() {
        assertThat(policy.getTrack()).isEqualTo(InterviewTrack.CS);
    }

    @Test
    @DisplayName("FOLLOWUP 0개 상태에서 진행 허용")
    void assertCanContinue_withZeroFollowUps_allowed() {
        Interview interview = standardInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 0);

        assertThatCode(() -> policy.assertCanContinue(interview, questionSet))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FOLLOWUP 1개 상태에서 진행 허용")
    void assertCanContinue_withOneFollowUp_allowed() {
        Interview interview = standardInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 1);

        assertThatCode(() -> policy.assertCanContinue(interview, questionSet))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("FOLLOWUP 2개 도달 시 MAX_FOLLOWUP_EXCEEDED 발생")
    void assertCanContinue_atMaxRounds_throws() {
        Interview interview = standardInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 2);

        assertThatThrownBy(() -> policy.assertCanContinue(interview, questionSet))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                        .isEqualTo("QUESTION_SET_004"));
    }

    @Test
    @DisplayName("FOLLOWUP 3개 초과 상태에서도 동일 예외 발생")
    void assertCanContinue_overMaxRounds_throws() {
        Interview interview = standardInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 3);

        assertThatThrownBy(() -> policy.assertCanContinue(interview, questionSet))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("getMaxFollowUpRounds 는 주입된 maxRounds 반환")
    void getMaxFollowUpRounds_returnsInjectedValue() {
        assertThat(policy.getMaxFollowUpRounds()).isEqualTo(2);
        assertThat(new StandardFollowUpPolicy(5).getMaxFollowUpRounds()).isEqualTo(5);
    }

    @Test
    @DisplayName("isExhausted 는 currentCount >= maxRounds 일 때 true")
    void isExhausted_atOrAboveMax_returnsTrue() {
        assertThat(policy.isExhausted(0)).isFalse();
        assertThat(policy.isExhausted(1)).isFalse();
        assertThat(policy.isExhausted(2)).isTrue();
        assertThat(policy.isExhausted(3)).isTrue();
    }

    @Test
    @DisplayName("max-follow-up-rounds=3 설정으로 주입되면 2턴 상태에서 진행 허용 (튜닝 가능성)")
    void assertCanContinue_withTunedMaxRounds_allowsMoreTurns() {
        StandardFollowUpPolicy tuned = new StandardFollowUpPolicy(3);
        Interview interview = standardInterview();
        QuestionSet questionSet = questionSetWithFollowUps(interview, 2);

        assertThatCode(() -> tuned.assertCanContinue(interview, questionSet))
                .doesNotThrowAnyException();
    }

    private Interview standardInterview() {
        return Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
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
