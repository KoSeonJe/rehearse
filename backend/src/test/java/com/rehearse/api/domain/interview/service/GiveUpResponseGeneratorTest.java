package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
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
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
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
@DisplayName("GiveUpResponseGenerator - GIVE_UP LLM 응답 생성")
class GiveUpResponseGeneratorTest {

    @Mock private AiClient aiClient;
    @Mock private AiResponseParser aiResponseParser;
    @Mock private InterviewContextBuilder contextBuilder;
    @Mock private InterviewRuntimeStateCache runtimeStateCache;

    private GiveUpResponseGenerator generator;

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

    private static final String MAIN_QUESTION = "B-Tree와 B+Tree의 차이점을 설명해주세요.";
    private static final String ANSWER_TEXT = "모르겠어요, 패스할게요.";

    private static final IntentBranchInput INPUT = new IntentBranchInput(
            1L, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, 0, List.of());

    @BeforeEach
    void setUp() {
        generator = new GiveUpResponseGenerator(aiClient, aiResponseParser, contextBuilder, runtimeStateCache);
        lenient().when(contextBuilder.build(any(ContextBuildRequest.class))).thenReturn(STUB_CONTEXT);
        lenient().when(aiClient.chat(any(ChatRequest.class))).thenReturn(DUMMY_RESPONSE);
        lenient().when(runtimeStateCache.get(any())).thenThrow(new IllegalStateException("not found"));
    }

    @Nested
    @DisplayName("SCAFFOLD 모드")
    class ScaffoldMode {

        @Test
        @DisplayName("LLM이 SCAFFOLD를 선택하면 type=SCAFFOLD인 FollowUpResponse를 반환한다")
        void generate_scaffold_returnsScaffoldType() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseGenerator.GiveUpAiResponse(
                            "힌트를 드릴게요 — B-Tree는 모든 노드에 데이터를 저장하지만 B+Tree는 리프에만 저장합니다.",
                            "힌트를 드릴게요 비트리는 모든 노드에 데이터를 저장하지만 비플러스트리는 리프에만 저장합니다.",
                            "단순 힌트로 답변 가능해 보임",
                            "SCAFFOLD"
                    ));

            FollowUpResponse response = generator.generate(INPUT);

            assertThat(response.getType()).isEqualTo("SCAFFOLD");
            assertThat(response.getQuestion()).contains("힌트를 드릴게요");
        }
    }

    @Nested
    @DisplayName("REVEAL_AND_MOVE_ON 모드")
    class RevealAndMoveOnMode {

        @Test
        @DisplayName("LLM이 REVEAL_AND_MOVE_ON을 선택하면 type=REVEAL_AND_MOVE_ON인 FollowUpResponse를 반환한다")
        void generate_revealAndMoveOn_returnsRevealType() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseGenerator.GiveUpAiResponse(
                            "B+Tree는 모든 데이터를 리프 노드에 저장하고 리프끼리 연결되어 범위 검색에 유리합니다.",
                            "비플러스트리는 모든 데이터를 리프 노드에 저장하고 리프끼리 연결되어 범위 검색에 유리합니다.",
                            "개념 자체를 모르는 상태",
                            "REVEAL_AND_MOVE_ON"
                    ));

            FollowUpResponse response = generator.generate(INPUT);

            assertThat(response.getType()).isEqualTo("REVEAL_AND_MOVE_ON");
            assertThat(response.getQuestion()).contains("B+Tree");
        }
    }

    @Nested
    @DisplayName("공통 응답 필드 검증")
    class CommonFields {

        @Test
        @DisplayName("skip=true, skipReason=GIVE_UP, presentToUser=true")
        void generate_skipFieldsAreSet() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseGenerator.GiveUpAiResponse("응답", "응답", "이유", "SCAFFOLD"));

            FollowUpResponse response = generator.generate(INPUT);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            assertThat(response.isPresentToUser()).isTrue();
        }

        @Test
        @DisplayName("answerText가 전달받은 값 그대로 유지된다")
        void generate_answerTextIsPreserved() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseGenerator.GiveUpAiResponse("응답", "응답", "이유", "SCAFFOLD"));

            FollowUpResponse response = generator.generate(INPUT);

            assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
        }

        @Test
        @DisplayName("LLM 호출이 RuntimeException으로 실패하면 fallback 응답을 반환한다")
        void generate_llmFailure_returnsFallback() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 실패"));

            FollowUpResponse response = generator.generate(INPUT);

            assertThat(response.getType()).isEqualTo("GIVE_UP_FALLBACK");
            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            assertThat(response.getQuestion()).contains(MAIN_QUESTION);
        }
    }

    @Test
    @DisplayName("contextBuilder가 giveup_response callType과 올바른 focusHints로 호출된다")
    void generate_contextBuilder_invokedWithGiveUpCallType() {
        given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                .willReturn(new GiveUpResponseGenerator.GiveUpAiResponse("응답", "응답", "이유", "SCAFFOLD"));
        ArgumentCaptor<ContextBuildRequest> captor = ArgumentCaptor.forClass(ContextBuildRequest.class);
        given(contextBuilder.build(captor.capture())).willReturn(STUB_CONTEXT);

        generator.generate(INPUT);

        assertThat(captor.getValue().callType()).isEqualTo("giveup_response");
        assertThat(captor.getValue().focusHints()).containsKey("mainQuestion");
        assertThat(captor.getValue().focusHints()).containsKey("userUtterance");
        assertThat(captor.getValue().focusHints()).containsKey("personaDepthHint");
    }
}
