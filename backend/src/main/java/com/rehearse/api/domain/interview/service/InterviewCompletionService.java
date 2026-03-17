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

            if (completedCount == totalCount) {
                int overallScore = calculateOverallScore(interviewId);
                String overallComment = "전체 " + totalCount + "개 질문세트 분석 완료";

                interview.updateOverallResult(overallScore, overallComment);
                interview.updateStatus(InterviewStatus.COMPLETED);

                log.info("면접 완료 처리: interviewId={}, overallScore={}, questionSets={}",
                        interviewId, overallScore, totalCount);
            }
        }
    }

    private int calculateOverallScore(Long interviewId) {
        var questionSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        List<Long> questionSetIds = questionSets.stream()
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
