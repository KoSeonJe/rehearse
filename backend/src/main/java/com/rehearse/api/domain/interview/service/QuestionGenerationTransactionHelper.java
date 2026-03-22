package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class QuestionGenerationTransactionHelper {

    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;

    @Transactional
    public void startGeneration(Long interviewId) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));
        interview.startQuestionGeneration();
        interviewRepository.flush();
        log.info("질문 생성 시작: interviewId={}", interviewId);
    }

    @Transactional
    public void saveResults(Long interviewId, List<QuestionSet> questionSets) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new BusinessException(InterviewErrorCode.NOT_FOUND));

        questionSets.forEach(qs -> qs.assignInterview(interview));
        questionSetRepository.saveAll(questionSets);
        interview.completeQuestionGeneration();
        interviewRepository.save(interview);

        log.info("질문 생성 완료: interviewId={}, questionSets={}", interviewId, questionSets.size());
    }

    @Transactional
    public void failGeneration(Long interviewId, String reason) {
        interviewRepository.findById(interviewId).ifPresent(interview -> {
            interview.failQuestionGeneration(reason);
            interviewRepository.save(interview);
            log.error("질문 생성 실패: interviewId={}, reason={}", interviewId, reason);
        });
    }
}
