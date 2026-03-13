package com.devlens.api.domain.interview.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class FollowUpRequest {

    @NotBlank(message = "질문 내용을 입력해주세요.")
    private String questionContent;

    @NotBlank(message = "답변 텍스트를 입력해주세요.")
    private String answerText;

    private String nonVerbalSummary;

    private List<FollowUpExchange> previousExchanges;

    @Getter
    @NoArgsConstructor
    @AllArgsConstructor
    public static class FollowUpExchange {
        private String question;
        private String answer;
    }
}
