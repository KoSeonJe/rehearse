package com.rehearse.api.infra.tts;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public record TtsRequest(
        @NotBlank(message = "텍스트는 필수입니다.")
        @Size(max = 1000, message = "텍스트는 1000자 이하여야 합니다.")
        String text
) {}