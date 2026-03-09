package com.devlens.api.domain.interview.controller;

import com.devlens.api.domain.interview.dto.*;
import com.devlens.api.domain.interview.service.InterviewService;
import com.devlens.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @Valid @RequestBody CreateInterviewRequest request) {

        InterviewResponse response = interviewService.createInterview(request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(@PathVariable Long id) {

        InterviewResponse response = interviewService.getInterview(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PatchMapping("/{id}/status")
    public ResponseEntity<ApiResponse<UpdateStatusResponse>> updateStatus(
            @PathVariable Long id,
            @Valid @RequestBody UpdateStatusRequest request) {

        UpdateStatusResponse response = interviewService.updateStatus(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/follow-up")
    public ResponseEntity<ApiResponse<FollowUpResponse>> generateFollowUp(
            @PathVariable Long id,
            @Valid @RequestBody FollowUpRequest request) {

        FollowUpResponse response = interviewService.generateFollowUp(id, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
