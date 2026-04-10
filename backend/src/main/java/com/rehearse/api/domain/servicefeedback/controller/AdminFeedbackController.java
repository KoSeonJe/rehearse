package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.admin.exception.AdminErrorCode;
import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final ServiceFeedbackService serviceFeedbackService;

    @Value("${app.admin.password}")
    private String adminPassword;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminFeedbackResponse>>> getAdminFeedbacks(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        if (password == null || !adminPassword.equals(password)) {
            throw new BusinessException(AdminErrorCode.INVALID_PASSWORD);
        }
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AdminFeedbackResponse> response = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(page, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
