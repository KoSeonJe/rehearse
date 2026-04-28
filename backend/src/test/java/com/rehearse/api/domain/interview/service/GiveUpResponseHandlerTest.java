package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.IntentBranchInput;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("GiveUpResponseHandler - Generator 위임")
class GiveUpResponseHandlerTest {

    @InjectMocks
    private GiveUpResponseHandler handler;

    @Mock
    private GiveUpResponseGenerator generator;

    private static final IntentBranchInput INPUT = new IntentBranchInput(
            1L,
            new FollowUpContext(Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, null, 2),
            "질문",
            "답변",
            0,
            List.of()
    );

    @Test
    @DisplayName("supports()는 IntentType.GIVE_UP을 반환한다")
    void supports_returnsGiveUp() {
        assertThat(handler.supports()).isEqualTo(IntentType.GIVE_UP);
    }

    @Test
    @DisplayName("handle()은 Generator에 위임하고 그 결과를 반환한다")
    void handle_delegatesToGenerator() {
        FollowUpResponse expected = FollowUpResponse.builder()
                .type("SCAFFOLD").skip(true).presentToUser(true).build();
        given(generator.generate(INPUT)).willReturn(expected);

        FollowUpResponse result = handler.handle(INPUT);

        assertThat(result).isSameAs(expected);
        then(generator).should().generate(INPUT);
    }
}
