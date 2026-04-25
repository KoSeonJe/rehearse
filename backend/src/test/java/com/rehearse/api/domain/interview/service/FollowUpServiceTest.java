package com.rehearse.api.domain.interview.service;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("FollowUpService - 꼬리질문 생성 (Step A → Step B v3)")
class FollowUpServiceTest {

    private FollowUpService followUpService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FollowUpPromptBuilder followUpPromptBuilder;

    @Mock
    private AnswerAnalyzer answerAnalyzer;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Mock
    private IntentClassifier intentClassifier;

    @Mock
    private OffTopicResponseHandler offTopicResponseHandler;

    @Mock
    private ClarifyResponseHandler clarifyResponseHandler;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    private AiResponseParser aiResponseParser;

    @BeforeEach
    void setUp() {
        aiResponseParser = new AiResponseParser(new ObjectMapper());

        lenient().when(offTopicResponseHandler.supports()).thenReturn(IntentType.OFF_TOPIC);
        lenient().when(clarifyResponseHandler.supports()).thenReturn(IntentType.CLARIFY_REQUEST);
        lenient().when(giveUpResponseHandler.supports()).thenReturn(IntentType.GIVE_UP);

        followUpService = new FollowUpService(
                aiClient, aiResponseParser, followUpPromptBuilder, answerAnalyzer,
                followUpTransactionHandler, intentClassifier,
                List.of(offTopicResponseHandler, clarifyResponseHandler, giveUpResponseHandler));
        ReflectionTestUtils.invokeMethod(followUpService, "registerHandlers");

        lenient().when(intentClassifier.classify(any(), any(), any()))
                .thenReturn(IntentResult.of(IntentType.ANSWER, 1.0, "test default"));
        lenient().when(followUpPromptBuilder.buildSystemPrompt(any())).thenReturn("system");
        lenient().when(followUpPromptBuilder.buildUserPromptWithAnalysis(any(), any(), any())).thenReturn("user");
    }

    private static FollowUpContext context(int nextOrderIndex, int maxFollowUpRounds) {
        return new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, nextOrderIndex,
                com.rehearse.api.domain.question.entity.ReferenceType.MODEL_ANSWER, maxFollowUpRounds);
    }

    private static GeneratedFollowUp sttFollowUp(String answerText) {
        GeneratedFollowUp f = new GeneratedFollowUp();
        ReflectionTestUtils.setField(f, "answerText", answerText);
        ReflectionTestUtils.setField(f, "skip", Boolean.FALSE);
        return f;
    }

    private static AnswerAnalysis analysisOf(RecommendedNextAction action) {
        return new AnswerAnalysis(
                50L,
                List.of(new Claim("핵심 주장", 3, EvidenceStrength.WEAK, "topic")),
                List.of(Perspective.RELIABILITY),
                List.of("가정"),
                3,
                action
        );
    }

    private static ChatResponse jsonChatResponse(String json) {
        return new ChatResponse(json, ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);
    }

    private static MockMultipartFile audio() {
        return new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private static FollowUpRequest request(String questionContent) {
        FollowUpRequest r = new FollowUpRequest();
        ReflectionTestUtils.setField(r, "questionSetId", 10L);
        ReflectionTestUtils.setField(r, "questionContent", questionContent);
        return r;
    }

    @Nested
    @DisplayName("generateFollowUp 메서드")
    class GenerateFollowUp {

        @Test
        @DisplayName("ANSWER 경로 — Step A 분석 + Step B 작문 결과로 응답 OVERRIDE")
        void generateFollowUp_answerPath_overridesWithStepBOutput() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willReturn(sttFollowUp("HashMap 은 해시 기반입니다."));
            given(answerAnalyzer.analyze(eq(1L), eq(50L), any(), any(), any(), any()))
                    .willReturn(analysisOf(RecommendedNextAction.DEEP_DIVE));

            String stepBJson = """
                    {
                      "skip": false,
                      "skipReason": null,
                      "answerText": "ignored — overridden by req.answerText()",
                      "target_claim_idx": 0,
                      "selected_perspective": "RELIABILITY",
                      "question": "Step B 가 만든 꼬리질문",
                      "ttsQuestion": "Step B 가 만든 꼬리질문",
                      "reason": "depth_score 3 claim 의 RELIABILITY 관점 확장",
                      "type": "DEEP_DIVE",
                      "modelAnswer": "모범답변"
                    }
                    """;
            given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonChatResponse(stepBJson));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP).questionText("Step B 가 만든 꼬리질문").orderIndex(1).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 100L);
            given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(1)))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 1));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("HashMap 충돌 해결?"), audio());

            assertThat(response.getQuestionId()).isEqualTo(100L);
            assertThat(response.getQuestion()).isEqualTo("Step B 가 만든 꼬리질문");
            assertThat(response.getType()).isEqualTo("DEEP_DIVE");
            assertThat(response.getAnswerText()).isEqualTo("HashMap 은 해시 기반입니다.");
            assertThat(response.getSelectedPerspective()).isEqualTo("RELIABILITY");
            assertThat(response.isFollowUpExhausted()).isFalse();
            then(answerAnalyzer).should().analyze(eq(1L), eq(50L), any(), any(), any(), any());
            then(aiClient).should().chat(any(ChatRequest.class));
        }

        @Test
        @DisplayName("ANSWER 경로 — 누적 카운트가 maxFollowUpRounds 도달 시 followUpExhausted=true")
        void generateFollowUp_atMaxRounds_returnsExhaustedTrue() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(2, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willReturn(sttFollowUp("답변"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(analysisOf(RecommendedNextAction.DEEP_DIVE));

            String stepBJson = """
                    {"skip": false, "answerText": "x", "question": "Q2", "ttsQuestion": "Q2",
                     "reason": "r", "type": "DEEP_DIVE", "modelAnswer": "m",
                     "target_claim_idx": 0, "selected_perspective": null}
                    """;
            given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonChatResponse(stepBJson));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP).questionText("Q2").orderIndex(2).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 200L);
            given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(2)))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 2));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isFalse();
            assertThat(response.isFollowUpExhausted()).isTrue();
        }

        @Test
        @DisplayName("STT 단계에서 AI 가 skip=true 를 반환하면 Step A/B 호출 없이 skip 응답")
        void generateFollowUp_sttSkipped_doesNotCallStepAB() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));

            GeneratedFollowUp followUp = new GeneratedFollowUp();
            ReflectionTestUtils.setField(followUp, "skip", Boolean.TRUE);
            ReflectionTestUtils.setField(followUp, "skipReason", "답변이 '잘 모르겠다'로 불충분함");
            ReflectionTestUtils.setField(followUp, "answerText", "잘 모르겠습니다.");
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class))).willReturn(followUp);

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("답변이 '잘 모르겠다'로 불충분함");
            assertThat(response.getQuestionId()).isNull();
            assertThat(response.getAnswerText()).isEqualTo("잘 모르겠습니다.");
            then(answerAnalyzer).should(never()).analyze(any(), any(), any(), any(), any(), any());
            then(aiClient).should(never()).chat(any(ChatRequest.class));
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(GeneratedFollowUp.class), anyInt());
        }

        @Test
        @DisplayName("Step A 가 SKIP 권고 시 Step B 호출 없이 analyzer_recommend_skip 으로 응답")
        void generateFollowUp_analyzerRecommendsSkip_skipsStepB() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willReturn(sttFollowUp("충분히 깊은 답변"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(analysisOf(RecommendedNextAction.SKIP));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("analyzer_recommend_skip");
            assertThat(response.getAnswerText()).isEqualTo("충분히 깊은 답변");
            then(aiClient).should(never()).chat(any(ChatRequest.class));
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(GeneratedFollowUp.class), anyInt());
        }

        @Test
        @DisplayName("Step B 응답이 skip=false 인데 question 이 비어 있으면 PARSE_FAILED")
        void generateFollowUp_stepBNonSkipWithBlankQuestion_throwsParseFailed() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willReturn(sttFollowUp("답변"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(analysisOf(RecommendedNextAction.DEEP_DIVE));

            String stepBJson = """
                    {"skip": false, "answerText": "x", "question": "  ", "type": "DEEP_DIVE",
                     "target_claim_idx": 0, "selected_perspective": null}
                    """;
            given(aiClient.chat(any(ChatRequest.class))).willReturn(jsonChatResponse(stepBJson));

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_005"));
            then(followUpTransactionHandler).should(never()).saveFollowUpResult(anyLong(), any(GeneratedFollowUp.class), anyInt());
        }

        @Test
        @DisplayName("Step B JSON 파싱이 1차 실패해도 parseOrRetry 가 재호출로 복구하면 정상 저장")
        void generateFollowUp_stepBParseRetry_recovers() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willReturn(sttFollowUp("답변"));
            given(answerAnalyzer.analyze(any(), any(), any(), any(), any(), any()))
                    .willReturn(analysisOf(RecommendedNextAction.DEEP_DIVE));

            String invalid = "이건 JSON 이 아닙니다";
            String valid = """
                    {"skip": false, "answerText": "x", "question": "복구된 Q", "ttsQuestion": "복구된 Q",
                     "reason": "r", "type": "DEEP_DIVE", "modelAnswer": "m",
                     "target_claim_idx": 0, "selected_perspective": "TRADEOFF"}
                    """;
            given(aiClient.chat(any(ChatRequest.class)))
                    .willReturn(jsonChatResponse(invalid))
                    .willReturn(jsonChatResponse(valid));

            Question savedQuestion = Question.builder()
                    .questionType(QuestionType.FOLLOWUP).questionText("복구된 Q").orderIndex(1).build();
            ReflectionTestUtils.setField(savedQuestion, "id", 300L);
            given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(1)))
                    .willReturn(new FollowUpSaveResult(savedQuestion, 1));

            FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request("질문"), audio());

            assertThat(response.getQuestion()).isEqualTo("복구된 Q");
            assertThat(response.getSelectedPerspective()).isEqualTo("TRADEOFF");
        }

        @Test
        @DisplayName("오디오 파일이 없으면 INTERVIEW_006 예외")
        void generateFollowUp_noAudioFile() {
            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("INTERVIEW_006"));
        }

        @Test
        @DisplayName("STT 호출 중 예외 발생 시 그대로 전파")
        void generateFollowUp_audioApiThrowsException_propagates() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context(1, 2));
            given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                    .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 서비스를 사용할 수 없습니다."));

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode()).isEqualTo("AI_005"));
        }

        @Test
        @DisplayName("진행 중이 아닌 면접에서 후속질문 생성 시 INTERVIEW_003 예외")
        void generateFollowUp_notInProgress() {
            given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L))
                    .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_003", "면접이 진행 중이 아닙니다."));

            assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request("질문"), audio()))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_003");
                    });
        }
    }
}
