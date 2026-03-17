package com.rehearse.api.domain.report.service;

import com.rehearse.api.domain.feedback.entity.Feedback;
import com.rehearse.api.domain.feedback.repository.FeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.entity.InterviewReport;
import com.rehearse.api.domain.report.exception.ReportErrorCode;
import com.rehearse.api.domain.report.repository.ReportRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedReport;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final InterviewFinder interviewFinder;
    private final AiClient aiClient;

    @Transactional
    public ReportResponse getReport(Long interviewId) {
        InterviewReport report = reportRepository.findByInterviewId(interviewId)
                .orElseGet(() -> generateAndSaveReport(interviewId));

        return ReportResponse.from(report);
    }

    @Transactional
    InterviewReport generateAndSaveReport(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);

        List<Feedback> feedbacks = feedbackRepository.findByInterviewIdOrderByTimestampSeconds(interviewId);

        if (feedbacks.isEmpty()) {
            throw new BusinessException(ReportErrorCode.NO_FEEDBACK);
        }

        String feedbackSummary = feedbacks.stream()
                .map(f -> String.format("[%s/%s] %s", f.getCategory(), f.getSeverity(), f.getContent()))
                .collect(Collectors.joining("\n"));

        GeneratedReport generated = aiClient.generateReport(feedbackSummary);

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(generated.getOverallScore())
                .summary(generated.getSummary())
                .strengths(generated.getStrengths())
                .improvements(generated.getImprovements())
                .feedbackCount(feedbacks.size())
                .build();

        InterviewReport saved = reportRepository.save(report);
        log.info("리포트 생성 완료: interviewId={}, score={}", interviewId, saved.getOverallScore());

        return saved;
    }
}
