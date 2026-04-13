package com.rehearse.api.domain.question.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class SaveAnswersRequest {

    @NotEmpty(message = "답변 목록은 비어있을 수 없습니다.")
    @Valid
    private List<AnswerTimestamp> answers;

    @Getter
    @NoArgsConstructor
    public static class AnswerTimestamp {
        @NotNull(message = "질문 ID는 필수입니다.")
        private Long questionId;

        @NotNull(message = "시작 시간은 필수입니다.")
        private Long startMs;

        @NotNull(message = "종료 시간은 필수입니다.")
        private Long endMs;
    }
}
