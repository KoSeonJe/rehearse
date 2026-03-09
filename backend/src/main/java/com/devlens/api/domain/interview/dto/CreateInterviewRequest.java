package com.devlens.api.domain.interview.dto;

import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewType;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class CreateInterviewRequest {

    @NotBlank(message = "직무를 입력해주세요.")
    @Size(max = 100, message = "직무는 100자 이내로 입력해주세요.")
    private String position;

    @NotNull(message = "레벨을 선택해주세요.")
    private InterviewLevel level;

    @NotNull(message = "면접 유형을 선택해주세요.")
    private InterviewType interviewType;
}
