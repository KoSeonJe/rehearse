package com.rehearse.api.domain.feedback.controller;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.service.FeedbackService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/interviews/{interviewId}/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class FeedbackController {

    private final FeedbackService feedbackService;

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> saveFeedback(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody SaveFeedbackRequest request) {

        feedbackService.saveFeedback(questionSetId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
