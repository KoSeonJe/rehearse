package com.rehearse.api.domain.interview.controller;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.service.FollowUpService;
import com.rehearse.api.domain.interview.service.InterviewCreationService;
import com.rehearse.api.domain.interview.service.InterviewQueryService;
import com.rehearse.api.domain.interview.service.InterviewService;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.config.AsyncConfig;
import jakarta.validation.Valid;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;

@RestController
@RequestMapping("/api/v1/interviews")
public class InterviewController {

    private final InterviewCreationService interviewCreationService;
    private final InterviewQueryService interviewQueryService;
    private final InterviewService interviewService;
    private final FollowUpService followUpService;
    private final Executor vtExecutor;

    public InterviewController(
            InterviewCreationService interviewCreationService,
            InterviewQueryService interviewQueryService,
            InterviewService interviewService,
            FollowUpService followUpService,
            @Qualifier(AsyncConfig.VT_EXECUTOR) Executor vtExecutor) {
        this.interviewCreationService = interviewCreationService;
        this.interviewQueryService = interviewQueryService;
        this.interviewService = interviewService;
        this.followUpService = followUpService;
        this.vtExecutor = vtExecutor;
    }

    @PostMapping(consumes = MediaType.MULTIPART_FORM_DATA_VALUE)
    public ResponseEntity<ApiResponse<InterviewResponse>> createInterview(
            @Valid @RequestPart("request") CreateInterviewRequest request,
            @RequestPart(value = "resumeFile", required = false) MultipartFile resumeFile) {
        InterviewResponse response = interviewCreationService.createInterview(request, resumeFile);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(response));
    }

    @GetMapping("/{id}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterview(@PathVariable Long id) {
        InterviewResponse response = interviewQueryService.getInterview(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/by-public-id/{publicId}")
    public ResponseEntity<ApiResponse<InterviewResponse>> getInterviewByPublicId(@PathVariable String publicId) {
        InterviewResponse response = interviewQueryService.getInterviewByPublicId(publicId);
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
    public CompletableFuture<ResponseEntity<ApiResponse<FollowUpResponse>>> generateFollowUp(
            @PathVariable Long id,
            @Valid @RequestPart("request") FollowUpRequest request,
            @RequestPart(value = "audio", required = false) MultipartFile audioFile) {
        return CompletableFuture.supplyAsync(
                () -> followUpService.generateFollowUp(id, request, audioFile),
                vtExecutor
        ).thenApply(response -> ResponseEntity.ok(ApiResponse.ok(response)));
    }

    @PostMapping("/{id}/retry-questions")
    public ResponseEntity<ApiResponse<InterviewResponse>> retryQuestionGeneration(
            @PathVariable Long id) {
        InterviewResponse response = interviewService.retryQuestionGeneration(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/skip-remaining")
    public ResponseEntity<ApiResponse<Void>> skipRemainingQuestionSets(
            @PathVariable Long id) {
        interviewService.skipRemainingQuestionSets(id);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
