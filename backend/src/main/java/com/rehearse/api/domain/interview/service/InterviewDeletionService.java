package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.analysis.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.question.repository.QuestionAnswerRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewDeletionService {

    private final InterviewFinder interviewFinder;
    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionAnswerRepository questionAnswerRepository;
    private final TimestampFeedbackRepository timestampFeedbackRepository;
    private final QuestionSetFeedbackRepository questionSetFeedbackRepository;
    private final QuestionSetAnalysisRepository questionSetAnalysisRepository;

    @Transactional
    public void deleteInterview(Long id, Long userId) {
        Interview interview = interviewFinder.findByIdAndValidateOwner(id, userId);

        // 하위 엔티티부터 명시적 삭제 (FK 제약조건 위반 방지)
        questionAnswerRepository.deleteAllByInterviewId(id);
        timestampFeedbackRepository.deleteAllByInterviewId(id);
        questionSetFeedbackRepository.deleteAllByInterviewId(id);
        questionSetAnalysisRepository.deleteAllByInterviewId(id);
        questionSetRepository.deleteAll(questionSetRepository.findByInterviewIdOrderByOrderIndex(id));
        interviewRepository.delete(interview);

        log.info("면접 세션 삭제: id={}, userId={}", id, userId);
    }
}
