package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.questionset.dto.*;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.domain.questionset.service.QuestionSetService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionSetController {

    private final QuestionSetService questionSetService;
    private final InternalQuestionSetService internalQuestionSetService;

    @PostMapping("/answers")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody SaveAnswersRequest request) {

        questionSetService.saveAnswers(questionSetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> generateUploadUrl(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody UploadUrlRequest request) {

        UploadUrlResponse response = questionSetService.generateUploadUrl(interviewId, questionSetId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QuestionSetStatusResponse>> getStatus(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        QuestionSetStatusResponse response = questionSetService.getStatus(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<QuestionSetFeedbackResponse>> getFeedback(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        QuestionSetFeedbackResponse response = questionSetService.getFeedback(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/questions-with-answers")
    public ResponseEntity<ApiResponse<QuestionsWithAnswersResponse>> getQuestionsWithAnswers(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        QuestionsWithAnswersResponse response = questionSetService.getQuestionsWithAnswers(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/retry-analysis")
    public ResponseEntity<ApiResponse<Void>> retryAnalysis(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        internalQuestionSetService.retryAnalysis(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
