package com.devlens.api.domain.feedback.controller;

import com.devlens.api.domain.feedback.dto.FeedbackListResponse;
import com.devlens.api.domain.feedback.dto.GenerateFeedbackRequest;
import com.devlens.api.domain.feedback.service.FeedbackService;
import com.devlens.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/feedbacks")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping
    public ResponseEntity<ApiResponse<FeedbackListResponse>> generateFeedback(
            @PathVariable Long interviewId,
            @Valid @RequestBody GenerateFeedbackRequest request) {

        FeedbackListResponse response = feedbackService.generateFeedback(interviewId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping
    public ResponseEntity<ApiResponse<FeedbackListResponse>> getFeedbacks(
            @PathVariable Long interviewId) {

        FeedbackListResponse response = feedbackService.getFeedbacks(interviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
