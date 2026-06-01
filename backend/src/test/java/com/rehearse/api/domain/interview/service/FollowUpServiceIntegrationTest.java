package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.event.AnswerAnalysisCompletedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionCategory;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.awaitility.Awaitility;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Import;
import org.springframework.context.event.EventListener;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.web.multipart.MultipartFile;

import java.time.Duration;
import java.util.List;
import java.util.Map;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("FollowUpService — RESUME_OPENER 답변도 분석 후 follow-up 생성만 skip")
@Import(FollowUpServiceIntegrationTest.TestEventCollectorConfig.class)
class FollowUpServiceIntegrationTest extends ServiceIntegrationSupport {

    @Autowired private FollowUpService followUpService;
    @Autowired private UserRepository userRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private QuestionSetRepository questionSetRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private AnswerAnalysisCompletedEventCollector eventCollector;

    @MockitoBean private AudioTurnAnalysisService audioTurnAnalysisService;
    @MockitoBean private FollowUpQuestionService followUpQuestionService;

    private static AnswerAnalysis analysisOf(String transcript, RecommendedNextAction action) {
        return new AnswerAnalysis(transcript, List.of(), Map.of(), null, List.of(), action);
    }

    @BeforeEach
    void resetCollector() {
        eventCollector.clear();
    }

    @Test
    @DisplayName("RESUME_OPENER → analyzer 1회 호출 + 이벤트 발행 + follow-up 미생성")
    void resumeOpener_publishesEvent_andSkipsFollowUp() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_OPENER);
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(analysisOf("원문 답변", RecommendedNextAction.CLARIFICATION));

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId),
                audio());

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("resume_opener_skip");
        assertThat(response.getAnswerText()).isEqualTo("원문 답변");
        verify(audioTurnAnalysisService, times(1))
                .analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class));
        verify(followUpQuestionService, never()).write(any(), any(), any(QuestionCategory.class));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(1);
            AnswerAnalysisCompletedEvent event = eventCollector.events().get(0);
            assertThat(event.questionId()).isEqualTo(fixture.questionId);
            assertThat(event.analysis().transcript()).isEqualTo("원문 답변");
        });

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(1);
    }

    @Test
    @DisplayName("RESUME_MAIN 일반 답변 → analyzer + 이벤트 + follow-up 생성")
    void resumeMain_followsUp() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_MAIN);
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(analysisOf("정상 답변", RecommendedNextAction.CLARIFICATION));
        given(followUpQuestionService.write(any(), any(), any(QuestionCategory.class)))
                .willReturn(new GeneratedFollowUp(
                        false, null, "심화 질문", "TTS", "이유", "claim", "best", null, 0));

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId),
                audio());

        assertThat(response.isSkip()).isFalse();
        assertThat(response.getQuestion()).isEqualTo("심화 질문");
        verify(audioTurnAnalysisService, times(1))
                .analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class));
        verify(followUpQuestionService, times(1)).write(any(), any(), any(QuestionCategory.class));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(eventCollector.events()).hasSize(1));

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(2);
        assertThat(finalQuestions.get(1).getQuestionType()).isEqualTo(QuestionType.RESUME_FOLLOWUP);
    }

    @Test
    @DisplayName("analyzer SKIP 권고 → 이벤트 발행 + writer 미호출 + follow-up 미생성")
    void analyzerSkip_publishesEventAndSkipsWriter() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_MAIN);
        AnswerAnalysis skipAnalysis = analysisOf("부족 답변", RecommendedNextAction.SKIP);
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(skipAnalysis);

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId),
                audio());

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("analyzer_recommend_skip");
        verify(followUpQuestionService, never()).write(any(), any(), any(QuestionCategory.class));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(eventCollector.events()).hasSize(1));

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(1);
    }

    @Test
    @DisplayName("후속질문 답변 턴 → 방금 답변된 후속질문 ID로 이벤트 발행 (메인질문 이벤트 미발생)")
    void followUpAnswer_publishesEventUnderFollowUpQuestionId() {
        Fixture fixture = persistResumeFixtureWithFollowUp(QuestionType.RESUME_MAIN, QuestionType.RESUME_FOLLOWUP);
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(analysisOf("후속 답변", RecommendedNextAction.CLARIFICATION));
        given(followUpQuestionService.write(any(), any(), any(QuestionCategory.class)))
                .willReturn(new GeneratedFollowUp(
                        false, null, "심화 질문2", "TTS", "이유", "claim", "best", null, 0));

        followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId),
                audio());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(1);
            assertThat(eventCollector.events().get(0).questionId()).isEqualTo(fixture.followUpQuestionId);
        });
    }

    @Test
    @DisplayName("메인 + 후속 2턴 연속 → 서로 다른 question_id 로 2건 이벤트 발행 (중복/누락 없음)")
    void mainThenFollowUp_publishesTwoEventsWithDistinctQuestionIds() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_MAIN);
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(analysisOf("정상 답변", RecommendedNextAction.CLARIFICATION));
        given(followUpQuestionService.write(any(), any(), any(QuestionCategory.class)))
                .willReturn(new GeneratedFollowUp(
                        false, null, "심화 질문", "TTS", "이유", "claim", "best", null, 0));

        followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId, request(fixture.questionSetId), audio());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(eventCollector.events()).hasSize(1));

        followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId, request(fixture.questionSetId), audio());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(2);
            assertThat(eventCollector.events())
                    .extracting(AnswerAnalysisCompletedEvent::questionId)
                    .doesNotHaveDuplicates();
        });

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(3);
    }

    @Test
    @DisplayName("후속 2개 한도 도달 후 추가 답변 → 이벤트 발행 + 새 후속 미생성 + skip:true")
    void followUpExhausted_publishesEventButSkipsNewFollowUp() {
        Fixture fixture = persistResumeFixtureWithTwoFollowUps();
        given(audioTurnAnalysisService.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), any(QuestionCategory.class)))
                .willReturn(analysisOf("마지막 후속 답변", RecommendedNextAction.CLARIFICATION));

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId),
                audio());

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("followup_exhausted");
        assertThat(response.getAnswerText()).isEqualTo("마지막 후속 답변");
        verify(followUpQuestionService, never()).write(any(), any(), any(QuestionCategory.class));

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() ->
                assertThat(eventCollector.events()).hasSize(1));

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(3);
    }

    private FollowUpRequest request(Long questionSetId) {
        FollowUpRequest req = new FollowUpRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(req, "questionSetId", questionSetId);
        org.springframework.test.util.ReflectionTestUtils.setField(req, "questionContent", "메인 질문 본문");
        return req;
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "answer.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private Fixture persistResumeFixture(QuestionType mainType) {
        User user = userRepository.saveAndFlush(User.builder()
                .email("followup-" + mainType + "@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-" + mainType)
                .role(UserRole.USER)
                .build());

        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        Question mainQuestion = Question.builder()
                .questionType(mainType)
                .questionText("이력서 기반 메인 질문")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(0)
                .build();
        questionSet.addQuestion(mainQuestion);
        questionSetRepository.saveAndFlush(questionSet);

        return new Fixture(user.getId(), interview.getId(), questionSet.getId(), mainQuestion.getId(), null);
    }

    private Fixture persistResumeFixtureWithFollowUp(QuestionType mainType, QuestionType followUpType) {
        User user = userRepository.saveAndFlush(User.builder()
                .email("followup-fu-" + mainType + "@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-fu-" + mainType)
                .role(UserRole.USER)
                .build());

        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        Question mainQuestion = Question.builder()
                .questionType(mainType)
                .questionText("이력서 기반 메인 질문")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(0)
                .build();
        Question followUpQuestion = Question.builder()
                .questionType(followUpType)
                .questionText("이력서 기반 후속 질문")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(1)
                .build();
        questionSet.addQuestion(mainQuestion);
        questionSet.addQuestion(followUpQuestion);
        questionSetRepository.saveAndFlush(questionSet);

        return new Fixture(user.getId(), interview.getId(), questionSet.getId(),
                mainQuestion.getId(), followUpQuestion.getId());
    }

    private Fixture persistResumeFixtureWithTwoFollowUps() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("followup-exhausted@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-exhausted")
                .role(UserRole.USER)
                .build());

        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        Question mainQuestion = Question.builder()
                .questionType(QuestionType.RESUME_MAIN)
                .questionText("이력서 기반 메인 질문")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(0)
                .build();
        Question followUp1 = Question.builder()
                .questionType(QuestionType.RESUME_FOLLOWUP)
                .questionText("후속 질문 1")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(1)
                .build();
        Question followUp2 = Question.builder()
                .questionType(QuestionType.RESUME_FOLLOWUP)
                .questionText("후속 질문 2")
                .ttsText("TTS")
                .bestAnswer("best")
                .orderIndex(2)
                .build();
        questionSet.addQuestion(mainQuestion);
        questionSet.addQuestion(followUp1);
        questionSet.addQuestion(followUp2);
        questionSetRepository.saveAndFlush(questionSet);

        return new Fixture(user.getId(), interview.getId(), questionSet.getId(),
                followUp2.getId(), followUp2.getId());
    }

    private record Fixture(Long userId, Long interviewId, Long questionSetId, Long questionId, Long followUpQuestionId) {}

    @TestConfiguration
    static class TestEventCollectorConfig {
        @Bean
        AnswerAnalysisCompletedEventCollector answerAnalysisCompletedEventCollector() {
            return new AnswerAnalysisCompletedEventCollector();
        }
    }

    static class AnswerAnalysisCompletedEventCollector {
        private final List<AnswerAnalysisCompletedEvent> events = new CopyOnWriteArrayList<>();

        @EventListener
        public void on(AnswerAnalysisCompletedEvent event) {
            events.add(event);
        }

        public List<AnswerAnalysisCompletedEvent> events() {
            return List.copyOf(events);
        }

        public void clear() {
            events.clear();
        }
    }
}
