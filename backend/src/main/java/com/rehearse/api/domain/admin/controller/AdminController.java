package com.rehearse.api.domain.admin.controller;

import com.rehearse.api.domain.admin.dto.AdminVerifyRequest;
import com.rehearse.api.domain.admin.exception.AdminErrorCode;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin")
@RequiredArgsConstructor
public class AdminController {

    @Value("${app.admin.password}")
    private String adminPassword;

    @PostMapping("/verify")
    public ApiResponse<Void> verifyAdminPassword(@Valid @RequestBody AdminVerifyRequest request) {
        if (!adminPassword.equals(request.password())) {
            throw new BusinessException(AdminErrorCode.INVALID_PASSWORD);
        }
        return ApiResponse.ok();
    }
}
