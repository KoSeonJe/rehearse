package com.rehearse.api.domain.interview.controller;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.service.InterviewService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class InterviewController {

    private final InterviewService interviewService;

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @Valid @RequestPart("request") CreateInterviewRequest request,
            @RequestPart(value = "resumeFile", required = false) MultipartFile resumeFile) {

        InterviewResponse response = interviewService.createInterview(request, resumeFile);
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

    @PostMapping(value = "/{id}/follow-up", consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<FollowUpResponse>> generateFollowUp(
            @PathVariable Long id,
            @Valid @RequestPart("request") FollowUpRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {

        FollowUpResponse response = interviewService.generateFollowUp(id, request, audioFile);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/retry-questions")
    public ResponseEntity<ApiResponse<InterviewResponse>> retryQuestionGeneration(
            @PathVariable Long id) {

        InterviewResponse response = interviewService.retryQuestionGeneration(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
