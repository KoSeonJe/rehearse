package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class InterviewCompletionService {

    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;

    @Scheduled(fixedDelay = 30_000)
    @Transactional
    public void checkAndCompleteInterviews() {
        List<Interview> inProgressInterviews = interviewRepository.findByStatus(InterviewStatus.IN_PROGRESS);

        for (Interview interview : inProgressInterviews) {
            Long interviewId = interview.getId();
            List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);

            if (questionSets.isEmpty()) {
                continue;
            }

            long totalCount = questionSets.size();
            long completedCount = 0;
            long skippedCount = 0;

            for (QuestionSet qs : questionSets) {
                QuestionSetAnalysis analysis = qs.getAnalysis();
                if (analysis == null) {
                    // analysis 미생성 = 아직 답변 전 상태 → 완료 카운트 불가
                    continue;
                }

                AnalysisStatus status = analysis.getAnalysisStatus();
                if (status == AnalysisStatus.COMPLETED || status == AnalysisStatus.PARTIAL) {
                    completedCount++;
                } else if (status == AnalysisStatus.SKIPPED) {
                    skippedCount++;
                }
            }

            if (completedCount + skippedCount == totalCount && completedCount > 0) {
                Interview freshInterview = interviewRepository.findById(interviewId).orElse(null);
                if (freshInterview == null || freshInterview.getStatus() == InterviewStatus.COMPLETED) {
                    continue;
                }

                int overallScore = calculateOverallScore(questionSets);
                String overallComment = String.format("전체 %d개 질문세트 중 %d개 분석 완료, %d개 건너뜀",
                        totalCount, completedCount, skippedCount);

                freshInterview.updateOverallResult(overallScore, overallComment);
                freshInterview.updateStatus(InterviewStatus.COMPLETED);

                log.info("면접 완료 처리: interviewId={}, overallScore={}, completed={}, skipped={}",
                        interviewId, overallScore, completedCount, skippedCount);
            }
        }
    }

    private int calculateOverallScore(List<QuestionSet> questionSets) {
        List<Long> questionSetIds = questionSets.stream()
                .filter(qs -> {
                    QuestionSetAnalysis analysis = qs.getAnalysis();
                    return analysis != null && (analysis.getAnalysisStatus() == AnalysisStatus.COMPLETED
                            || analysis.getAnalysisStatus() == AnalysisStatus.PARTIAL);
                })
                .map(QuestionSet::getId)
                .toList();

        var feedbacks = feedbackRepository.findByQuestionSetIdIn(questionSetIds);

        if (feedbacks.isEmpty()) {
            return 0;
        }

        int totalScore = feedbacks.stream()
                .mapToInt(f -> f.getQuestionSetScore())
                .sum();

        return totalScore / feedbacks.size();
    }
}
