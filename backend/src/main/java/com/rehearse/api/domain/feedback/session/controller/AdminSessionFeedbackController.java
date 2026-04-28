package com.rehearse.api.domain.feedback.session.controller;

import com.rehearse.api.domain.feedback.session.SessionFeedbackService;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackResponse;
import com.rehearse.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/admin/interviews")
@RequiredArgsConstructor
@PreAuthorize("hasRole('ADMIN')")
public class AdminSessionFeedbackController {

    private final SessionFeedbackService sessionFeedbackService;

    @GetMapping("/{id}/session-feedback")
    public ResponseEntity<ApiResponse<SessionFeedbackResponse>> getSessionFeedback(
            @PathVariable Long id) {
        SessionFeedbackResponse response = sessionFeedbackService.getByInterview(id);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping("/{id}/session-feedback/retry-delivery")
    public ResponseEntity<ApiResponse<Void>> retryDelivery(
            @PathVariable Long id,
            @AuthenticationPrincipal Long adminUserId) {
        sessionFeedbackService.retryDelivery(id, adminUserId);
        return ResponseEntity.ok(ApiResponse.ok());
    }
}
