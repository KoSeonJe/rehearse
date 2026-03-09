package com.devlens.api.domain.report.service;

import com.devlens.api.domain.feedback.entity.Feedback;
import com.devlens.api.domain.feedback.repository.FeedbackRepository;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.domain.report.dto.ReportResponse;
import com.devlens.api.domain.report.entity.InterviewReport;
import com.devlens.api.domain.report.repository.ReportRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.ClaudeApiClient;
import com.devlens.api.infra.ai.dto.GeneratedReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReportService {

    private final ReportRepository reportRepository;
    private final FeedbackRepository feedbackRepository;
    private final InterviewRepository interviewRepository;
    private final ClaudeApiClient claudeApiClient;

    public ReportResponse getReport(Long interviewId) {
        InterviewReport report = reportRepository.findByInterviewId(interviewId)
                .orElseGet(() -> generateAndSaveReport(interviewId));

        return ReportResponse.from(report);
    }

    @Transactional
    InterviewReport generateAndSaveReport(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."
                ));

        List<Feedback> feedbacks = feedbackRepository.findByInterviewIdOrderByTimestampSeconds(interviewId);

        if (feedbacks.isEmpty()) {
            throw new BusinessException(HttpStatus.CONFLICT, "REPORT_001", "피드백이 없어 리포트를 생성할 수 없습니다.");
        }

        String feedbackSummary = feedbacks.stream()
                .map(f -> String.format("[%s/%s] %s", f.getCategory(), f.getSeverity(), f.getContent()))
                .collect(Collectors.joining("\n"));

        GeneratedReport generated = claudeApiClient.generateReport(feedbackSummary);

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(generated.getOverallScore())
                .summary(generated.getSummary())
                .strengths(String.join("|", generated.getStrengths()))
                .improvements(String.join("|", generated.getImprovements()))
                .feedbackCount(feedbacks.size())
                .build();

        InterviewReport saved = reportRepository.save(report);
        log.info("리포트 생성 완료: interviewId={}, score={}", interviewId, saved.getOverallScore());

        return saved;
    }
}
