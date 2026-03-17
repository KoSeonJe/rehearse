package com.rehearse.api.domain.report.controller;

import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.service.ReportService;
import com.rehearse.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable Long interviewId) {

        ReportResponse response = reportService.getReport(interviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
