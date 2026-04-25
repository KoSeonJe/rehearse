package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.ClarifyResponsePromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ClarifyResponseHandler - CLARIFY_REQUEST 처리")
class ClarifyResponseHandlerTest {

    @InjectMocks
    private ClarifyResponseHandler handler;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private ClarifyResponsePromptBuilder promptBuilder;

    private static final ChatResponse DUMMY_RESPONSE =
            new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1, null);

    private static final String MAIN_QUESTION = "서비스 디스커버리에 대해 설명해주세요.";
    private static final String ANSWER_TEXT = "그게 무슨 뜻인지 모르겠어요.";

    @BeforeEach
    void setUp() {
        given(promptBuilder.buildSystemPrompt()).willReturn("system-prompt");
        given(promptBuilder.buildUserPrompt(any(), any(), any())).willReturn("user-prompt");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(DUMMY_RESPONSE);
    }

    @Test
    @DisplayName("AI 응답을 FollowUpResponse로 올바르게 매핑한다")
    void handle_mapsAiResponseToFollowUpResponse() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse(
                        "서비스 디스커버리란 마이크로서비스 간 위치를 동적으로 찾는 메커니즘입니다. 힌트: Eureka나 Consul 같은 레지스트리를 생각해보세요.",
                        "서비스 디스커버리란 마이크로서비스 간 위치를 동적으로 찾는 메커니즘입니다. 힌트 유레카나 콘술 같은 레지스트리를 생각해보세요.",
                        "응시자가 용어 자체를 모름"
                ));

        FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

        assertThat(response.getQuestion()).contains("서비스 디스커버리란");
        assertThat(response.getTtsQuestion()).contains("서비스 디스커버리란");
        assertThat(response.getReason()).isEqualTo("응시자가 용어 자체를 모름");
        assertThat(response.getType()).isEqualTo("CLARIFY_REESTABLISH");
    }

    @Test
    @DisplayName("skip=true, skipReason=CLARIFY_REQUEST로 설정된다")
    void handle_skipFieldsAreSet() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("질문 재설명", "질문 재설명", "재설명 이유"));

        FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo("CLARIFY_REQUEST");
    }

    @Test
    @DisplayName("answerText가 전달받은 값 그대로 유지된다")
    void handle_answerTextIsPreserved() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("재설명", "재설명", "이유"));

        FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

        assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
    }

    @Test
    @DisplayName("questionId와 modelAnswer는 null이다")
    void handle_questionIdAndModelAnswerAreNull() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new ClarifyResponseHandler.ClarifyAiResponse("재설명", "재설명", "이유"));

        FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

        assertThat(response.getQuestionId()).isNull();
        assertThat(response.getModelAnswer()).isNull();
    }
}
