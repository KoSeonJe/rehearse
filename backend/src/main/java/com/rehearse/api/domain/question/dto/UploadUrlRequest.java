package com.rehearse.api.domain.question.dto;

import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.NoArgsConstructor;

@Getter
@NoArgsConstructor
public class UploadUrlRequest {

    @NotBlank(message = "Content-Type은 필수입니다.")
    private String contentType;
}
