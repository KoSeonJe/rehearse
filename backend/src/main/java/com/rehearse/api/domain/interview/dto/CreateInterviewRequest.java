package com.rehearse.api.domain.interview.dto;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;

import java.util.List;

@Getter
@NoArgsConstructor
public class CreateInterviewRequest {

    @NotNull(message = "직무를 선택해주세요.")
    private Position position;

    @Size(max = 100, message = "직무 상세는 100자 이내로 입력해주세요.")
    private String positionDetail;

    @NotNull(message = "레벨을 선택해주세요.")
    private InterviewLevel level;

    @NotEmpty(message = "면접 유형을 최소 1개 선택해주세요.")
    private List<InterviewType> interviewTypes;

    private List<String> csSubTopics;

    @NotNull(message = "면접 시간을 설정해주세요.")
    @Min(value = 5, message = "면접 시간은 최소 5분입니다.")
    @Max(value = 120, message = "면접 시간은 최대 120분입니다.")
    private Integer durationMinutes;
}
