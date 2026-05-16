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

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionPersister {

    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final InterviewFinder interviewFinder;

    @Transactional
    public int persistAll(Long interviewId, List<ResumeQuestionDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return 0;
        }
        QuestionSet questionSet = findOrCreateQuestionSet(interviewId);
        List<Question> questions = new ArrayList<>(drafts.size());
        for (ResumeQuestionDraft draft : drafts) {
            questions.add(Question.resume(
                    questionSet, draft.questionType(),
                    draft.questionText(), draft.ttsText(), draft.bestAnswer(), draft.orderIndex()));
        }
        questionRepository.saveAll(questions);
        log.info("[ResumeQuestionPersister] 질문 일괄 저장: interviewId={}, count={}",
                interviewId, questions.size());
        return questions.size();
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

    public record ResumeQuestionDraft(
            QuestionType questionType,
            String questionText,
            String ttsText,
            String bestAnswer,
            int orderIndex
    ) {
    }
}
