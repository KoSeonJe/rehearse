package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.vo.IntentType;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("IntentDispatcher — handler 라우팅 + 미등록 throw")
class IntentDispatcherTest {

    private static class StubHandler implements IntentResponseHandler {
        private final IntentType intent;
        private final FollowUpResponse response;

        StubHandler(IntentType intent, FollowUpResponse response) {
            this.intent = intent;
            this.response = response;
        }

        @Override
        public IntentType supports() {
            return intent;
        }

        @Override
        public FollowUpResponse handle(IntentBranchInput input) {
            return response;
        }
    }

    @Test
    @DisplayName("등록된 intent 는 해당 handler 로 라우팅")
    void dispatch_routesToRegisteredHandler() {
        FollowUpResponse offTopic = FollowUpResponse.builder().question("OFF").skip(true).build();
        FollowUpResponse clarify = FollowUpResponse.builder().question("CLAR").skip(true).build();

        IntentDispatcher dispatcher = new IntentDispatcher(List.of(
                new StubHandler(IntentType.OFF_TOPIC, offTopic),
                new StubHandler(IntentType.CLARIFY_REQUEST, clarify)));
        ReflectionTestUtils.invokeMethod(dispatcher, "register");

        IntentBranchInput input = new IntentBranchInput(1L, null, "q", "a", 0, List.of());
        assertThat(dispatcher.dispatch(IntentType.OFF_TOPIC, input)).isSameAs(offTopic);
        assertThat(dispatcher.dispatch(IntentType.CLARIFY_REQUEST, input)).isSameAs(clarify);
    }

    @Test
    @DisplayName("미등록 intent → IllegalStateException")
    void dispatch_unregisteredIntent_throws() {
        IntentDispatcher dispatcher = new IntentDispatcher(List.of(
                new StubHandler(IntentType.OFF_TOPIC, FollowUpResponse.builder().build())));
        ReflectionTestUtils.invokeMethod(dispatcher, "register");

        IntentBranchInput input = new IntentBranchInput(1L, null, "q", "a", 0, List.of());
        assertThatThrownBy(() -> dispatcher.dispatch(IntentType.GIVE_UP, input))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("GIVE_UP");
    }
}
