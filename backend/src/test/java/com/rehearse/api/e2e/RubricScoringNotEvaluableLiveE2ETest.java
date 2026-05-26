package com.rehearse.api.e2e;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoringResult;
import com.rehearse.api.domain.feedback.rubric.service.RubricScorer;
import com.rehearse.api.domain.feedback.score.entity.DimensionStatus;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.interview.service.AudioTurnAnalyzer;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.domain.user.repository.UserRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.Assumptions;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@Disabled("Live LLM E2E — RUN_LIVE_API=true 환경변수로만 활성")
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@DisplayName("NOT_EVALUABLE 오판정 해소 Live LLM E2E — 실제 음성 분석 + 채점 경로 검증")
class RubricScoringNotEvaluableLiveE2ETest extends ServiceIntegrationSupport {

    private static final String LONG_AUDIO = "fixtures/audio/not-evaluable/long-korean.webm";
    private static final String SILENT_AUDIO = "fixtures/audio/not-evaluable/silent-5s.webm";
    private static final String MAIN_QUESTION = "JVM 가비지 컬렉션의 동작 방식을 설명해주세요.";

    @Autowired
    private AudioTurnAnalyzer audioTurnAnalyzer;

    @Autowired
    private RubricScorer rubricScorer;

    @Autowired
    private UserRepository userRepository;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Test
    @DisplayName("(1) 긴 음성 답변 + FE 송신 텍스트 빈 문자열 → 차원별 점수 정상 산출 (NOT_EVALUABLE 0건)")
    void scenario1_longAudio_emptyUserAnswer_scoresAllDimensions() {
        Fixture fixture = persistFixture();
        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                fixture.interviewId, loadAudio(LONG_AUDIO), MAIN_QUESTION, ReferenceType.MODEL_ANSWER, false);

        assertThat(analysis.transcript().strip().length())
                .as("음성 전사 텍스트가 임계(3자) 초과로 확보됨").isGreaterThan(3);

        RubricScoringResult result = rubricScorer.score(
                fixture.question, fixture.questionSet, fixture.interview, "", analysis);

        assertNoNotEvaluable(result);
    }

    @Test
    @DisplayName("(2) 긴 음성 답변 + 정상 LLM 응답 → 차원별 점수 정상 산출")
    void scenario2_longAudio_normalUserAnswer_scoresAllDimensions() {
        Fixture fixture = persistFixture();
        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                fixture.interviewId, loadAudio(LONG_AUDIO), MAIN_QUESTION, ReferenceType.MODEL_ANSWER, false);

        RubricScoringResult result = rubricScorer.score(
                fixture.question, fixture.questionSet, fixture.interview,
                "JVM 은 힙을 Young 과 Old 로 나누어 관리합니다.", analysis);

        assertNoNotEvaluable(result);
    }

    @Test
    @DisplayName("(3) 무음 + 텍스트 임계 미만 → 전 차원 NOT_EVALUABLE 정상 적용")
    void scenario3_silentAudio_shortUserAnswer_marksNotEvaluable() {
        Fixture fixture = persistFixture();
        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                fixture.interviewId, loadAudio(SILENT_AUDIO), MAIN_QUESTION, ReferenceType.MODEL_ANSWER, false);

        RubricScoringResult result = rubricScorer.score(
                fixture.question, fixture.questionSet, fixture.interview, "잘", analysis);

        assertAllNotEvaluable(result);
    }

    @Test
    @DisplayName("(4) 긴 답변 + LLM observation 정상 → '관련 발언 없음' 트리거 미작동")
    void scenario4_longAudio_noRelatedRemarkTrigger() {
        Fixture fixture = persistFixture();
        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                fixture.interviewId, loadAudio(LONG_AUDIO), MAIN_QUESTION, ReferenceType.MODEL_ANSWER, false);

        RubricScoringResult result = rubricScorer.score(
                fixture.question, fixture.questionSet, fixture.interview,
                "JVM 은 힙을 Young 과 Old 로 나누어 관리합니다.", analysis);

        long notEvaluable = result.scoredDimensions().stream()
                .filter(dim -> result.dimensionScores().get(dim).status() == DimensionStatus.NOT_EVALUABLE)
                .count();
        assertThat(notEvaluable).as("'관련 발언 없음' sentinel 트리거 0회").isZero();
    }

    @Test
    @DisplayName("(5) 긴 답변 + validator 통과 LLM 응답 → validator 트리거 미작동 (예외 없이 점수 산출)")
    void scenario5_longAudio_validatorPasses() {
        Fixture fixture = persistFixture();
        AnswerAnalysis analysis = audioTurnAnalyzer.analyze(
                fixture.interviewId, loadAudio(LONG_AUDIO), MAIN_QUESTION, ReferenceType.MODEL_ANSWER, false);

        RubricScoringResult result = rubricScorer.score(
                fixture.question, fixture.questionSet, fixture.interview,
                "JVM 은 힙을 Young 과 Old 로 나누어 관리합니다.", analysis);

        assertThat(result.scoredDimensions()).as("validator 차단 없이 차원 채점 완료").isNotEmpty();
    }

    private void assertNoNotEvaluable(RubricScoringResult result) {
        for (String dim : result.scoredDimensions()) {
            assertThat(result.dimensionScores().get(dim).status())
                    .as("차원 %s 는 NOT_EVALUABLE 가 아니어야 함", dim)
                    .isNotEqualTo(DimensionStatus.NOT_EVALUABLE);
        }
    }

    private void assertAllNotEvaluable(RubricScoringResult result) {
        assertThat(result.scoredDimensions()).isNotEmpty();
        for (String dim : result.scoredDimensions()) {
            assertThat(result.dimensionScores().get(dim).status())
                    .as("차원 %s 는 NOT_EVALUABLE 여야 함", dim)
                    .isEqualTo(DimensionStatus.NOT_EVALUABLE);
        }
    }

    private MultipartFile loadAudio(String classpathLocation) {
        try (InputStream stream = getClass().getClassLoader().getResourceAsStream(classpathLocation)) {
            Assumptions.assumeTrue(stream != null,
                    "audio fixture 부재 — " + classpathLocation + " 생성 후 재실행");
            return new MockMultipartFile("audio", "audio.webm", "audio/webm", stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException("audio fixture 로드 실패: " + classpathLocation, e);
        }
    }

    private Fixture persistFixture() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("live-not-evaluable@example.com")
                .name("Live 테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-live-not-evaluable")
                .role(UserRole.USER)
                .build());

        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.MID, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        questionSetRepository.saveAndFlush(questionSet);

        Question question = TestFixtures.createResumeQuestion(QuestionType.RESUME_MAIN);
        questionRepository.saveAndFlush(question);

        return new Fixture(interview.getId(), interview, questionSet, question);
    }

    private record Fixture(Long interviewId, Interview interview, QuestionSet questionSet, Question question) {
    }
}
