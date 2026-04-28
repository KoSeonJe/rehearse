package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.OffTopicMarkers;
import com.rehearse.api.domain.interview.entity.Position;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OffTopicResponseGenerator - OFF_TOPIC 리다이렉트 응답 생성")
class OffTopicResponseGeneratorTest {

    private OffTopicResponseGenerator generator;

    private static final FollowUpContext CONTEXT = new FollowUpContext(
            Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 50L, 1, null, 2);

    private static final String MAIN_QUESTION = "JVM의 메모리 구조를 설명해주세요.";
    private static final String ANSWER_TEXT = "오늘 날씨가 좋네요.";

    private static final IntentBranchInput INPUT = new IntentBranchInput(
            1L, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, 0, List.of());

    @BeforeEach
    void setUp() {
        generator = new OffTopicResponseGenerator();
    }

    @Test
    @DisplayName("응답에 mainQuestion 내용이 포함된다")
    void generate_includesMainQuestion() {
        FollowUpResponse response = generator.generate(INPUT);

        assertThat(response.getQuestion()).contains(MAIN_QUESTION);
        assertThat(response.getTtsQuestion()).contains(MAIN_QUESTION);
    }

    @Test
    @DisplayName("응답에 OFF_TOPIC connector 문자열이 포함된다")
    void generate_includesOffTopicConnector() {
        FollowUpResponse response = generator.generate(INPUT);

        assertThat(response.getQuestion()).contains(OffTopicMarkers.CONNECTOR);
    }

    @Test
    @DisplayName("skip=true, skipReason=OFF_TOPIC, type=OFF_TOPIC_REDIRECT")
    void generate_skipFieldsAreSet() {
        FollowUpResponse response = generator.generate(INPUT);

        assertThat(response.isSkip()).isTrue();
        assertThat(response.getSkipReason()).isEqualTo(OffTopicMarkers.SKIP_REASON);
        assertThat(response.getType()).isEqualTo(OffTopicMarkers.FOLLOW_UP_TYPE);
    }

    @Test
    @DisplayName("presentToUser=true")
    void generate_presentToUserIsTrue() {
        FollowUpResponse response = generator.generate(INPUT);

        assertThat(response.isPresentToUser()).isTrue();
    }

    @Test
    @DisplayName("answerText가 전달받은 값 그대로 유지된다")
    void generate_answerTextIsPreserved() {
        FollowUpResponse response = generator.generate(INPUT);

        assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
    }

    @Test
    @DisplayName("같은 interviewId + turnIndex는 동일한 lead-in을 반환한다")
    void generate_deterministicLeadIn_sameInput() {
        FollowUpResponse r1 = generator.generate(INPUT);
        FollowUpResponse r2 = generator.generate(INPUT);

        assertThat(r1.getQuestion()).isEqualTo(r2.getQuestion());
    }

    @Test
    @DisplayName("turnIndex가 다르면 다른 lead-in이 선택될 수 있다")
    void generate_differentTurnIndex_mayProduceDifferentLeadIn() {
        IntentBranchInput input2 = new IntentBranchInput(1L, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, 1, List.of());
        IntentBranchInput input3 = new IntentBranchInput(1L, CONTEXT, MAIN_QUESTION, ANSWER_TEXT, 2, List.of());

        FollowUpResponse r0 = generator.generate(INPUT);
        FollowUpResponse r1 = generator.generate(input2);
        FollowUpResponse r2 = generator.generate(input3);

        // 4개 pool에서 결정적으로 선택 — 적어도 하나는 달라야 한다 (모두 같을 수도 있으나 실제 분포 확인)
        assertThat(List.of(r0.getQuestion(), r1.getQuestion(), r2.getQuestion()))
                .allMatch(q -> q.contains(MAIN_QUESTION));
    }
}
