package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
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

            CompletionSummary summary = summarize(questionSets);

            if (summary.isAllResolved()) {
                Interview freshInterview = interviewRepository.findById(interviewId).orElse(null);
                if (freshInterview == null || freshInterview.getStatus() == InterviewStatus.COMPLETED) {
                    continue;
                }

                freshInterview.completeWithComment(summary.toComment());
                freshInterview.updateStatus(InterviewStatus.COMPLETED);

                log.info("면접 완료 처리: interviewId={}, completed={}, partial={}, skipped={}",
                        interviewId, summary.completed, summary.partial, summary.skipped);
            }
        }
    }

    private record CompletionSummary(long total, long completed, long partial, long skipped) {
        boolean isAllResolved() {
            return completed + partial + skipped == total && (completed + partial) > 0;
        }

        String toComment() {
            return String.format("전체 %d개 질문세트 중 %d개 완료, %d개 부분완료, %d개 건너뜀",
                    total, completed, partial, skipped);
        }
    }

    private CompletionSummary summarize(List<QuestionSet> questionSets) {
        long completed = 0;
        long partial = 0;
        long skipped = 0;

        for (QuestionSet qs : questionSets) {
            AnalysisStatus status = qs.getEffectiveAnalysisStatus();
            if (status.isFullyCompleted()) {
                completed++;
            } else if (status.isPartiallyCompleted()) {
                partial++;
            } else if (status == AnalysisStatus.SKIPPED) {
                skipped++;
            }
        }

        return new CompletionSummary(questionSets.size(), completed, partial, skipped);
    }

}
