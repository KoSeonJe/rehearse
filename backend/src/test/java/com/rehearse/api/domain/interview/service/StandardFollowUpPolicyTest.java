package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
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

    @Test
    @DisplayName("shouldSkipFollowUp — 첫 질문이 RESUME_OPENER 이면 true")
    void shouldSkipFollowUp_resumeOpener_true() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.RESUME_BASED).orderIndex(0).build();
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.RESUME_OPENER).questionText("self-intro").orderIndex(0).build());

        assertThat(policy.shouldSkipFollowUp(qs, null)).isTrue();
    }

    @Test
    @DisplayName("shouldSkipFollowUp — RESUME_MAIN 은 follow-up 진행 (false)")
    void shouldSkipFollowUp_resumeMain_false() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.RESUME_BASED).orderIndex(0).build();
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.RESUME_MAIN).questionText("project").orderIndex(0).build());

        assertThat(policy.shouldSkipFollowUp(qs, null)).isFalse();
    }

    @Test
    @DisplayName("shouldSkipFollowUp — TECH_MAIN 은 follow-up 진행 (false)")
    void shouldSkipFollowUp_techMain_false() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.CS_FUNDAMENTAL).orderIndex(0).build();
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.TECH_MAIN).questionText("gc").orderIndex(0).build());

        assertThat(policy.shouldSkipFollowUp(qs, null)).isFalse();
    }

    @Test
    @DisplayName("이력서 트랙 — RESUME_MAIN per-main cap=2 도달 시 throw")
    void assertCanContinue_resumeMain_capOneEnforced() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.RESUME_BASED).orderIndex(0).build();
        qs.addQuestion(resumeQuestion(QuestionType.RESUME_MAIN, "main-1", 0));
        qs.addQuestion(resumeQuestion(QuestionType.RESUME_FOLLOWUP, "f-1", 1));
        qs.addQuestion(resumeQuestion(QuestionType.RESUME_FOLLOWUP, "f-2", 2));

        assertThatThrownBy(() -> policy.assertCanContinue(interview, qs))
                .isInstanceOf(BusinessException.class);
    }

    @Test
    @DisplayName("이력서 트랙 — 두 번째 RESUME_MAIN 의 follow-up 은 currentMainQuestionId 로 카운트 분리")
    void assertCanContinue_resumeMain_scopedByCurrentMainQuestionId() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.RESUME_BASED).orderIndex(0).build();
        Question main1 = resumeQuestion(QuestionType.RESUME_MAIN, "main-1", 0);
        setId(main1, 11L);
        qs.addQuestion(main1);
        qs.addQuestion(resumeQuestion(QuestionType.RESUME_FOLLOWUP, "f-1", 1));
        Question main2 = resumeQuestion(QuestionType.RESUME_MAIN, "main-2", 2);
        setId(main2, 12L);
        qs.addQuestion(main2);

        assertThatCode(() -> policy.assertCanContinue(interview, qs, 12L))
                .doesNotThrowAnyException();
    }

    @Test
    @DisplayName("이력서 트랙 — RESUME_OPENER 가 currentMainQuestionId 로 지정되면 skip=true")
    void shouldSkipFollowUp_resumeOpener_withCurrentMainId() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview).category(InterviewType.RESUME_BASED).orderIndex(0).build();
        Question opener = resumeQuestion(QuestionType.RESUME_OPENER, "self-intro", 0);
        setId(opener, 21L);
        qs.addQuestion(opener);
        Question main = resumeQuestion(QuestionType.RESUME_MAIN, "main-1", 1);
        setId(main, 22L);
        qs.addQuestion(main);

        assertThat(policy.shouldSkipFollowUp(qs, 21L)).isTrue();
        assertThat(policy.shouldSkipFollowUp(qs, 22L)).isFalse();
    }

    private static Question resumeQuestion(QuestionType type, String text, int orderIndex) {
        return Question.builder()
                .questionType(type)
                .questionText(text)
                .orderIndex(orderIndex)
                .build();
    }

    private static void setId(Question q, long id) {
        try {
            java.lang.reflect.Field f = Question.class.getDeclaredField("id");
            f.setAccessible(true);
            f.set(q, id);
        } catch (ReflectiveOperationException e) {
            throw new RuntimeException(e);
        }
    }

    @Test
    @DisplayName("TECH_FOLLOWUP / BEHAVIORAL_FOLLOWUP 도 isFollowUp() 으로 카운트되어 cap 트리거")
    void assertCanContinue_subTypeFollowUps_countsTowardsCap() {
        Interview interview = standardInterview();
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.TECH_MAIN)
                .questionText("main").orderIndex(0).build());
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.TECH_FOLLOWUP)
                .questionText("f1").orderIndex(1).build());
        qs.addQuestion(Question.builder()
                .questionType(QuestionType.BEHAVIORAL_FOLLOWUP)
                .questionText("f2").orderIndex(2).build());

        assertThatThrownBy(() -> policy.assertCanContinue(interview, qs))
                .isInstanceOf(BusinessException.class);
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
                .category(InterviewType.CS_FUNDAMENTAL)
                .orderIndex(0)
                .build();

        qs.addQuestion(Question.builder()
                .questionType(QuestionType.TECH_MAIN)
                .questionText("main")
                .orderIndex(0)
                .build());

        for (int i = 0; i < followUpCount; i++) {
            qs.addQuestion(Question.builder()
                    .questionType(QuestionType.TECH_FOLLOWUP)
                    .questionText("followup " + (i + 1))
                    .orderIndex(i + 1)
                    .build());
        }
        return qs;
    }
}
