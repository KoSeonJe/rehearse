package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("OffTopicResponseHandler - OFF_TOPIC 응답 생성")
class OffTopicResponseHandlerTest {

    private final OffTopicResponseHandler handler = new OffTopicResponseHandler();

    private static final String MAIN_QUESTION = "HashMap의 해시 충돌 해결 방법을 설명해주세요.";
    private static final String ANSWER_TEXT = "시간이 얼마나 남았어요?";

    @Nested
    @DisplayName("리드인 풀 4종 검증")
    class LeadInPool {

        @Test
        @DisplayName("리드인 0번 인덱스 — 방금 답변은 질문 주제에서 벗어난 것 같습니다")
        void handle_leadIn0_containsFirstMessage() {
            // interviewId=1, turnIndex=0 → hash(1,0) % 4 == 0 확인 후 조합
            Long interviewId = findInterviewIdForIdx(0);
            FollowUpResponse response = handler.handle(interviewId, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).startsWith("방금 답변은 질문 주제에서 벗어난 것 같습니다.");
            assertThat(response.getQuestion()).contains(OffTopicResponseHandler.OFF_TOPIC_CONNECTOR);
            assertThat(response.getQuestion()).endsWith(MAIN_QUESTION);
        }

        @Test
        @DisplayName("리드인 1번 인덱스 — 응답이 질문 범위 밖으로 보입니다")
        void handle_leadIn1_containsSecondMessage() {
            Long interviewId = findInterviewIdForIdx(1);
            FollowUpResponse response = handler.handle(interviewId, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).startsWith("응답이 질문 범위 밖으로 보입니다.");
        }

        @Test
        @DisplayName("리드인 2번 인덱스 — 지금 내용은 현재 질문과 직접 관련이 없습니다")
        void handle_leadIn2_containsThirdMessage() {
            Long interviewId = findInterviewIdForIdx(2);
            FollowUpResponse response = handler.handle(interviewId, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).startsWith("지금 내용은 현재 질문과 직접 관련이 없습니다.");
        }

        @Test
        @DisplayName("리드인 3번 인덱스 — 질문과 다소 다른 방향의 답변으로 판단됩니다")
        void handle_leadIn3_containsFourthMessage() {
            Long interviewId = findInterviewIdForIdx(3);
            FollowUpResponse response = handler.handle(interviewId, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).startsWith("질문과 다소 다른 방향의 답변으로 판단됩니다.");
        }

        private Long findInterviewIdForIdx(int targetIdx) {
            for (long id = 1L; id <= 1000L; id++) {
                if (Math.floorMod(java.util.Objects.hash(id, 0), 4) == targetIdx) {
                    return id;
                }
            }
            throw new IllegalStateException("해당 인덱스를 생성하는 interviewId를 찾을 수 없습니다: " + targetIdx);
        }
    }

    @Nested
    @DisplayName("응답 필드 검증")
    class ResponseFields {

        @Test
        @DisplayName("connector 문자열이 리드인과 mainQuestion 사이에 고정적으로 포함된다")
        void handle_connectorIsFixed() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).contains(OffTopicResponseHandler.OFF_TOPIC_CONNECTOR);
        }

        @Test
        @DisplayName("question과 ttsQuestion이 동일하다")
        void handle_questionAndTtsQuestionAreEqual() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestion()).isEqualTo(response.getTtsQuestion());
        }

        @Test
        @DisplayName("type은 OFF_TOPIC_REDIRECT이다")
        void handle_typeIsOffTopicRedirect() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getType()).isEqualTo("OFF_TOPIC_REDIRECT");
        }

        @Test
        @DisplayName("skip=true, skipReason=OFF_TOPIC이다")
        void handle_skipFieldsAreSet() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.isSkip()).isTrue();
            assertThat(response.getSkipReason()).isEqualTo("OFF_TOPIC");
        }

        @Test
        @DisplayName("answerText가 전달받은 값 그대로 유지된다")
        void handle_answerTextIsPreserved() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getAnswerText()).isEqualTo(ANSWER_TEXT);
        }

        @Test
        @DisplayName("questionId와 modelAnswer는 null이다")
        void handle_questionIdAndModelAnswerAreNull() {
            FollowUpResponse response = handler.handle(1L, 0, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(response.getQuestionId()).isNull();
            assertThat(response.getModelAnswer()).isNull();
        }

        @Test
        @DisplayName("mainQuestion에 큰따옴표가 포함되어 있어도 정상 처리된다")
        void handle_mainQuestionWithSpecialChars() {
            String questionWithQuotes = "\"HashMap\"과 \"TreeMap\"의 차이점을 설명해주세요.\n자세히 설명하세요.";
            FollowUpResponse response = handler.handle(1L, 0, questionWithQuotes, ANSWER_TEXT);

            assertThat(response.getQuestion()).endsWith(questionWithQuotes);
        }

        @Test
        @DisplayName("동일한 interviewId+turnIndex는 항상 동일한 리드인을 반환한다 (결정적)")
        void handle_sameInputProducesSameLeadIn() {
            FollowUpResponse r1 = handler.handle(42L, 3, MAIN_QUESTION, ANSWER_TEXT);
            FollowUpResponse r2 = handler.handle(42L, 3, MAIN_QUESTION, ANSWER_TEXT);

            assertThat(r1.getQuestion()).isEqualTo(r2.getQuestion());
        }
    }
}
