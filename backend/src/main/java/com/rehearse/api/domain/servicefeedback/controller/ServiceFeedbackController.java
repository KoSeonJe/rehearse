package com.rehearse.api.domain.servicefeedback.controller;

import com.rehearse.api.domain.servicefeedback.dto.CreateServiceFeedbackRequest;
import com.rehearse.api.domain.servicefeedback.dto.FeedbackNeedCheckResponse;
import com.rehearse.api.domain.servicefeedback.service.ServiceFeedbackService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/service-feedbacks")
@RequiredArgsConstructor
public class ServiceFeedbackController {

    private final ServiceFeedbackService serviceFeedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<Void>> submitFeedback(
            @AuthenticationPrincipal Long userId,
            @Valid @RequestBody CreateServiceFeedbackRequest request) {
        serviceFeedbackService.submitFeedback(userId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @GetMapping("/need-check")
    public ResponseEntity<ApiResponse<FeedbackNeedCheckResponse>> checkNeedsFeedback(
            @AuthenticationPrincipal Long userId) {
        FeedbackNeedCheckResponse response = serviceFeedbackService.checkNeedsFeedback(userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
