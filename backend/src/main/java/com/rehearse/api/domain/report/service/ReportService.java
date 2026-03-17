package com.rehearse.api.domain.report.service;

import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.entity.InterviewReport;
import com.rehearse.api.domain.report.exception.ReportErrorCode;
import com.rehearse.api.domain.report.repository.ReportRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;

    public ReportResponse getReport(Long interviewId) {
        InterviewReport report = reportRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }
}
