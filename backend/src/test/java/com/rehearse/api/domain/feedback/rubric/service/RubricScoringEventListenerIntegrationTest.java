package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.interview.service.FollowUpService;
import com.rehearse.api.domain.interview.service.FollowUpTransactionHandler;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.service.ResumeTurnEventPublisher;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.ResilientAiClient;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.Duration;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.reset;

@DisplayName("RubricScoringEventListener Service Integration")
class RubricScoringEventListenerIntegrationTest extends ServiceIntegrationSupport {

    @Autowired
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Autowired
    private FollowUpService followUpService;

    @Autowired
    private ResumeTurnEventPublisher resumeTurnEventPublisher;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionScoreRepository questionScoreRepository;

    @Autowired
    private QuestionScoreDimensionRepository questionScoreDimensionRepository;

    @MockitoBean
    private ResilientAiClient resilientAiClient;

    @BeforeEach
    void resetAiMock() {
        reset(resilientAiClient);
    }

    @Nested
    @DisplayName("FollowUpQuestionCreatedEvent 적재")
    class FollowUpQuestionCreatedEventPersist {

        @Test
        @DisplayName("정상 1턴 STANDARD 답변은 question_score 와 dimension 을 적재한다")
        void standardAnswer_persistsQuestionScore() {
            InterviewData data = persistInterview(InterviewType.CS_FUNDAMENTAL, InterviewType.CS_FUNDAMENTAL, QuestionType.TECH_MAIN);
            given(resilientAiClient.chat(any())).willReturn(rubricResponse());

            followUpTransactionHandler.publishFollowUpQuestionCreatedEvent(
                    data.interview().getId(), context(data), answerTurn(RecommendedNextAction.DEEP_DIVE),
                    data.question().getId());

            QuestionScore score = awaitScores(data.interview().getId(), 1).get(0);
            assertThat(score.getQuestionId()).isEqualTo(data.question().getId());
            assertThat(questionScoreDimensionRepository.findByQuestionScoreId(score.getId())).isNotEmpty();
        }

        @Test
        @DisplayName("RESUME_BASED 1턴 정상 publish 는 question_score 를 적재한다")
        void resumeAnswer_persistsQuestionScore() {
            InterviewData data = persistInterview(InterviewType.RESUME_BASED, InterviewType.RESUME_BASED,
                    QuestionType.RESUME_PLAYGROUND);
            given(resilientAiClient.chat(any())).willReturn(rubricResponse());

            resumeTurnEventPublisher.publish(
                    data.interview().getId(), 0L, answerAnalysis(RecommendedNextAction.DEEP_DIVE),
                    ResumeMode.PLAYGROUND, 1, null, "답변 텍스트", data.question().getId());

            QuestionScore score = awaitScores(data.interview().getId(), 1).get(0);
            assertThat(score.getQuestionId()).isEqualTo(data.question().getId());
            assertThat(questionScoreDimensionRepository.findByQuestionScoreId(score.getId())).isNotEmpty();
        }

        @Test
        @DisplayName("analyzer_skip 분기는 publish 후 question_score 를 적재한다")
        void analyzerSkip_publishesAndPersists() {
            InterviewData data = persistInterview(InterviewType.CS_FUNDAMENTAL, InterviewType.CS_FUNDAMENTAL, QuestionType.TECH_MAIN);
            given(resilientAiClient.chatWithAudio(any(), any())).willReturn(analyzerResponse(RecommendedNextAction.SKIP));
            given(resilientAiClient.chat(any())).willReturn(rubricResponse());

            followUpService.generateFollowUp(data.interview().getId(), data.interview().getUserId(), request(data), audio());

            assertThat(awaitScores(data.interview().getId(), 1)).hasSize(1);
        }

        @Test
        @DisplayName("step_b_skip 분기는 publish 후 question_score 를 적재한다")
        void stepBSkip_publishesAndPersists() {
            InterviewData data = persistInterview(InterviewType.CS_FUNDAMENTAL, InterviewType.CS_FUNDAMENTAL, QuestionType.TECH_MAIN);
            given(resilientAiClient.chatWithAudio(any(), any())).willReturn(analyzerResponse(RecommendedNextAction.DEEP_DIVE));
            given(resilientAiClient.chat(any())).willReturn(stepBSkipResponse(), rubricResponse());

            followUpService.generateFollowUp(data.interview().getId(), data.interview().getUserId(), request(data), audio());

            assertThat(awaitScores(data.interview().getId(), 1)).hasSize(1);
        }

        @Test
        @DisplayName("verbal 채점 결과는 한국어 observation + userAnswer substring evidence_quote 로 적재된다")
        void verbalScoring_persistsKoreanObservationAndVerbatimEvidence() {
            InterviewData data = persistInterview(InterviewType.CS_FUNDAMENTAL, InterviewType.CS_FUNDAMENTAL, QuestionType.TECH_MAIN);
            given(resilientAiClient.chat(any())).willReturn(rubricResponse());

            followUpTransactionHandler.publishFollowUpQuestionCreatedEvent(
                    data.interview().getId(), context(data), answerTurn(RecommendedNextAction.DEEP_DIVE),
                    data.question().getId());

            QuestionScore score = awaitScores(data.interview().getId(), 1).get(0);
            var dimensions = questionScoreDimensionRepository.findByQuestionScoreId(score.getId());
            assertThat(dimensions).isNotEmpty();
            assertThat(dimensions).allSatisfy(dim -> {
                assertThat(dim.getObservation()).matches(".*[\\uAC00-\\uD7A3].*");
                assertThat(dim.getEvidenceQuote()).isEqualTo("답변 텍스트");
                assertThat(dim.getEvidenceQuote()).doesNotContain("팀에서 했어요 수준");
            });
        }

        @Test
        @DisplayName("Resume publish 시 questionId null 은 발행 skip 되어 question_score 를 적재하지 않는다")
        void resumeNullQuestionId_skipsPublish() {
            InterviewData data = persistInterview(InterviewType.RESUME_BASED, InterviewType.RESUME_BASED,
                    QuestionType.RESUME_PLAYGROUND);

            resumeTurnEventPublisher.publish(
                    data.interview().getId(), 0L, answerAnalysis(RecommendedNextAction.DEEP_DIVE),
                    ResumeMode.PLAYGROUND, 1, null, "답변 텍스트", null);

            assertNoScoresDuring(data.interview().getId(), Duration.ofMillis(500));
            then(resilientAiClient).shouldHaveNoInteractions();
        }
    }

    private InterviewData persistInterview(InterviewType category, InterviewType type, QuestionType questionType) {
        User user = userRepository.saveAndFlush(User.builder()
                .email("test@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-test")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(type));
        interview.completeQuestionGeneration();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, category, 0);
        questionSetRepository.saveAndFlush(questionSet);

        Question question = questionType.name().startsWith("RESUME_")
                ? Question.resume(questionSet, questionType, "프로젝트 경험을 설명해주세요.", null, null, 0)
                : Question.builder()
                .questionType(questionType)
                .questionText("HashMap과 TreeMap의 차이점은?")
                .orderIndex(0)
                .build();
        questionSet.addQuestion(question);
        questionRepository.saveAndFlush(question);
        return new InterviewData(interview, questionSet, question);
    }

    private com.rehearse.api.domain.interview.dto.FollowUpContext context(InterviewData data) {
        return new com.rehearse.api.domain.interview.dto.FollowUpContext(
                data.interview().getPosition(), data.interview().getEffectiveTechStack(), data.interview().getLevel(),
                data.questionSet().getId(), data.question().getId(), 1, ReferenceType.MODEL_ANSWER, 2);
    }

    private FollowUpRequest request(InterviewData data) {
        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", data.questionSet().getId());
        ReflectionTestUtils.setField(request, "questionContent", data.question().getQuestionText());
        ReflectionTestUtils.setField(request, "answerText", "답변 텍스트");
        ReflectionTestUtils.setField(request, "previousExchanges", List.of());
        return request;
    }

    private TurnAnalysisResult answerTurn(RecommendedNextAction action) {
        return new TurnAnalysisResult("답변 텍스트", answerAnalysis(action));
    }

    private AnswerAnalysis answerAnalysis(RecommendedNextAction action) {
        return new AnswerAnalysis(50L, List.of(), List.of(), List.of(), 3, action);
    }

    private List<QuestionScore> awaitScores(Long interviewId, int expectedSize) {
        Duration timeout = Duration.ofSeconds(5);
        long deadline = System.nanoTime() + timeout.toNanos();
        List<QuestionScore> scores;
        do {
            scores = questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId);
            if (scores.size() == expectedSize) {
                return scores;
            }
            waitBriefly();
        } while (System.nanoTime() < deadline);
        assertThat(scores).hasSize(expectedSize);
        return scores;
    }

    private void assertNoScoresDuring(Long interviewId, Duration duration) {
        long deadline = System.nanoTime() + duration.toNanos();
        do {
            assertThat(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId)).isEmpty();
            waitBriefly();
        } while (System.nanoTime() < deadline);
    }

    private void waitBriefly() {
        try {
            Thread.sleep(50);
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new AssertionError("비동기 리스너 검증 대기 중 interrupt 발생", e);
        }
    }

    private ChatResponse analyzerResponse(RecommendedNextAction action) {
        return chat("""
                {
                  "answer_text": "답변 텍스트",
                  "answer_analysis": {
                    "turn_id": 50,
                    "claims": [],
                    "missing_perspectives": [],
                    "unstated_assumptions": [],
                    "answer_quality": 3,
                    "recommended_next_action": "%s"
                  }
                }
                """.formatted(action.name()));
    }

    private ChatResponse stepBSkipResponse() {
        return chat("""
                {"skip": true, "skip_reason": "답변이 main_question 과 무관"}
                """);
    }

    private ChatResponse rubricResponse() {
        return chat("""
                {
                  "technical_depth": {"score": 2, "observation": "기술 깊이 확인", "evidence_quote": "답변 텍스트"},
                  "reasoning_communication": {"score": 2, "observation": "설명 흐름 확인", "evidence_quote": "답변 텍스트"},
                  "conceptual_accuracy": {"score": 2, "observation": "개념 정확성 확인", "evidence_quote": "답변 텍스트"},
                  "recovery_from_gaps": {"score": 2, "observation": "보완 가능성 확인", "evidence_quote": "답변 텍스트"},
                  "experience_concreteness": {"score": 2, "observation": "경험 구체성 확인", "evidence_quote": "답변 텍스트"},
                  "factual_consistency": {"score": 2, "observation": "사실 일치 확인", "evidence_quote": "답변 텍스트"},
                  "chain_depth": {"score": 2, "observation": "체인 깊이 확인", "evidence_quote": "답변 텍스트"}
                }
                """);
    }

    private ChatResponse chat(String content) {
        return new ChatResponse(content, ChatResponse.Usage.empty(), "mock", "test", false, false);
    }

    private MockMultipartFile audio() {
        return new MockMultipartFile("audio", "answer.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private record InterviewData(Interview interview, QuestionSet questionSet, Question question) {
    }
}
