package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.questionset.dto.UpdateConvertStatusRequest;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.question.dto.AnswersResponse;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/interviews/{interviewId}/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class InternalQuestionSetController {

    private final InternalQuestionSetService internalQuestionSetService;

    @PutMapping("/progress")
    public ResponseEntity<ApiResponse<Void>> updateProgress(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody UpdateProgressRequest request) {

        internalQuestionSetService.updateProgress(questionSetId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @GetMapping("/answers")
    public ResponseEntity<ApiResponse<AnswersResponse>> getAnswers(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {
        AnswersResponse response = internalQuestionSetService.getAnswersResponse(interviewId, questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PutMapping("/convert-status")
    public ResponseEntity<ApiResponse<Void>> updateConvertStatus(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody UpdateConvertStatusRequest request) {

        internalQuestionSetService.updateConvertStatus(questionSetId, request);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    @PostMapping("/retry-analysis")
    public ResponseEntity<ApiResponse<Void>> retryAnalysis(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        internalQuestionSetService.retryAnalysis(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
