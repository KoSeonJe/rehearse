package com.rehearse.api.domain.question.controller;

import com.rehearse.api.domain.admin.exception.AdminErrorCode;
import com.rehearse.api.domain.question.dto.AdminQuestionPoolResponse;
import com.rehearse.api.domain.question.dto.AdminQuestionPoolSearchCondition;
import com.rehearse.api.domain.question.dto.CreateQuestionPoolRequest;
import com.rehearse.api.domain.question.dto.DeactivateQuestionPoolsRequest;
import com.rehearse.api.domain.question.dto.UpdateQuestionPoolRequest;
import com.rehearse.api.domain.question.service.AdminQuestionPoolService;
import com.rehearse.api.global.common.ApiResponse;
import com.rehearse.api.global.exception.BusinessException;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/question-pools")
@RequiredArgsConstructor
public class AdminQuestionPoolController {

    private final AdminQuestionPoolService adminQuestionPoolService;

    @Value("${app.admin.password}")
    private String adminPassword;

    @GetMapping
    public ResponseEntity<ApiResponse<Page<AdminQuestionPoolResponse>>> searchQuestionPools(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "20") int size,
            @RequestParam(required = false) String cacheKey,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) Boolean isActive,
            @RequestParam(required = false) String keyword) {

        validateAdminPassword(password);

        int safeSize = Math.min(Math.max(size, 1), 100);
        AdminQuestionPoolSearchCondition condition = new AdminQuestionPoolSearchCondition(
                cacheKey, category, isActive, keyword);
        Page<AdminQuestionPoolResponse> response = adminQuestionPoolService.search(
                condition,
                PageRequest.of(page, safeSize));

        return ResponseEntity.ok(ApiResponse.ok(response));
    }

    @PostMapping
    public ResponseEntity<ApiResponse<AdminQuestionPoolResponse>> createQuestionPool(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @Valid @RequestBody CreateQuestionPoolRequest request) {

        validateAdminPassword(password);
        return ResponseEntity.ok(ApiResponse.ok(adminQuestionPoolService.create(request)));
    }

    @PatchMapping("/{id}")
    public ResponseEntity<ApiResponse<AdminQuestionPoolResponse>> updateQuestionPool(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @PathVariable Long id,
            @Valid @RequestBody UpdateQuestionPoolRequest request) {

        validateAdminPassword(password);
        return ResponseEntity.ok(ApiResponse.ok(adminQuestionPoolService.update(id, request)));
    }

    @PatchMapping("/{id}/deactivate")
    public ResponseEntity<ApiResponse<AdminQuestionPoolResponse>> deactivateQuestionPool(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @PathVariable Long id) {

        validateAdminPassword(password);
        return ResponseEntity.ok(ApiResponse.ok(adminQuestionPoolService.deactivate(id)));
    }

    @PatchMapping("/deactivate")
    public ResponseEntity<ApiResponse<Void>> deactivateQuestionPools(
            @RequestHeader(value = "X-Admin-Password", required = false) String password,
            @Valid @RequestBody DeactivateQuestionPoolsRequest request) {

        validateAdminPassword(password);
        adminQuestionPoolService.deactivateAll(request.ids());
        return ResponseEntity.ok(ApiResponse.ok(null));
    }

    private void validateAdminPassword(String password) {
        if (password == null || !adminPassword.equals(password)) {
            throw new BusinessException(AdminErrorCode.INVALID_PASSWORD);
        }
    }
}
