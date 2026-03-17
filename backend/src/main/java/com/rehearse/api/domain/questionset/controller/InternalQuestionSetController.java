package com.rehearse.api.domain.questionset.controller;

import com.rehearse.api.domain.questionset.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnswer;
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
    public ResponseEntity<ApiResponse<List<AnswerResponse>>> getAnswers(
            @PathVariable Long interviewId,
            @PathVariable Long questionSetId) {

        List<QuestionSetAnswer> answers = internalQuestionSetService.getAnswers(questionSetId);
        List<AnswerResponse> response = answers.stream()
                .map(a -> new AnswerResponse(
                        a.getId(),
                        a.getQuestion().getId(),
                        a.getQuestion().getQuestionType().name(),
                        a.getQuestion().getQuestionText(),
                        a.getStartMs(),
                        a.getEndMs()))
                .toList();
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

    public record AnswerResponse(Long id, Long questionId, String questionType,
                                  String questionText, long startMs, long endMs) {}
}
