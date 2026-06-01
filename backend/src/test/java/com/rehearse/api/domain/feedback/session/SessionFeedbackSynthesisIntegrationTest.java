package com.rehearse.api.domain.feedback.session;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackResponse;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.models.service.SessionFeedbackSynthesizer;
import com.rehearse.api.domain.feedback.session.repository.SessionFeedbackRepository;
import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.domain.feedback.session.vo.GapItem;
import com.rehearse.api.domain.feedback.session.vo.OverallSection;
import com.rehearse.api.domain.feedback.session.vo.StrengthItem;
import com.rehearse.api.domain.feedback.session.vo.WeekPlanItem;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.assertj.core.api.Assertions;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@DisplayName("SessionFeedback 종합 — timestamp_feedback 코멘트 기반 입력으로 session_feedback 적재")
class SessionFeedbackSynthesisIntegrationTest extends ServiceIntegrationSupport {

    @Autowired private SessionFeedbackService sessionFeedbackService;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private QuestionSetRepository questionSetRepository;
    @Autowired private QuestionSetFeedbackRepository questionSetFeedbackRepository;
    @Autowired private SessionFeedbackRepository sessionFeedbackRepository;
    @Autowired private UserRepository userRepository;

    @MockitoBean private SessionFeedbackSynthesizer synthesizer;

    @Test
    @DisplayName("synthesizePreliminary 가 코멘트 turn 입력으로 session_feedback 을 적재하고 응답에 루브릭 차원명/점수가 없다")
    void synthesizePreliminary_persistsCommentBasedSessionFeedback() {
        Fixture fixture = persistInterviewWithComments();
        ArgumentCaptor<SessionFeedbackInput> inputCaptor = ArgumentCaptor.forClass(SessionFeedbackInput.class);
        given(synthesizer.synthesize(any())).willReturn(samplePayload());

        sessionFeedbackService.synthesizePreliminary(fixture.interviewId());

        org.mockito.Mockito.verify(synthesizer).synthesize(inputCaptor.capture());
        SessionFeedbackInput input = inputCaptor.getValue();
        assertThat(input.turnScores()).hasSize(2);
        assertThat(input.turnScores().get(0).turnLabel()).isEqualTo("1-1");
        assertThat(input.turnScores().get(0).verbalComment()).isEqualTo("요구사항을 차분히 분해함");
        assertThat(input.turnScores().get(1).turnLabel()).isEqualTo("1-2");
        assertThat(input.coverage()).isEqualTo("all turns scored");

        Optional<SessionFeedback> persisted = sessionFeedbackRepository.findByInterviewId(fixture.interviewId());
        assertThat(persisted).isPresent();

        SessionFeedbackResponse response = sessionFeedbackService.getByInterview(fixture.interviewId());
        assertThat(response.getOverall().narrative()).isEqualTo("코멘트 기반 종합 서술");
        assertThat(response.getStrengths()).hasSize(1);
        assertThat(response.getGaps()).hasSize(1);

        String serialized = response.getOverall().toString()
                + response.getStrengths()
                + response.getGaps();
        Assertions.assertThat(serialized)
                .doesNotContain("dimensionScores")
                .doesNotContain("levelGap")
                .doesNotContain("문제 정의")
                .doesNotContain("기술 깊이");
    }

    private GeneratedSessionFeedback samplePayload() {
        return new GeneratedSessionFeedback(
                new OverallSection("주니어 기대치 충족", "코멘트 기반 종합 서술", "all turns scored"),
                List.of(new StrengthItem("1-1 답변에서 요구사항 분해", "구조적 사고")),
                List.of(new GapItem("1-2 답변에서 근거 부족", "비교표 작성 연습")),
                null,
                List.of(new WeekPlanItem(1, "자료구조", List.of("CTCI"), "매일 1문제"))
        );
    }

    private Fixture persistInterviewWithComments() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("session-feedback@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-session-feedback")
                .role(UserRole.USER)
                .build());

        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.MID, List.of(InterviewType.CS_FUNDAMENTAL));
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.CS_FUNDAMENTAL, 0);
        Question main = Question.builder()
                .questionType(QuestionType.TECH_MAIN)
                .questionText("메인 질문")
                .orderIndex(0)
                .build();
        Question followUp = Question.builder()
                .questionType(QuestionType.TECH_FOLLOWUP)
                .questionText("후속 질문")
                .orderIndex(1)
                .build();
        questionSet.addQuestion(main);
        questionSet.addQuestion(followUp);
        questionSetRepository.saveAndFlush(questionSet);

        QuestionSetFeedback questionSetFeedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment("세트 종합 코멘트")
                .build();
        questionSetFeedback.addTimestampFeedback(comment(main, 0, "요구사항을 차분히 분해함"));
        questionSetFeedback.addTimestampFeedback(comment(followUp, 1000, "근거가 다소 얕았음"));
        questionSetFeedbackRepository.saveAndFlush(questionSetFeedback);

        return new Fixture(interview.getId());
    }

    private TimestampFeedback comment(Question question, long startMs, String verbalComment) {
        return TimestampFeedback.builder()
                .question(question)
                .startMs(startMs)
                .endMs(startMs + 1000)
                .verbalComment(verbalComment)
                .overallComment("종합")
                .isAnalyzed(true)
                .build();
    }

    private record Fixture(Long interviewId) {}
}
