package com.rehearse.api.domain.feedback.rubric.service;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.entity.QuestionType;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.slf4j.LoggerFactory;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("NonverbalScorePersister - silent skip 분류 로그 (Issue #472)")
class NonverbalScorePersisterTest {

    private static final Long INTERVIEW_ID = 100L;
    private static final Long QUESTION_ID = 200L;

    @InjectMocks
    private NonverbalScorePersister persister;

    @Mock
    private NonverbalRubricScorer nonverbalRubricScorer;

    @Mock
    private QuestionScorePersister questionScorePersister;

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
        @DisplayName("scorer 가 점수를 반환하면 nonverbal QuestionScore 가 적재되고 skip 로그는 남지 않는다")
        void persistAll_validScore_savesNonverbalAndLogsNoSkip() {
            QuestionSet questionSet = createResumeQuestionSet();
            QuestionSetFeedback feedback = createFeedbackWith(questionSet);

            SaveFeedbackRequest.NonverbalScore payload = createPayload(3, 2, 1, 2);
            SaveFeedbackRequest.TimestampFeedbackItem item = createItem(payload);
            given(nonverbalRubricScorer.score(eq(payload), anyString(), any(), any(), anyString()))
                    .willReturn(new NonverbalTurnScore(3, 2, 1, 2, Map.of(), 1.0));

            persister.persistAll(questionSet, feedback, List.of(item));

            ArgumentCaptor<Map<String, DimensionScore>> dimsCaptor = dimensionsCaptor();
            then(questionScorePersister).should()
                    .saveNonverbal(eq(QUESTION_ID), eq(INTERVIEW_ID), dimsCaptor.capture());
            assertThat(dimsCaptor.getValue()).containsKeys(
                    "fluency", "confidence_tone", "eye_contact_posture", "composure");
            assertThat(skipMessages()).isEmpty();
        }
    }

    @Nested
    @DisplayName("정상 skip — payload null")
    class PayloadNullSkip {

        @Test
        @DisplayName("Lambda payload 가 null 이면 [정상 skip] payloadNull 로그를 남기고 적재하지 않는다")
        void persistAll_payloadNull_logsNormalSkipAndDoesNotPersist() {
            QuestionSet questionSet = createResumeQuestionSet();
            QuestionSetFeedback feedback = createFeedbackWith(questionSet);

            SaveFeedbackRequest.TimestampFeedbackItem item = createItem(null);
            given(nonverbalRubricScorer.score(eq(null), anyString(), any(), any(), anyString()))
                    .willReturn(NonverbalTurnScore.empty());

            persister.persistAll(questionSet, feedback, List.of(item));

            then(questionScorePersister).should(never()).saveNonverbal(anyLong(), anyLong(), any());
            assertThat(infoMessages())
                    .as("[정상 skip] payloadNull 로그 발생")
                    .anyMatch(m -> m.contains("[정상 skip] Nonverbal payloadNull")
                            && m.contains("interviewId=" + INTERVIEW_ID)
                            && m.contains("questionId=" + QUESTION_ID));
            assertThat(warnMessages())
                    .as("결함 skip 로그 미발생")
                    .noneMatch(m -> m.contains("[결함 skip]"));
        }
    }

    @Nested
    @DisplayName("결함 skip — payload 있는데 scorer 결과 비어있음")
    class ScoreEmptySkip {

        @Test
        @DisplayName("payload 가 있는데 scorer 결과 hasAnyScore=false 이면 [결함 skip] scoreEmpty WARN 로그를 남긴다")
        void persistAll_scoreEmpty_logsDefectSkipAndDoesNotPersist() {
            QuestionSet questionSet = createResumeQuestionSet();
            QuestionSetFeedback feedback = createFeedbackWith(questionSet);

            SaveFeedbackRequest.NonverbalScore payload = createPayload(null, null, null, null);
            SaveFeedbackRequest.TimestampFeedbackItem item = createItem(payload);
            given(nonverbalRubricScorer.score(eq(payload), anyString(), any(), any(), anyString()))
                    .willReturn(NonverbalTurnScore.empty());

            persister.persistAll(questionSet, feedback, List.of(item));

            then(questionScorePersister).should(never()).saveNonverbal(anyLong(), anyLong(), any());
            assertThat(warnMessages())
                    .as("[결함 skip] scoreEmpty WARN 로그 발생")
                    .anyMatch(m -> m.contains("[결함 skip] Nonverbal scoreEmpty")
                            && m.contains("interviewId=" + INTERVIEW_ID)
                            && m.contains("questionId=" + QUESTION_ID));
            assertThat(infoMessages())
                    .as("정상 skip 로그 미발생")
                    .noneMatch(m -> m.contains("[정상 skip]"));
        }
    }

    // ----------------------------------------------------------------
    // helpers
    // ----------------------------------------------------------------

    private QuestionSet createResumeQuestionSet() {
        Interview interview = Interview.builder()
                .userId(1L)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .techStack(TechStack.JAVA_SPRING)
                .build();
        ReflectionTestUtils.setField(interview, "id", INTERVIEW_ID);

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.RESUME_BASED)
                .orderIndex(0)
                .build();
        return qs;
    }

    private QuestionSetFeedback createFeedbackWith(QuestionSet questionSet) {
        Question question = Question.resume(
                questionSet, QuestionType.RESUME_PLAYGROUND,
                "이 경험을 더 설명해주세요", null, null, 0);
        ReflectionTestUtils.setField(question, "id", QUESTION_ID);
        questionSet.addQuestion(question);

        QuestionSetFeedback feedback = new QuestionSetFeedback(questionSet, "코멘트");
        TimestampFeedback ts = TimestampFeedback.builder()
                .question(question)
                .startMs(0L)
                .endMs(1000L)
                .isAnalyzed(true)
                .build();
        feedback.addTimestampFeedback(ts);
        return feedback;
    }

    private SaveFeedbackRequest.TimestampFeedbackItem createItem(
            SaveFeedbackRequest.NonverbalScore payload) {
        SaveFeedbackRequest.TimestampFeedbackItem item = new SaveFeedbackRequest.TimestampFeedbackItem();
        ReflectionTestUtils.setField(item, "questionId", QUESTION_ID);
        ReflectionTestUtils.setField(item, "startMs", 0L);
        ReflectionTestUtils.setField(item, "endMs", 1000L);
        ReflectionTestUtils.setField(item, "difficulty", "easy");
        ReflectionTestUtils.setField(item, "resumeMode", "PLAYGROUND");
        ReflectionTestUtils.setField(item, "nonverbalScore", payload);
        return item;
    }

    private SaveFeedbackRequest.NonverbalScore createPayload(
            Integer fluency, Integer confidenceTone, Integer eyeContactPosture, Integer composure) {
        SaveFeedbackRequest.NonverbalScore score = new SaveFeedbackRequest.NonverbalScore();
        ReflectionTestUtils.setField(score, "fluency", fluency);
        ReflectionTestUtils.setField(score, "confidenceTone", confidenceTone);
        ReflectionTestUtils.setField(score, "eyeContactPosture", eyeContactPosture);
        ReflectionTestUtils.setField(score, "composure", composure);
        return score;
    }

    @SuppressWarnings("unchecked")
    private ArgumentCaptor<Map<String, DimensionScore>> dimensionsCaptor() {
        return ArgumentCaptor.forClass(Map.class);
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

    private List<String> skipMessages() {
        return logAppender.list.stream()
                .map(ILoggingEvent::getFormattedMessage)
                .filter(m -> m.contains("[정상 skip]") || m.contains("[결함 skip]"))
                .toList();
    }
}
