package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.lenient;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClarifyResponseHandler - CLARIFY_REQUEST 처리")
class ClarifyResponseHandlerTest {

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private InterviewContextBuilder contextBuilder;

    @Mock
    private InterviewRuntimeStateCache runtimeStateStore;

    private ClarifyResponseHandler handler;

    private static final ChatResponse DUMMY_RESPONSE =
            new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

    private static final BuiltContext STUB_CONTEXT = new BuiltContext(
            List.of(ChatMessage.ofCached(ChatMessage.Role.SYSTEM, "system"),
                    ChatMessage.of(ChatMessage.Role.USER, "user")),
            50,
            Map.of("L1", 40, "L4", 10, "total", 50)
    );

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, null, 2);

    private static final String MAIN_QUESTION = "서비스 디스커버리에 대해 설명해주세요.";
    private static final String ANSWER_TEXT = "그게 무슨 뜻인지 모르겠어요.";

    private static final IntentBranchInput INPUT = new IntentBranchInput(
            1L, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, 0, List.of());

    @BeforeEach
    void setUp() {
        handler = new ClarifyResponseHandler(aiClient, aiResponseParser, contextBuilder, runtimeStateStore);
        lenient().when(contextBuilder.build(any(ContextBuildRequest.class))).thenReturn(STUB_CONTEXT);
        lenient().when(aiClient.chat(any(ChatRequest.class))).thenReturn(DUMMY_RESPONSE);
        lenient().when(runtimeStateStore.get(any())).thenThrow(new IllegalStateException("not found"));
    }

    @Test
    @DisplayName("AI 응답을 FollowUpResponse로 올바르게 매핑한다")
    void handle_mapsAiResponseToFollowUpResponse() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse(
                        "서비스 디스커버리란 마이크로서비스 간 위치를 동적으로 찾는 메커니즘입니다.",
                        "서비스 디스커버리란 마이크로서비스 간 위치를 동적으로 찾는 메커니즘입니다.",
                        "응시자가 용어 자체를 모름"
                ));

        FollowUpResponse response = handler.handle(INPUT);

        assertThat(response.getQuestion()).contains("서비스 디스커버리란");
        assertThat(response.getTtsQuestion()).contains("서비스 디스커버리란");
        assertThat(response.getReason()).isEqualTo("응시자가 용어 자체를 모름");
        assertThat(response.getType()).isEqualTo("CLARIFY_REESTABLISH");
    }

    @Test
    @DisplayName("skip=true, skipReason=CLARIFY_REQUEST, presentToUser=true")
    void handle_skipFieldsAreSet() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("질문 재설명", "질문 재설명", "재설명 이유"));

        FollowUpResponse response = handler.handle(INPUT);

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
        assertThat(response.isPresentToUser()).isTrue();
    }

    @Test
    @DisplayName("answerText가 전달받은 값 그대로 유지된다")
    void handle_answerTextIsPreserved() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("재설명", "재설명", "이유"));

        FollowUpResponse response = handler.handle(INPUT);

        assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
    }

    @Test
    @DisplayName("LLM 호출 중 RuntimeException이 나면 fallback 응답으로 복구한다")
    void handle_llmFailure_returnsFallback() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 실패"));

        FollowUpResponse response = handler.handle(INPUT);

        assertThat(response.getType()).isEqualTo("CLARIFY_FALLBACK");
        assertThat(response.getQuestion()).contains(MAIN_QUESTION);
        assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
    }

    @Test
    @DisplayName("supports() 는 IntentType.CLARIFY_REQUEST 를 반환한다")
    void supports_returnsClarifyRequest() {
        assertThat(handler.supports()).isEqualTo(IntentType.CLARIFY_REQUEST);
    }

    @Test
    @DisplayName("contextBuilder가 clarify_response callType과 올바른 focusHints로 호출된다")
    void handle_contextBuilder_invokedWithClarifyCallType() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("재설명", "재설명", "이유"));
        ArgumentCaptor<ContextBuildRequest> captor = ArgumentCaptor.forClass(ContextBuildRequest.class);
        given(contextBuilder.build(captor.capture())).willReturn(STUB_CONTEXT);

        handler.handle(INPUT);

        assertThat(captor.getValue().callType()).isEqualTo("clarify_response");
        assertThat(captor.getValue().focusHints()).containsKey("mainQuestion");
        assertThat(captor.getValue().focusHints()).containsKey("userUtterance");
    }
}
