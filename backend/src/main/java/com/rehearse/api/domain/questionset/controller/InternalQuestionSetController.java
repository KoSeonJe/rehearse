package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.questionset.dto.AnswerResponse;
import com.rehearse.api.domain.questionset.dto.AnswersResponse;
import com.rehearse.api.domain.questionset.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.service.InternalQuestionSetService;
import com.rehearse.api.global.common.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;

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

        QuestionSet questionSet = internalQuestionSetService.getQuestionSet(questionSetId);
        List<AnswerResponse> answers = internalQuestionSetService.getAnswers(questionSetId).stream()
                .map(AnswerResponse::from)
                .toList();

        AnswersResponse response = AnswersResponse.builder()
                .analysisStatus(questionSet.getAnalysisStatus().name())
                .answers(answers)
                .build();
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/feedback")
    public ResponseEntity<ApiResponse<Void>> saveFeedback(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId,
            @Valid @RequestBody SaveFeedbackRequest request) {

        internalQuestionSetService.saveFeedback(questionSetId, request);
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
