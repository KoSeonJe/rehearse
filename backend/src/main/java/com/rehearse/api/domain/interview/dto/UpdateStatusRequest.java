package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.InterviewStatus;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UpdateStatusRequest {

    @NotNull(message = "변경할 상태를 입력해주세요.")
    private InterviewStatus status;
}
