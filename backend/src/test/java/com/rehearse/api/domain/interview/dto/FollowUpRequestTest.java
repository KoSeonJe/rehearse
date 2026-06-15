package com.rehearse.api.domain.interview.dto;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("FollowUpRequest - Jackson 역직렬화 (terminate 필드)")
class FollowUpRequestTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("terminate 필드는 boolean primitive (default false). FE 가 미전송해도 false 로 안전 환원된다")
    class TerminateField {

        @Test
        @DisplayName("terminate 미포함 JSON → isTerminate() == false (default)")
        void deserialize_missingTerminate_defaultsFalse() throws Exception {
            String json = """
                    {
                      "questionSetId": 12,
                      "questionContent": "프로젝트에서 가장 어려웠던 점은?",
                      "answerText": "...",
                      "previousExchanges": []
                    }
                    """;

            FollowUpRequest request = objectMapper.readValue(json, FollowUpRequest.class);

            assertThat(request.isTerminate()).isFalse();
            assertThat(request.getQuestionSetId()).isEqualTo(12L);
        }

        @Test
        @DisplayName("terminate=true JSON → isTerminate() == true")
        void deserialize_terminateTrue_returnsTrue() throws Exception {
            String json = """
                    {
                      "questionSetId": 12,
                      "questionContent": "프로젝트에서 가장 어려웠던 점은?",
                      "answerText": "...",
                      "terminate": true
                    }
                    """;

            FollowUpRequest request = objectMapper.readValue(json, FollowUpRequest.class);

            assertThat(request.isTerminate()).isTrue();
        }

        @Test
        @DisplayName("terminate=false 명시 JSON → isTerminate() == false")
        void deserialize_terminateFalse_returnsFalse() throws Exception {
            String json = """
                    {
                      "questionSetId": 12,
                      "questionContent": "프로젝트에서 가장 어려웠던 점은?",
                      "answerText": "...",
                      "terminate": false
                    }
                    """;

            FollowUpRequest request = objectMapper.readValue(json, FollowUpRequest.class);

            assertThat(request.isTerminate()).isFalse();
        }
    }
}
