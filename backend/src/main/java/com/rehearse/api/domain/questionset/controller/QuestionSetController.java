package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.feedback.dto.QuestionSetFeedbackResponse;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.dto.QuestionsWithAnswersResponse;
import com.rehearse.api.domain.question.dto.SaveAnswersRequest;
import com.rehearse.api.domain.questionset.dto.QuestionSetStatusResponse;
import com.rehearse.api.domain.questionset.dto.UploadUrlRequest;
import com.rehearse.api.domain.questionset.dto.UploadUrlResponse;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.domain.questionset.service.QuestionSetService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/question-sets/{questionSetId}")
@RequiredArgsConstructor
public class QuestionSetController {

    private final QuestionSetService questionSetService;
    private final InternalQuestionSetService internalQuestionSetService;
    private final InterviewFinder interviewFinder;

    @PostMapping("/answers")
    public ResponseEntity<ApiResponse<Void>> saveAnswers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody SaveAnswersRequest request) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        questionSetService.saveAnswers(questionSetId, request);
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.ok(null));
    }

    @PostMapping("/upload-url")
    public ResponseEntity<ApiResponse<UploadUrlResponse>> generateUploadUrl(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody UploadUrlRequest request) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        UploadUrlResponse response = questionSetService.generateUploadUrl(interviewId, questionSetId, request);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/status")
    public ResponseEntity<ApiResponse<QuestionSetStatusResponse>> getStatus(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        QuestionSetStatusResponse response = questionSetService.getStatus(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/feedback")
    public ResponseEntity<ApiResponse<QuestionSetFeedbackResponse>> getFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        QuestionSetFeedbackResponse response = questionSetService.getFeedback(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @GetMapping("/questions-with-answers")
    public ResponseEntity<ApiResponse<QuestionsWithAnswersResponse>> getQuestionsWithAnswers(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        QuestionsWithAnswersResponse response = questionSetService.getQuestionsWithAnswers(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/retry-analysis")
    public ResponseEntity<ApiResponse<Void>> retryAnalysis(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        interviewFinder.findById(interviewId).validateOwner(userId);
        internalQuestionSetService.retryAnalysis(questionSetId);
        return ResponseEntity.ok(ApiResponse.ok(null));
    }
}
