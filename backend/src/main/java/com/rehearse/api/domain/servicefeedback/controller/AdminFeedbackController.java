package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.servicefeedback.dto.AdminFeedbackResponse;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/admin/feedbacks")
@RequiredArgsConstructor
public class AdminFeedbackController {

    private final ServiceFeedbackService serviceFeedbackService;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminFeedbackResponse>>> getAdminFeedbacks(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size) {
        int safeSize = Math.min(Math.max(size, 1), 100);
        Page<AdminFeedbackResponse> response = serviceFeedbackService.getAdminFeedbacks(PageRequest.of(page, safeSize));
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
