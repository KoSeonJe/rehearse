package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.OffTopicMarkers;
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
@DisplayName("OffTopicResponseHandler - OFF_TOPIC 정책 + dispatch")
class OffTopicResponseHandlerTest {

    @InjectMocks
    private OffTopicResponseHandler handler;

    @Mock
    private OffTopicEscalationDetector escalationDetector;

    @Mock
    private GiveUpResponseHandler giveUpResponseHandler;

    @Mock
    private OffTopicResponseGenerator offTopicResponseGenerator;

    @Mock
    private IntentClassifierProperties properties;

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, null, 2);

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "시간이 얼마나 남았어요?";

    private static final FollowUpResponse REDIRECT_RESPONSE = FollowUpResponse.builder()
            .question("범위 밖 redirect").ttsQuestion("범위 밖 redirect")
            .type(OffTopicMarkers.FOLLOW_UP_TYPE).skip(true).skipReason(OffTopicMarkers.SKIP_REASON)
            .presentToUser(true).answerText(ANSWER_TEXT).build();

    @BeforeEach
    void setUp() {
        lenient().when(properties.offTopicConsecutiveLimit()).thenReturn(3);
        lenient().when(escalationDetector.countRecentConsecutive(any())).thenReturn(0);
        lenient().when(escalationDetector.shouldEscalate(anyInt(), anyInt())).thenReturn(false);
        lenient().when(offTopicResponseGenerator.generate(any())).thenReturn(REDIRECT_RESPONSE);
    }

    private IntentBranchInput buildInput(Long interviewId, int turnIndex) {
        return new IntentBranchInput(interviewId, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, turnIndex, List.of());
    }

    @Nested
    @DisplayName("Escalation 정책")
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
            then(offTopicResponseGenerator).should(never()).generate(any());
        }

        @Test
        @DisplayName("shouldEscalate=false이면 OffTopicResponseGenerator에 위임한다")
        void handle_shouldNotEscalate_delegatesToGenerator() {
            FollowUpResponse response = handler.handle(buildInput(1L, 5));

            assertThat(response.getType()).isEqualTo(OffTopicMarkers.FOLLOW_UP_TYPE);
            then(offTopicResponseGenerator).should().generate(any(IntentBranchInput.class));
            then(giveUpResponseHandler).should(never()).handle(any(IntentBranchInput.class));
        }
    }

    @Nested
    @DisplayName("Strategy 등록")
    class StrategyRegistration {

        @Test
        @DisplayName("supports() 는 IntentType.OFF_TOPIC 을 반환한다")
        void supports_returnsOffTopic() {
            assertThat(handler.supports()).isEqualTo(com.rehearse.api.domain.interview.entity.IntentType.OFF_TOPIC);
        }
    }
}
