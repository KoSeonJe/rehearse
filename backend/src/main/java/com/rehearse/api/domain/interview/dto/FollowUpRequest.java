package com.rehearse.api.domain.interview.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
@JsonIgnoreProperties(ignoreUnknown = true)
public class FollowUpRequest {

    @NotNull(message = "질문세트 ID는 필수입니다.")
    private Long questionSetId;

    @NotBlank(message = "질문 내용을 입력해주세요.")
    private String questionContent;

    private String nonVerbalSummary;

    private List<FollowUpExchange> previousExchanges;

    private boolean terminate;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpExchange {
        private String question;
        private String answerText;
        private String followUpType;

        public FollowUpExchange(String question, String answerText) {
            this(question, answerText, null);
        }
    }
}
