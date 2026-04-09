package com.rehearse.api.domain.admin.dto;

import jakarta.validation.constraints.NotBlank;

public record AdminVerifyRequest(
        @NotBlank String password
) {}
