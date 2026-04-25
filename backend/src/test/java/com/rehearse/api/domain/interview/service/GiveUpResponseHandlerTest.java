package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.prompt.GiveUpResponsePromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("GiveUpResponseHandler - GIVE_UP 처리")
class GiveUpResponseHandlerTest {

    @InjectMocks
    private GiveUpResponseHandler handler;

    @Mock
    private AiClient aiClient;

    @Mock
    private AiResponseParser aiResponseParser;

    @Mock
    private GiveUpResponsePromptBuilder promptBuilder;

    private static final ChatResponse DUMMY_RESPONSE =
            new ChatResponse("{}", ChatResponse.Usage.empty(), "openai", "gpt-4o-mini", false, false);

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1, null);

    private static final String MAIN_QUESTION = "B-Tree와 B+Tree의 차이점을 설명해주세요.";
    private static final String ANSWER_TEXT = "모르겠어요, 패스할게요.";

    @BeforeEach
    void setUp() {
        given(promptBuilder.buildSystemPrompt()).willReturn("system-prompt");
        given(promptBuilder.buildUserPrompt(any(), any(), any())).willReturn("user-prompt");
        given(aiClient.chat(any(ChatRequest.class))).willReturn(DUMMY_RESPONSE);
    }

    @Nested
    @DisplayName("SCAFFOLD 모드")
    class ScaffoldMode {

        @Test
        @DisplayName("LLM이 SCAFFOLD를 선택하면 type=SCAFFOLD인 FollowUpResponse를 반환한다")
        void handle_scaffold_returnsScaffoldType() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse(
                            "힌트를 드릴게요 — B-Tree는 모든 노드에 데이터를 저장하지만 B+Tree는 리프에만 저장합니다. 다시 시도해보시겠어요?",
                            "힌트를 드릴게요 비트리는 모든 노드에 데이터를 저장하지만 비플러스트리는 리프에만 저장합니다. 다시 시도해보시겠어요?",
                            "단순 힌트로 답변 가능해 보임",
                            "SCAFFOLD"
                    ));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getType()).isEqualTo("SCAFFOLD");
            assertThat(response.getQuestion()).contains("힌트를 드릴게요");
        }
    }

    @Nested
    @DisplayName("REVEAL_AND_MOVE_ON 모드")
    class RevealAndMoveOnMode {

        @Test
        @DisplayName("LLM이 REVEAL_AND_MOVE_ON을 선택하면 type=REVEAL_AND_MOVE_ON인 FollowUpResponse를 반환한다")
        void handle_revealAndMoveOn_returnsRevealType() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse(
                            "B+Tree는 모든 데이터를 리프 노드에 저장하고 리프끼리 연결되어 범위 검색에 유리합니다. 다음 주제로 넘어가겠습니다.",
                            "비플러스트리는 모든 데이터를 리프 노드에 저장하고 리프끼리 연결되어 범위 검색에 유리합니다. 다음 주제로 넘어가겠습니다.",
                            "개념 자체를 모르는 상태",
                            "REVEAL_AND_MOVE_ON"
                    ));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getType()).isEqualTo("REVEAL_AND_MOVE_ON");
            assertThat(response.getQuestion()).contains("다음 주제로 넘어가겠습니다");
        }
    }

    @Nested
    @DisplayName("공통 응답 필드 검증")
    class CommonFields {

        @Test
        @DisplayName("skip=true, skipReason=GIVE_UP으로 설정된다")
        void handle_skipFieldsAreSet() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse("응답", "응답", "이유", "SCAFFOLD"));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
        }

        @Test
        @DisplayName("answerText가 전달받은 값 그대로 유지된다")
        void handle_answerTextIsPreserved() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse("응답", "응답", "이유", "SCAFFOLD"));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
        }

        @Test
        @DisplayName("questionId와 modelAnswer는 null이다")
        void handle_questionIdAndModelAnswerAreNull() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse("응답", "응답", "이유", "REVEAL_AND_MOVE_ON"));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestionId()).isNull();
            assertThat(response.getModelAnswer()).isNull();
        }

        @Test
        @DisplayName("reason 필드가 LLM 응답 그대로 매핑된다")
        void handle_reasonIsMappedFromAiResponse() {
            given(aiResponseParser.parseOrRetry(any(), any(), any(), any()))
                    .willReturn(new GiveUpResponseHandler.GiveUpAiResponse("응답", "응답", "개념 자체를 모르는 상태", "REVEAL_AND_MOVE_ON"));

            FollowUpResponse response = handler.handle(CONTEXT, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getReason()).isEqualTo("개념 자체를 모르는 상태");
        }
    }
}
