package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.CachePolicy;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.Mockito.mock;

/**
 * AiClient.chat() 계약 테스트 — MockAiClient 단위 테스트 (외부 의존 없음).
 *
 * <p>검증 항목:
 * <ul>
 *   <li>role 매핑 — SYSTEM/USER 메시지 구성 후 응답 정상 반환</li>
 *   <li>callType 태그 — ChatResponse content 에 callType 반영</li>
 *   <li>modelOverride — 요청에 modelOverride 포함 시 null 이 아닌 값 그대로 전달</li>
 *   <li>responseFormat JSON_OBJECT — 요청 빌드 시 예외 없음</li>
 *   <li>messages 비어있으면 IllegalArgumentException</li>
 *   <li>withCachePolicy 불변성 — 원본 ChatRequest 불변</li>
 * </ul>
 */
class AiClientChatTest {

    private MockAiClient mockAiClient;

    private List<ChatMessage> basicMessages;

    @BeforeEach
    void setUp() {
        mockAiClient = new MockAiClient(
                mock(QuestionGenerationAdapter.class),
                mock(FollowUpGenerationAdapter.class));
        basicMessages = List.of(
                ChatMessage.of(ChatMessage.Role.SYSTEM, "당신은 면접관입니다."),
                ChatMessage.of(ChatMessage.Role.USER, "Java GC 에 대해 설명해주세요.")
        );
    }

    @Test
    @DisplayName("기본 chat 호출 — content 비어있지 않음")
    void chat_returnsNonEmptyContent() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .callType("test_basic")
                .build();

        ChatResponse response = mockAiClient.chat(request);

        assertThat(response).isNotNull();
        assertThat(response.content()).isNotBlank();
    }

    @Test
    @DisplayName("callType 태그 — 응답 content 에 callType 반영")
    void chat_callTypeReflectedInMockContent() {
        String callType = "intent_classifier";
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .callType(callType)
                .build();

        ChatResponse response = mockAiClient.chat(request);

        // MockAiClient 는 "[Mock] {callType} response" 반환
        assertThat(response.content()).contains(callType);
    }

    @Test
    @DisplayName("modelOverride 설정 — 요청 빌드 시 예외 없음, 값 보존")
    void chat_withModelOverride_buildsWithoutException() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .modelOverride("gpt-4o")
                .callType("resume_extractor")
                .build();

        assertThat(request.modelOverride()).isEqualTo("gpt-4o");

        ChatResponse response = mockAiClient.chat(request);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("responseFormat JSON_OBJECT — 요청 빌드 및 호출 정상")
    void chat_withJsonObjectFormat_succeeds() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType("answer_analyzer")
                .build();

        assertThat(request.responseFormat()).isEqualTo(ResponseFormat.JSON_OBJECT);

        ChatResponse response = mockAiClient.chat(request);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("cacheControl=true 메시지 포함 요청 — 정상 처리")
    void chat_withCachedSystemMessage_succeeds() {
        List<ChatMessage> messages = List.of(
                ChatMessage.ofCached(ChatMessage.Role.SYSTEM, "캐시 대상 시스템 프롬프트"),
                ChatMessage.of(ChatMessage.Role.USER, "질문입니다.")
        );

        ChatRequest request = ChatRequest.builder()
                .messages(messages)
                .cachePolicy(CachePolicy.explicit())
                .callType("follow_up_generator")
                .build();

        ChatResponse response = mockAiClient.chat(request);
        assertThat(response).isNotNull();
    }

    @Test
    @DisplayName("messages 가 null 이면 IllegalArgumentException")
    void chat_nullMessages_throwsIllegalArgument() {
        assertThatThrownBy(() -> ChatRequest.builder()
                .messages(null)
                .callType("test")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("messages 가 비어있으면 IllegalArgumentException")
    void chat_emptyMessages_throwsIllegalArgument() {
        assertThatThrownBy(() -> ChatRequest.builder()
                .messages(List.of())
                .callType("test")
                .build())
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    @DisplayName("provider 필드 — MockAiClient 는 'mock' 반환")
    void chat_provider_isMock() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .callType("test_provider")
                .build();

        ChatResponse response = mockAiClient.chat(request);
        assertThat(response.provider()).isEqualTo("mock");
    }

    @Test
    @DisplayName("cachePolicy 기본값 — AUTO, allowMiss=false")
    void chatRequest_defaultCachePolicy() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .callType("test_default_cache")
                .build();

        assertThat(request.cachePolicy().providerCache()).isEqualTo(CachePolicy.ProviderCache.AUTO);
        assertThat(request.cachePolicy().allowMiss()).isFalse();
    }

    @Test
    @DisplayName("withCachePolicy — 새 ChatRequest 반환, 원본 불변")
    void chatRequest_withCachePolicy_returnsNewInstance() {
        ChatRequest original = ChatRequest.builder()
                .messages(basicMessages)
                .callType("test_immutable")
                .build();

        ChatRequest modified = original.withCachePolicy(CachePolicy.defaults().withAllowMiss(true));

        assertThat(modified.cachePolicy().allowMiss()).isTrue();
        assertThat(original.cachePolicy().allowMiss()).isFalse();
        assertThat(modified).isNotSameAs(original);
    }

    @Test
    @DisplayName("ChatMessage.roleLowercase() — SYSTEM → 'system'")
    void chatMessage_roleLowercase_system() {
        ChatMessage msg = ChatMessage.of(ChatMessage.Role.SYSTEM, "내용");
        assertThat(msg.roleLowercase()).isEqualTo("system");
    }

    @Test
    @DisplayName("ChatMessage.roleLowercase() — USER → 'user'")
    void chatMessage_roleLowercase_user() {
        ChatMessage msg = ChatMessage.of(ChatMessage.Role.USER, "내용");
        assertThat(msg.roleLowercase()).isEqualTo("user");
    }

    @Test
    @DisplayName("ChatMessage.roleLowercase() — ASSISTANT → 'assistant'")
    void chatMessage_roleLowercase_assistant() {
        ChatMessage msg = ChatMessage.of(ChatMessage.Role.ASSISTANT, "내용");
        assertThat(msg.roleLowercase()).isEqualTo("assistant");
    }

    @Test
    @DisplayName("callType 기본값 — null 전달 시 'unknown' 으로 정규화")
    void chatRequest_nullCallType_defaultsToUnknown() {
        ChatRequest request = ChatRequest.builder()
                .messages(basicMessages)
                .callType(null)
                .build();

        assertThat(request.callType()).isEqualTo("unknown");
    }
}
