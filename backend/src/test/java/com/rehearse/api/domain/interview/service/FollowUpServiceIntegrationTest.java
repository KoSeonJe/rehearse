package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
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
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;

@DisplayName("FollowUpService — RESUME_OPENER 답변도 분석/채점 후 follow-up 생성만 skip")
@Import(FollowUpServiceIntegrationTest.TestEventCollectorConfig.class)
class FollowUpServiceIntegrationTest extends ServiceIntegrationSupport {

    @Autowired private FollowUpService followUpService;
    @Autowired private UserRepository userRepository;
    @Autowired private InterviewRepository interviewRepository;
    @Autowired private QuestionSetRepository questionSetRepository;
    @Autowired private QuestionRepository questionRepository;
    @Autowired private QuestionScoreRepository questionScoreRepository;
    @Autowired private AnswerAnalysisCompletedEventCollector eventCollector;

    @MockitoBean private AudioTurnAnalyzer audioTurnAnalyzer;
    @MockitoBean private FollowUpQuestionWriter followUpQuestionWriter;
    @MockitoBean private RubricScorer rubricScorer;

    private static final AnswerAnalysis SAMPLE_ANALYSIS = new AnswerAnalysis(
            List.of(), Map.of(), null, List.of(), RecommendedNextAction.CLARIFICATION);

    @BeforeEach
    void resetCollector() {
        eventCollector.clear();
    }

    @Test
    @DisplayName("RESUME_OPENER → analyzer 1회 호출 + 이벤트 발행 + QuestionScore 적재 + follow-up 미생성")
    void resumeOpener_publishesEvent_persistsScore_andSkipsFollowUp() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_OPENER);
        given(audioTurnAnalyzer.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), anyBoolean()))
                .willReturn(SAMPLE_ANALYSIS);
        given(rubricScorer.score(any(Question.class), any(QuestionSet.class), any(Interview.class), any(), any()))
                .willReturn(rubricResult());

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId, "원문 답변"),
                audio());

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("resume_opener_skip");
        verify(audioTurnAnalyzer, times(1))
                .analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), anyBoolean());
        verify(followUpQuestionWriter, never()).write(any(), any(), any(), any());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(1);
            AnswerAnalysisCompletedEvent event = eventCollector.events().get(0);
            assertThat(event.questionId()).isEqualTo(fixture.questionId);
            assertThat(event.userAnswer()).isEqualTo("원문 답변");

            List<QuestionScore> scores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(fixture.interviewId);
            assertThat(scores).hasSize(1);
            assertThat(scores.get(0).getQuestionId()).isEqualTo(fixture.questionId);
        });

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(1);
    }

    @Test
    @DisplayName("RESUME_MAIN 일반 답변 → analyzer + 이벤트 + 채점 + follow-up 생성")
    void resumeMain_followsUpAndPersistsScore() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_MAIN);
        given(audioTurnAnalyzer.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), anyBoolean()))
                .willReturn(SAMPLE_ANALYSIS);
        given(rubricScorer.score(any(Question.class), any(QuestionSet.class), any(Interview.class), any(), any()))
                .willReturn(rubricResult());
        given(followUpQuestionWriter.write(any(), any(), any(), any()))
                .willReturn(new GeneratedFollowUp(
                        false, null, "심화 질문", "TTS", "이유", "claim", "best", null, 0));

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId, "정상 답변"),
                audio());

        assertThat(response.isSkip()).isFalse();
        assertThat(response.getQuestion()).isEqualTo("심화 질문");
        verify(audioTurnAnalyzer, times(1))
                .analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), anyBoolean());
        verify(followUpQuestionWriter, times(1)).write(any(), any(), any(), any());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(1);
            List<QuestionScore> scores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(fixture.interviewId);
            assertThat(scores).hasSize(1);
        });

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(2);
        assertThat(finalQuestions.get(1).getQuestionType()).isEqualTo(QuestionType.RESUME_FOLLOWUP);
    }

    @Test
    @DisplayName("analyzer SKIP 권고 → 이벤트 발행 + writer 미호출 + follow-up 미생성")
    void analyzerSkip_publishesEventAndSkipsWriter() {
        Fixture fixture = persistResumeFixture(QuestionType.RESUME_MAIN);
        AnswerAnalysis skipAnalysis = new AnswerAnalysis(
                List.of(), Map.of(), null, List.of(), RecommendedNextAction.SKIP);
        given(audioTurnAnalyzer.analyze(eq(fixture.interviewId), any(MultipartFile.class), any(), any(), anyBoolean()))
                .willReturn(skipAnalysis);
        given(rubricScorer.score(any(Question.class), any(QuestionSet.class), any(Interview.class), any(), any()))
                .willReturn(rubricResult());

        FollowUpResponse response = followUpService.generateFollowUp(
                fixture.interviewId, fixture.userId,
                request(fixture.questionSetId, "부족 답변"),
                audio());

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("analyzer_recommend_skip");
        verify(followUpQuestionWriter, never()).write(any(), any(), any(), any());

        Awaitility.await().atMost(Duration.ofSeconds(5)).untilAsserted(() -> {
            assertThat(eventCollector.events()).hasSize(1);
            List<QuestionScore> scores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(fixture.interviewId);
            assertThat(scores).hasSize(1);
        });

        List<Question> finalQuestions = questionRepository
                .findByQuestionSetIdOrderByOrderIndex(fixture.questionSetId);
        assertThat(finalQuestions).hasSize(1);
    }

    private FollowUpRequest request(Long questionSetId, String answerText) {
        FollowUpRequest req = new FollowUpRequest();
        org.springframework.test.util.ReflectionTestUtils.setField(req, "questionSetId", questionSetId);
        org.springframework.test.util.ReflectionTestUtils.setField(req, "questionContent", "메인 질문 본문");
        org.springframework.test.util.ReflectionTestUtils.setField(req, "answerText", answerText);
        return req;
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "answer.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private RubricScoringResult rubricResult() {
        Map<String, DimensionScore> dims = Map.of(
                "clarity", new DimensionScore(3, "관찰", "evidence"));
        return new RubricScoringResult("resume-v1", List.of("clarity"), dims, "L2");
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

        return new Fixture(user.getId(), interview.getId(), questionSet.getId(), mainQuestion.getId());
    }

    private record Fixture(Long userId, Long interviewId, Long questionSetId, Long questionId) {}

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
