package com.rehearse.api.domain.question.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeIngestionService;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister.ResumeQuestionDraft;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions.GeneratedResumeQuestion;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ResumeTrackInitiator — opener N + main M 일괄 생성")
class ResumeTrackInitiatorTest {

    private QuestionGenerationTransactionHandler transactionHandler;
    private ResumeIngestionService ingestionService;
    private ResumeQuestionPersister persister;
    private AiClient aiClient;
    private AiResponseParser parser;
    private InterviewContextBuilder contextBuilder;
    private ObjectMapper objectMapper;

    private ResumeTrackInitiator initiator;

    @BeforeEach
    void setUp() {
        transactionHandler = mock(QuestionGenerationTransactionHandler.class);
        ingestionService = mock(ResumeIngestionService.class);
        persister = mock(ResumeQuestionPersister.class);
        aiClient = mock(AiClient.class);
        parser = mock(AiResponseParser.class);
        contextBuilder = mock(InterviewContextBuilder.class);
        objectMapper = new ObjectMapper();

        initiator = new ResumeTrackInitiator(
                transactionHandler, ingestionService, persister,
                aiClient, parser, contextBuilder, objectMapper);
    }

    @Test
    @DisplayName("durationMinutes=30 이면 opener 1 + main 12 (30/3+2) 드래프트 일괄 적재 + orderIndex 0..N-1")
    void initiate_persistsOpenerAndMains_withSequentialOrder() {
        long interviewId = 99L;
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "resume-1", "hash-1", CandidateLevel.MID, "backend", List.of());
        when(ingestionService.ingestExtractedText(eq(interviewId), any(), eq("hash-1")))
                .thenReturn(skeleton);
        when(contextBuilder.build(any(ContextBuildRequest.class)))
                .thenReturn(new BuiltContext(
                        List.of(ChatMessage.of(ChatMessage.Role.SYSTEM, "system")),
                        0,
                        java.util.Map.of()));
        when(aiClient.chat(any(ChatRequest.class)))
                .thenReturn(mock(ChatResponse.class));
        GeneratedResumeQuestions generated = new GeneratedResumeQuestions(
                List.of(new GeneratedResumeQuestion("자기소개 부탁드립니다", "TTS-O1", "best-O1")),
                List.of(
                        new GeneratedResumeQuestion("프로젝트 A 의 핵심 기술 결정", "TTS-M1", "best-M1"),
                        new GeneratedResumeQuestion("프로젝트 B 의 기술 결정", "TTS-M2", "best-M2"),
                        new GeneratedResumeQuestion("프로젝트 C 의 기술 결정", "TTS-M3", "best-M3")
                ));
        when(parser.parseOrRetry(any(), eq(GeneratedResumeQuestions.class), any(), any()))
                .thenReturn(generated);

        initiator.initiate(interviewId, "hash-1", "이력서 본문", 30);

        @SuppressWarnings("unchecked")
        ArgumentCaptor<List<ResumeQuestionDraft>> captor = ArgumentCaptor.forClass(List.class);
        verify(persister).persistAll(eq(interviewId), captor.capture());
        List<ResumeQuestionDraft> drafts = captor.getValue();

        assertThat(drafts).hasSize(4);
        assertThat(drafts.get(0).questionType()).isEqualTo(QuestionType.RESUME_OPENER);
        assertThat(drafts.get(0).orderIndex()).isZero();
        assertThat(drafts.subList(1, 4))
                .allSatisfy(d -> assertThat(d.questionType()).isEqualTo(QuestionType.RESUME_MAIN));
        assertThat(drafts.get(1).orderIndex()).isEqualTo(1);
        assertThat(drafts.get(2).orderIndex()).isEqualTo(2);
        assertThat(drafts.get(3).orderIndex()).isEqualTo(3);

        verify(transactionHandler).completeGeneration(interviewId);
    }

    @Test
    @DisplayName("LLM 실패 시 transactionHandler.failGeneration 호출 후 예외 재전파")
    void initiate_propagatesExceptionAndFailsGeneration() {
        long interviewId = 100L;
        when(ingestionService.ingestExtractedText(anyLong(), any(), any()))
                .thenThrow(new RuntimeException("ingest 실패"));

        try {
            initiator.initiate(interviewId, "hash", "본문", 30);
        } catch (RuntimeException expected) {
            assertThat(expected).hasMessageContaining("ingest 실패");
        }

        verify(transactionHandler).failGeneration(eq(interviewId), any());
    }
}
