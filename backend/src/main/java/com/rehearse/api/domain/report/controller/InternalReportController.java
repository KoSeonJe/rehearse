package com.rehearse.api.domain.report.controller;

import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.service.ReportService;
import com.rehearse.api.global.common.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/internal/interviews/{interviewId}")
@RequiredArgsConstructor
public class InternalReportController {

    private final ReportService reportService;

    @PostMapping("/report")
    public ResponseEntity<ApiResponse<ReportResponse>> generateReport(
            @PathVariable Long interviewId) {

        ReportResponse response = reportService.generateReport(interviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
