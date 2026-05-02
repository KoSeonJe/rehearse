package com.rehearse.api.domain.feedback.session.controller;

import com.rehearse.api.domain.feedback.session.SessionFeedbackService;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackResponse;
import com.rehearse.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews")
@RequiredArgsConstructor
public class UserSessionFeedbackController {

    private final SessionFeedbackService sessionFeedbackService;

    @GetMapping("/{id}/session-feedback")
    public ResponseEntity<ApiResponse<SessionFeedbackResponse>> getSessionFeedback(
            @AuthenticationPrincipal Long userId,
            @PathVariable Long id) {
        SessionFeedbackResponse response = sessionFeedbackService.getByInterviewForUser(id, userId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
