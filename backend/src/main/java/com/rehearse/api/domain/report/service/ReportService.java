package com.rehearse.api.domain.report.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetFeedback;
import com.rehearse.api.domain.questionset.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
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
    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;
    private final InterviewFinder interviewFinder;
    private final AiClient aiClient;

    public ReportResponse getReport(Long interviewId) {
        InterviewReport report = reportRepository.findByInterviewId(interviewId)
                .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_NOT_FOUND));

        return ReportResponse.from(report);
    }

    @Transactional
    public ReportResponse generateReport(Long interviewId) {
        // 이미 리포트가 있으면 기존 것 반환 (멱등성)
        return reportRepository.findByInterviewId(interviewId)
                .map(ReportResponse::from)
                .orElseGet(() -> createReport(interviewId));
    }

    private ReportResponse createReport(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(interviewId);

        // 모든 질문세트가 COMPLETED인지 확인
        boolean allCompleted = !questionSets.isEmpty() && questionSets.stream()
                .allMatch(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED);

        if (!allCompleted) {
            throw new BusinessException(ReportErrorCode.ANALYSIS_NOT_COMPLETED);
        }

        // 피드백 수집
        List<Long> questionSetIds = questionSets.stream()
                .map(QuestionSet::getId)
                .toList();
        List<QuestionSetFeedback> feedbacks = feedbackRepository.findByQuestionSetIdIn(questionSetIds);

        // 피드백 요약 생성
        String feedbackSummary = buildFeedbackSummary(questionSets, feedbacks);

        // Claude API로 종합 리포트 생성
        GeneratedReport generated = aiClient.generateReport(feedbackSummary);

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(generated.getOverallScore())
                .summary(generated.getSummary())
                .strengths(generated.getStrengths())
                .improvements(generated.getImprovements())
                .feedbackCount(feedbacks.size())
                .build();

        reportRepository.save(report);

        log.info("종합 리포트 생성 완료: interviewId={}, score={}", interviewId, generated.getOverallScore());

        return ReportResponse.from(report);
    }

    private String buildFeedbackSummary(List<QuestionSet> questionSets, List<QuestionSetFeedback> feedbacks) {
        StringBuilder sb = new StringBuilder();
        sb.append(String.format("면접 질문세트 수: %d개\n\n", questionSets.size()));

        for (QuestionSetFeedback feedback : feedbacks) {
            QuestionSet qs = feedback.getQuestionSet();
            sb.append(String.format("## 질문세트 (카테고리: %s)\n", qs.getCategory()));
            sb.append(String.format("- 점수: %d/100\n", feedback.getQuestionSetScore()));
            sb.append(String.format("- 평가: %s\n", feedback.getQuestionSetComment()));

            // 질문 텍스트 포함
            qs.getQuestions().forEach(q ->
                    sb.append(String.format("- 질문 [%s]: %s\n", q.getQuestionType(), q.getQuestionText()))
            );

            sb.append("\n");
        }

        return sb.toString();
    }
}
