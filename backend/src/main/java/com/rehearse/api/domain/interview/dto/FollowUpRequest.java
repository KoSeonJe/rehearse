package com.rehearse.api.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FollowUpRequest {

    @NotNull(message = "질문세트 ID는 필수입니다.")
    private Long questionSetId;

    @NotBlank(message = "질문 내용을 입력해주세요.")
    private String questionContent;

    private String answerText;

    private String nonVerbalSummary;

    private List<FollowUpExchange> previousExchanges;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpExchange {
        private String question;
        private String answer;
        private String followUpType;
        private String selectedPerspective;

        public FollowUpExchange(String question, String answer) {
            this(question, answer, null, null);
        }

        public FollowUpExchange(String question, String answer, String followUpType) {
            this(question, answer, followUpType, null);
        }
    }
}
