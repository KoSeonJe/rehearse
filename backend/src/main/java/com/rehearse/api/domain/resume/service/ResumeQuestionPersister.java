package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionPersister {

    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final InterviewFinder interviewFinder;

    @Transactional
    public Long persist(Long interviewId, QuestionType questionType,
                        String questionText, String ttsText, String bestAnswer, int orderIndex) {
        QuestionSet questionSet = findOrCreateQuestionSet(interviewId);
        Question question = Question.resume(questionSet, questionType, questionText, ttsText, bestAnswer, orderIndex);
        questionRepository.save(question);
        log.debug("[ResumeQuestionPersister] 질문 저장: interviewId={}, type={}, questionId={}",
                interviewId, questionType, question.getId());
        return question.getId();
    }

    private QuestionSet findOrCreateQuestionSet(Long interviewId) {
        return questionSetRepository
                .findByInterviewIdAndCategory(interviewId, InterviewType.RESUME_BASED)
                .orElseGet(() -> createQuestionSet(interviewId));
    }

    private QuestionSet createQuestionSet(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        long existingCount = questionSetRepository.countByInterviewId(interviewId);
        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category(InterviewType.RESUME_BASED)
                .orderIndex((int) existingCount)
                .build();
        questionSetRepository.save(questionSet);
        log.info("[ResumeQuestionPersister] RESUME_BASED QuestionSet 생성: interviewId={}, questionSetId={}",
                interviewId, questionSet.getId());
        return questionSet;
    }
}
