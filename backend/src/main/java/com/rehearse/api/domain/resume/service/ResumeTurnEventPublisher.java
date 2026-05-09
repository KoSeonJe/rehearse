package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.feedback.rubric.event.TurnCompletedEvent;
import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.BlockReason;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeTurnEventPublisher {

    private final InterviewFinder interviewFinder;
    private final QuestionSetRepository questionSetRepository;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional(readOnly = true)
    public void publish(Long interviewId, long turnIndex, AnswerAnalysis analysis,
                        ResumeMode currentMode, int currentChainLevel,
                        ResumeSkeleton skeleton, String userAnswer, Long questionId) {
        if (questionId == null) {
            log.warn("[진행차단진단] interviewId={} track={} stage={} reason={} turnIndex={}",
                    interviewId, InterviewTrack.RESUME.logLabel(),
                    currentMode.name().toLowerCase(),
                    BlockReason.QUESTION_ID_MISSING.logValue(), turnIndex);
            return;
        }
        try {
            Interview interview = interviewFinder.findById(interviewId);
            QuestionSet questionSet = resolveQuestionSet(interviewId, questionId);
            TurnCompletedEvent event = TurnCompletedEvent.ofResumeTrack(
                    interviewId, turnIndex, interview.getUserId(),
                    questionId, questionSet != null ? questionSet.getId() : null,
                    userAnswer != null ? userAnswer : "",
                    analysis, interview.getLevel(),
                    currentMode, currentChainLevel, skeleton
            );
            eventPublisher.publishEvent(event);
        } catch (Exception e) {
            log.warn("Resume TurnCompletedEvent 발행 실패 — 턴 진행 차단하지 않음: interviewId={}, reason={}",
                    interviewId, e.getMessage());
        }
    }

    private QuestionSet resolveQuestionSet(Long interviewId, Long questionId) {
        if (questionId == null) {
            return null;
        }
        return questionSetRepository
                .findByInterviewIdAndCategory(interviewId, InterviewType.RESUME_BASED)
                .orElse(null);
    }
}
