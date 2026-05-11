package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.service.InterviewPlanRuntimeCache;
import com.rehearse.api.domain.resume.service.ResumeSkeletonRuntimeCache;
import com.rehearse.api.domain.resume.service.InterviewPlanPersister;
import com.rehearse.api.domain.resume.service.ResumeInterviewPlanner;
import com.rehearse.api.domain.resume.service.ResumeInterviewService;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService — RubricEvent publish")
class FollowUpServiceRubricEventTest {

    private FollowUpService followUpService;

    @Mock private AudioTurnAnalyzer audioTurnAnalyzer;
    @Mock private FollowUpQuestionWriter followUpQuestionWriter;
    @Mock private FollowUpTransactionHandler followUpTransactionHandler;
    @Mock private InterviewRuntimeStateCache runtimeStateStore;
    @Mock private AiCallMetrics aiCallMetrics;
    @Mock private ResumeInterviewService resumeInterviewService;
    @Mock private ResumeSkeletonPersister resumeSkeletonStore;
    @Mock private InterviewPlanPersister interviewPlanStore;
    @Mock private ResumeSkeletonRuntimeCache resumeSkeletonCache;
    @Mock private InterviewPlanRuntimeCache interviewPlanCache;
    @Mock private ResumeInterviewPlanner resumeInterviewPlanner;
    @Mock private InterviewFinder interviewFinder;

    @BeforeEach
    void setUp() {
        followUpService = new FollowUpService(
                audioTurnAnalyzer, followUpQuestionWriter,
                followUpTransactionHandler, runtimeStateStore, aiCallMetrics,
                resumeInterviewService, resumeSkeletonStore, interviewPlanStore,
                resumeSkeletonCache, interviewPlanCache, resumeInterviewPlanner, interviewFinder);

        InterviewRuntimeState state = new InterviewRuntimeState("MID", null);
        lenient().when(runtimeStateStore.getOrInit(any(), any())).thenReturn(state);
        lenient().when(runtimeStateStore.get(any())).thenReturn(state);

        Interview csInterview = mock(Interview.class);
        lenient().when(csInterview.getInterviewTypes()).thenReturn(Set.of(InterviewType.CS_FUNDAMENTAL));
        lenient().when(csInterview.getUserId()).thenReturn(1L);
        lenient().when(csInterview.getLevel()).thenReturn(InterviewLevel.MID);
        lenient().when(interviewFinder.findById(any())).thenReturn(csInterview);
    }

    @Test
    @DisplayName("Standard Track 정상 턴 완료 시 TurnCompletedEvent 1회 publish")
    void generateFollowUp_answer_publishesOneEvent() {
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.MID, 20L, 50L, 1,
                com.rehearse.api.domain.question.entity.ReferenceType.MODEL_ANSWER, 3);
        given(followUpTransactionHandler.loadFollowUpContext(any(), any(), any())).willReturn(context);

        AnswerAnalysis analysis = new AnswerAnalysis(
                0L, List.of(), List.of(), List.of(), 3, RecommendedNextAction.DEEP_DIVE);
        TurnAnalysisResult turn = new TurnAnalysisResult("답변 텍스트", analysis);
        given(audioTurnAnalyzer.analyze(any(), any(), any(), any(), any(), any())).willReturn(turn);

        GeneratedFollowUp followUp = buildFollowUp("다음 질문");
        given(followUpQuestionWriter.write(any(), any(), any())).willReturn(followUp);

        Question savedQuestion = Question.builder()
                .questionType(QuestionType.TECH_FOLLOWUP)
                .questionText("다음 질문")
                .orderIndex(1)
                .build();
        given(followUpTransactionHandler.saveFollowUpResultAndPublishEvent(any(), any(), any(), any()))
                .willReturn(new FollowUpSaveResult(savedQuestion, 1));

        FollowUpRequest request = buildRequest();
        followUpService.generateFollowUp(1L, 1L, request, buildAudioFile());

        then(followUpTransactionHandler).should()
                .saveFollowUpResultAndPublishEvent(any(), any(), any(), any());
    }

    private GeneratedFollowUp buildFollowUp(String question) {
        return new GeneratedFollowUp(
                false, null, question, question, "테스트", "CONCEPTUAL",
                null, null, null, null);
    }

    private FollowUpRequest buildRequest() {
        FollowUpRequest req = new FollowUpRequest();
        ReflectionTestUtils.setField(req, "questionSetId", 20L);
        ReflectionTestUtils.setField(req, "questionContent", "TCP 3-way handshake를 설명하세요");
        ReflectionTestUtils.setField(req, "answerText", "답변 텍스트");
        ReflectionTestUtils.setField(req, "previousExchanges", List.of());
        return req;
    }

    private org.springframework.mock.web.MockMultipartFile buildAudioFile() {
        return new org.springframework.mock.web.MockMultipartFile(
                "audio", "test.webm", "audio/webm", new byte[]{1, 2, 3});
    }
}
