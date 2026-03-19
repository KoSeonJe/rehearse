package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
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
            long totalCount = questionSetRepository.countByInterviewId(interviewId);

            if (totalCount == 0) {
                continue;
            }

            long completedCount = questionSetRepository.countByInterviewIdAndAnalysisStatus(
                    interviewId, AnalysisStatus.COMPLETED);
            long skippedCount = questionSetRepository.countByInterviewIdAndAnalysisStatus(
                    interviewId, AnalysisStatus.SKIPPED);

            if (completedCount + skippedCount == totalCount && completedCount > 0) {
                // FE에서 이미 COMPLETED로 전이했을 수 있음 (중도 종료 시)
                if (interview.getStatus() == InterviewStatus.COMPLETED) {
                    continue;
                }

                int overallScore = calculateOverallScore(interviewId);
                String overallComment = String.format("전체 %d개 질문세트 중 %d개 분석 완료, %d개 건너뜀",
                        totalCount, completedCount, skippedCount);

                interview.updateOverallResult(overallScore, overallComment);
                interview.updateStatus(InterviewStatus.COMPLETED);

                log.info("면접 완료 처리: interviewId={}, overallScore={}, completed={}, skipped={}",
                        interviewId, overallScore, completedCount, skippedCount);
            }
        }
    }

    private int calculateOverallScore(Long interviewId) {
        var questionSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        List<Long> questionSetIds = questionSets.stream()
                .filter(qs -> qs.getAnalysisStatus() == AnalysisStatus.COMPLETED)
                .map(qs -> qs.getId())
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
