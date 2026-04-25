package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.global.config.IntentClassifierProperties;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("OffTopicResponseHandler - OFF_TOPIC 응답 생성")
class OffTopicResponseHandlerTest {

    @InjectMocks
    private OffTopicResponseHandler handler;

    @Mock
    private OffTopicEscalationDetector escalationDetector;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    @Mock
    private IntentClassifierProperties properties;

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1, null, 2);

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "시간이 얼마나 남았어요?";

    @BeforeEach
    void setUp() {
        lenient().when(properties.offTopicConsecutiveLimit()).thenReturn(3);
        lenient().when(escalationDetector.countRecentConsecutive(any())).thenReturn(0);
        lenient().when(escalationDetector.shouldEscalate(anyInt(), anyInt())).thenReturn(false);
    }

    private IntentBranchInput buildInput(Long interviewId, int turnIndex) {
        return new IntentBranchInput(interviewId, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, turnIndex, List.of());
    }

    @Nested
    @DisplayName("리드인 풀 결정성 (interviewId XOR turnIndex*31 hash)")
    class LeadInPool {

        @Test
        @DisplayName("동일한 interviewId+turnIndex는 항상 동일한 리드인을 반환한다")
        void handle_sameInputProducesSameLeadIn() {
            FollowUpResponse r1 = handler.handle(buildInput(42L, 3));
            FollowUpResponse r2 = handler.handle(buildInput(42L, 3));

            assertThat(r1.getQuestion()).isEqualTo(r2.getQuestion());
        }

        @Test
        @DisplayName("리드인 풀 4개 중 하나로 응답이 시작한다")
        void handle_responseStartsWithOneOfLeadInPool() {
            List<String> validLeadIns = List.of(
                    "방금 답변은 질문 주제에서 벗어난 것 같습니다.",
                    "응답이 질문 범위 밖으로 보입니다.",
                    "지금 내용은 현재 질문과 직접 관련이 없습니다.",
                    "질문과 다소 다른 방향의 답변으로 판단됩니다."
            );

            FollowUpResponse response = handler.handle(buildInput(7L, 2));

            assertThat(validLeadIns.stream().anyMatch(li -> response.getQuestion().startsWith(li))).isTrue();
        }
    }

    @Nested
    @DisplayName("응답 필드 검증")
    class ResponseFields {

        @Test
        @DisplayName("connector 문자열이 리드인과 mainQuestion 사이에 고정 포함된다")
        void handle_connectorIsFixed() {
            FollowUpResponse response = handler.handle(buildInput(1L, 0));

            assertThat(response.getQuestion()).contains(OffTopicMarker.CONNECTOR);
        }

        @Test
        @DisplayName("question과 ttsQuestion이 동일하다")
        void handle_questionAndTtsQuestionAreEqual() {
            FollowUpResponse response = handler.handle(buildInput(1L, 0));

            assertThat(response.getQuestion()).isEqualTo(response.getTtsQuestion());
        }

        @Test
        @DisplayName("type은 OFF_TOPIC_REDIRECT이다")
        void handle_typeIsOffTopicRedirect() {
            FollowUpResponse response = handler.handle(buildInput(1L, 0));

            assertThat(response.getType()).isEqualTo(OffTopicMarker.FOLLOW_UP_TYPE);
        }

        @Test
        @DisplayName("skip=true, skipReason=OFF_TOPIC, presentToUser=true")
        void handle_skipFieldsAreSet() {
            FollowUpResponse response = handler.handle(buildInput(1L, 0));

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
            assertThat(response.isPresentToUser()).isTrue();
        }

        @Test
        @DisplayName("answerText가 전달받은 값 그대로 유지된다")
        void handle_answerTextIsPreserved() {
            FollowUpResponse response = handler.handle(buildInput(1L, 0));

            assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
        }

        @Test
        @DisplayName("mainQuestion에 큰따옴표/개행이 있어도 정상 처리된다")
        void handle_mainQuestionWithSpecialChars() {
            String questionWithQuotes = "\"HashMap\"과 \"TreeMap\"의 차이점을 설명해주세요.\n자세히 설명하세요.";
            IntentBranchInput input = new IntentBranchInput(1L, CONTEXT, questionWithQuotes, ANSWER_TEXT, 0, List.of());

            FollowUpResponse response = handler.handle(input);

            assertThat(response.getQuestion()).endsWith(questionWithQuotes);
        }
    }

    @Nested
    @DisplayName("Escalation 흡수 — handler 내부에서 GIVE_UP 위임 결정")
    class Escalation {

        @Test
        @DisplayName("shouldEscalate=true이면 giveUpResponseHandler에 위임한다")
        void handle_shouldEscalate_delegatesToGiveUp() {
            given(escalationDetector.countRecentConsecutive(any())).willReturn(2);
            given(escalationDetector.shouldEscalate(2, 3)).willReturn(true);
            FollowUpResponse giveUpResponse = FollowUpResponse.builder()
                    .question("힌트 제공").skip(true).skipReason("GIVE_UP").presentToUser(true).build();
            given(giveUpResponseHandler.handle(any(IntentBranchInput.class))).willReturn(giveUpResponse);

            FollowUpResponse response = handler.handle(buildInput(1L, 5));

            assertThat(response.getSkipReason()).isEqualTo("GIVE_UP");
            then(giveUpResponseHandler).should().handle(any(IntentBranchInput.class));
        }

        @Test
        @DisplayName("shouldEscalate=false이면 giveUp 위임 없이 redirect 응답을 만든다")
        void handle_shouldNotEscalate_buildsRedirect() {
            FollowUpResponse response = handler.handle(buildInput(1L, 5));

            assertThat(response.getType()).isEqualTo(OffTopicMarker.FOLLOW_UP_TYPE);
            then(giveUpResponseHandler).should(never()).handle(any(IntentBranchInput.class));
        }
    }

    @Nested
    @DisplayName("Strategy 등록")
    class StrategyRegistration {

        @Test
        @DisplayName("supports() 는 IntentType.OFF_TOPIC 을 반환한다")
        void supports_returnsOffTopic() {
            assertThat(handler.supports()).isEqualTo(com.rehearse.api.domain.interview.vo.IntentType.OFF_TOPIC);
        }
    }
}
