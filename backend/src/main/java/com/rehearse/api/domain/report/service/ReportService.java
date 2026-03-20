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
import org.springframework.dao.DataIntegrityViolationException;
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
        // 리포트가 이미 존재하면 바로 반환
        return reportRepository.findByInterviewId(interviewId)
                .map(ReportResponse::from)
                .orElseThrow(() -> {
                    // 리포트 없음 → 분석 상태에 따라 에러 구분
                    List<QuestionSet> allSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
                    boolean allResolved = !allSets.isEmpty() && allSets.stream()
                            .allMatch(qs -> qs.getAnalysisStatus().isResolved());
                    boolean hasCompleted = allSets.stream()
                            .anyMatch(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED);

                    if (allResolved && hasCompleted) {
                        // 분석 완료 but 리포트 미생성 → 생성 중 (202)
                        return new BusinessException(ReportErrorCode.REPORT_GENERATING);
                    }
                    // 분석 미완료 → 400
                    return new BusinessException(ReportErrorCode.ANALYSIS_NOT_COMPLETED);
                });
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

        // 모든 질문세트가 COMPLETED 또는 SKIPPED인지 확인 (최소 1개 COMPLETED 필요)
        boolean allResolved = !questionSets.isEmpty() && questionSets.stream()
                .allMatch(qs -> qs.getAnalysisStatus().isResolved());
        boolean hasCompleted = questionSets.stream()
                .anyMatch(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED);

        if (!allResolved || !hasCompleted) {
            throw new BusinessException(ReportErrorCode.ANALYSIS_NOT_COMPLETED);
        }

        // 피드백 수집 (COMPLETED 세트만)
        List<Long> questionSetIds = questionSets.stream()
                .filter(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                .map(QuestionSet::getId)
                .toList();
        List<QuestionSetFeedback> feedbacks = feedbackRepository.findByQuestionSetIdIn(questionSetIds);

        // 피드백 요약 생성
        String feedbackSummary = buildFeedbackSummary(questionSets, feedbacks);

        // Claude API로 종합 리포트 생성
        GeneratedReport generated;
        try {
            generated = aiClient.generateReport(feedbackSummary);
        } catch (Exception e) {
            log.error("리포트 생성 실패: interviewId={}", interviewId, e);
            throw new BusinessException(ReportErrorCode.REPORT_GENERATION_FAILED);
        }

        InterviewReport report = InterviewReport.builder()
                .interview(interview)
                .overallScore(generated.getOverallScore())
                .summary(generated.getSummary())
                .strengths(generated.getStrengths())
                .improvements(generated.getImprovements())
                .feedbackCount(feedbacks.size())
                .build();

        try {
            reportRepository.save(report);
        } catch (DataIntegrityViolationException e) {
            log.info("리포트 동시 생성 감지, 기존 리포트 반환: interviewId={}", interviewId);
            return reportRepository.findByInterviewId(interviewId)
                    .map(ReportResponse::from)
                    .orElseThrow(() -> new BusinessException(ReportErrorCode.REPORT_GENERATION_FAILED));
        }

        log.info("종합 리포트 생성 완료: interviewId={}, score={}", interviewId, generated.getOverallScore());

        return ReportResponse.from(report);
    }

    private String buildFeedbackSummary(List<QuestionSet> questionSets, List<QuestionSetFeedback> feedbacks) {
        StringBuilder sb = new StringBuilder();
        long completedCount = questionSets.stream()
                .filter(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                .count();
        long skippedCount = questionSets.stream()
                .filter(qs -> qs.getAnalysisStatus() == AnalysisStatus.SKIPPED)
                .count();
        sb.append(String.format("면접 질문세트 수: %d개 (분석 완료: %d개, 건너뜀: %d개)\n\n",
                questionSets.size(), completedCount, skippedCount));

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
