package com.rehearse.api.domain.feedback.rubric.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreDimensionRepository;
import com.rehearse.api.domain.feedback.score.repository.QuestionScoreRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
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
import com.rehearse.api.support.ServiceIntegrationSupport;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.ArrayList;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("NonverbalScorePersister - 영역 키 머지 + saveRubric 단일 진입")
class NonverbalScorePersisterTest extends ServiceIntegrationSupport {

    @Autowired
    private NonverbalScorePersister persister;

    @Autowired
    private InterviewRepository interviewRepository;

    @Autowired
    private QuestionSetRepository questionSetRepository;

    @Autowired
    private QuestionRepository questionRepository;

    @Autowired
    private QuestionScoreRepository questionScoreRepository;

    @Autowired
    private QuestionScoreDimensionRepository dimensionRepository;

    @Autowired
    private UserRepository userRepository;

    private ListAppender<ILoggingEvent> logAppender;
    private Logger persisterLogger;

    @BeforeEach
    void attachLogAppender() {
        persisterLogger = (Logger) LoggerFactory.getLogger(NonverbalScorePersister.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        persisterLogger.addAppender(logAppender);
    }

    @AfterEach
    void detachLogAppender() {
        persisterLogger.detachAppender(logAppender);
        logAppender.stop();
    }

    @Nested
    @DisplayName("정상 적재")
    class NormalPersist {

        @Test
        @DisplayName("vocal + vision 영역 dimension 머지 시 rubric_id=nonverbal-v1 1 row + dimension 3 row 적재")
        void persistAll_mergesVocalAndVision_savesRubricV1() {
            Persisted ctx = persistInterview();
            SaveFeedbackRequest.NonverbalScore payload = new SaveFeedbackRequest.NonverbalScore();
            setArea(payload, "vocal", List.of(
                    dim("fluency", 3, "유창하게 답변", "유창성 근거 인용"),
                    dim("confidence_tone", 2, "안정적인 톤", "확신 톤 근거")
            ));
            setArea(payload, "vision", List.of(
                    dim("eye_contact_posture", 2, "시선 안정", "시선 근거")
            ));

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(payload)));

            List<QuestionScore> scores = questionScoreRepository
                    .findByInterviewIdOrderByQuestionIdAsc(ctx.interview().getId());
            assertThat(scores).hasSize(1);
            QuestionScore score = scores.get(0);
            assertThat(score.getRubricId()).isEqualTo("nonverbal-v1");
            assertThat(score.getQuestionId()).isEqualTo(ctx.question().getId());

            List<QuestionScoreDimension> dims = dimensionRepository.findByQuestionScoreId(score.getId());
            assertThat(dims).extracting(QuestionScoreDimension::getDimensionRef)
                    .containsExactlyInAnyOrder("fluency", "confidence_tone", "eye_contact_posture");
            assertThat(dims).allSatisfy(d -> {
                assertThat(d.getObservation()).isNotBlank();
                assertThat(d.getEvidenceQuote()).isNotBlank();
            });
            assertThat(dims).extracting(QuestionScoreDimension::getDimensionRef)
                    .doesNotContain("composure");
            assertThat(questionScoreRepository.findByQuestionIdAndRubricId(
                    ctx.question().getId(), "nonverbal").isEmpty()).isTrue();
        }

        @Test
        @DisplayName("vocal 만 산출 (vision null) 시 vocal dimension 만 적재 — fault isolation")
        void persistAll_vocalOnly_persistsOnlyVocalDimensions() {
            Persisted ctx = persistInterview();
            SaveFeedbackRequest.NonverbalScore payload = new SaveFeedbackRequest.NonverbalScore();
            setArea(payload, "vocal", List.of(
                    dim("fluency", 3, "유창함", "유창성 근거"),
                    dim("confidence_tone", 2, "안정적", "톤 근거")
            ));

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(payload)));

            List<QuestionScore> scores = questionScoreRepository
                    .findByInterviewIdOrderByQuestionIdAsc(ctx.interview().getId());
            assertThat(scores).hasSize(1);
            List<QuestionScoreDimension> dims = dimensionRepository
                    .findByQuestionScoreId(scores.get(0).getId());
            assertThat(dims).extracting(QuestionScoreDimension::getDimensionRef)
                    .containsExactlyInAnyOrder("fluency", "confidence_tone");
        }

        @Test
        @DisplayName("dimension 일부가 observation null 이면 해당 dimension 만 skip 하고 나머지는 적재한다")
        void persistAll_partialInvalidDimension_skipsInvalidOnly() {
            Persisted ctx = persistInterview();
            SaveFeedbackRequest.NonverbalScore payload = new SaveFeedbackRequest.NonverbalScore();
            setArea(payload, "vocal", List.of(
                    dim("fluency", 3, "유창함", "유창성 근거"),
                    dim("confidence_tone", 2, null, "톤 근거")
            ));

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(payload)));

            List<QuestionScore> scores = questionScoreRepository
                    .findByInterviewIdOrderByQuestionIdAsc(ctx.interview().getId());
            assertThat(scores).hasSize(1);
            List<QuestionScoreDimension> dims = dimensionRepository
                    .findByQuestionScoreId(scores.get(0).getId());
            assertThat(dims).extracting(QuestionScoreDimension::getDimensionRef)
                    .containsExactly("fluency");
        }
    }

    @Nested
    @DisplayName("skip 분기")
    class SkipBranches {

        @Test
        @DisplayName("payload null → [정상 skip] payloadNull INFO 로그 + row 0건")
        void persistAll_payloadNull_logsNormalSkipAndDoesNotPersist() {
            Persisted ctx = persistInterview();

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(null)));

            assertNoQuestionScoreFor(ctx.interview().getId());
            assertThat(infoMessages())
                    .anyMatch(m -> m.contains("[정상 skip] Nonverbal payloadNull")
                            && m.contains("interviewId=" + ctx.interview().getId())
                            && m.contains("questionId=" + ctx.question().getId()));
            assertThat(warnMessages()).noneMatch(m -> m.contains("[결함 skip]"));
        }

        @Test
        @DisplayName("vocal/vision 둘 다 null → [결함 skip] areasEmpty WARN 로그 + row 0건")
        void persistAll_areasEmpty_logsDefectSkipAndDoesNotPersist() {
            Persisted ctx = persistInterview();
            SaveFeedbackRequest.NonverbalScore payload = new SaveFeedbackRequest.NonverbalScore();

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(payload)));

            assertNoQuestionScoreFor(ctx.interview().getId());
            assertThat(warnMessages())
                    .anyMatch(m -> m.contains("[결함 skip] Nonverbal areasEmpty")
                            && m.contains("interviewId=" + ctx.interview().getId())
                            && m.contains("questionId=" + ctx.question().getId()));
        }

        @Test
        @DisplayName("vocal + vision 에 동일 dimension key 가 들어오면 vision 쪽을 skip 하고 dimensionKeyConflict WARN 을 남긴다")
        void persistAll_dimensionKeyConflict_logsWarnAndKeepsFirstArea() {
            Persisted ctx = persistInterview();
            SaveFeedbackRequest.NonverbalScore payload = new SaveFeedbackRequest.NonverbalScore();
            setArea(payload, "vocal", List.of(
                    dim("fluency", 3, "유창함 vocal", "vocal 근거")
            ));
            setArea(payload, "vision", List.of(
                    dim("fluency", 1, "유창함 vision", "vision 근거")
            ));

            persister.persistAll(ctx.questionSet(), ctx.feedback(), List.of(item(payload)));

            List<QuestionScore> scores = questionScoreRepository
                    .findByInterviewIdOrderByQuestionIdAsc(ctx.interview().getId());
            assertThat(scores).hasSize(1);
            List<QuestionScoreDimension> dims = dimensionRepository
                    .findByQuestionScoreId(scores.get(0).getId());
            // 첫 area (vocal) 값 유지, 두 번째 (vision) skip.
            assertThat(dims).hasSize(1);
            assertThat(dims.get(0).getDimensionRef()).isEqualTo("fluency");
            assertThat(dims.get(0).getScore()).isEqualTo(3);
            assertThat(dims.get(0).getObservation()).isEqualTo("유창함 vocal");

            assertThat(warnMessages())
                    .anyMatch(m -> m.contains("[결함 skip] Nonverbal dimensionKeyConflict")
                            && m.contains("interviewId=" + ctx.interview().getId())
                            && m.contains("questionId=" + ctx.question().getId())
                            && m.contains("key=fluency")
                            && m.contains("skippedArea=vision"));
        }
    }

    private Persisted persistInterview() {
        User user = userRepository.saveAndFlush(User.builder()
                .email("nonverbal-test@example.com")
                .name("테스터")
                .provider(OAuthProvider.GITHUB)
                .providerId("github-nonverbal-test")
                .role(UserRole.USER)
                .build());
        Interview interview = TestFixtures.createInterview(
                user.getId(), Position.BACKEND, InterviewLevel.JUNIOR, List.of(InterviewType.RESUME_BASED));
        interviewRepository.saveAndFlush(interview);

        QuestionSet questionSet = TestFixtures.createQuestionSet(interview, InterviewType.RESUME_BASED, 0);
        questionSetRepository.saveAndFlush(questionSet);

        Question question = Question.resume(
                questionSet, QuestionType.RESUME_OPENER,
                "프로젝트 경험을 설명해주세요.", null, null, 0, null);
        questionSet.addQuestion(question);
        questionRepository.saveAndFlush(question);

        QuestionSetFeedback feedback = new QuestionSetFeedback(questionSet, "코멘트");
        TimestampFeedback ts = TimestampFeedback.builder()
                .question(question)
                .startMs(0L)
                .endMs(1000L)
                .isAnalyzed(true)
                .build();
        feedback.addTimestampFeedback(ts);
        return new Persisted(interview, questionSet, question, feedback);
    }

    private void assertNoQuestionScoreFor(Long interviewId) {
        assertThat(questionScoreRepository.findByInterviewIdOrderByQuestionIdAsc(interviewId)).isEmpty();
    }

    private SaveFeedbackRequest.TimestampFeedbackItem item(SaveFeedbackRequest.NonverbalScore payload) {
        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 1000L);
        ReflectionTestUtils.setField(item, "difficulty", "easy");
        ReflectionTestUtils.setField(item, "resumeMode", "PLAYGROUND");
        ReflectionTestUtils.setField(item, "nonverbalScore", payload);
        return item;
    }

    private SaveFeedbackRequest.DimensionScoreItem dim(String ref, Integer score,
                                                      String observation, String evidence) {
        SaveFeedbackRequest.DimensionScoreItem dim = new SaveFeedbackRequest.DimensionScoreItem();
        ReflectionTestUtils.setField(dim, "dimensionRef", ref);
        ReflectionTestUtils.setField(dim, "score", score);
        ReflectionTestUtils.setField(dim, "observation", observation);
        ReflectionTestUtils.setField(dim, "evidenceQuote", evidence);
        return dim;
    }

    private void setArea(SaveFeedbackRequest.NonverbalScore payload, String areaName,
                         List<SaveFeedbackRequest.DimensionScoreItem> dimensions) {
        SaveFeedbackRequest.AreaScore area = new SaveFeedbackRequest.AreaScore();
        ReflectionTestUtils.setField(area, "dimensions", new ArrayList<>(dimensions));
        ReflectionTestUtils.setField(payload, areaName, area);
    }

    private List<String> infoMessages() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.INFO)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private List<String> warnMessages() {
        return logAppender.list.stream()
                .filter(event -> event.getLevel() == Level.WARN)
                .map(ILoggingEvent::getFormattedMessage)
                .toList();
    }

    private record Persisted(Interview interview, QuestionSet questionSet,
                             Question question, QuestionSetFeedback feedback) {
    }
}
