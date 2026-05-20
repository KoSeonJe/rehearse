package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionDepthType;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionPersister {

    private final QuestionSetRepository questionSetRepository;
    private final InterviewFinder interviewFinder;

    @Transactional
    public int persistAll(Long interviewId, List<ResumeQuestionDraft> drafts) {
        if (drafts == null || drafts.isEmpty()) {
            return 0;
        }
        Interview interview = interviewFinder.findById(interviewId);
        List<QuestionSet> questionSets = new ArrayList<>(drafts.size());
        for (ResumeQuestionDraft draft : drafts) {
            QuestionSet questionSet = QuestionSet.builder()
                    .interview(interview)
                    .category(InterviewType.RESUME_BASED)
                    .orderIndex(draft.orderIndex())
                    .build();
            Question question = Question.resume(
                    questionSet, draft.questionType(),
                    draft.questionText(), draft.ttsText(), draft.bestAnswer(), 0, draft.depthType());
            questionSet.addQuestion(question);
            questionSets.add(questionSet);
        }
        questionSetRepository.saveAll(questionSets);
        log.info("[ResumeQuestionPersister] 질문 일괄 저장 (QSet 1:1 분리): interviewId={}, count={}, depthTypeCounts={}",
                interviewId, questionSets.size(), countDepthTypes(drafts));
        return questionSets.size();
    }

    private Map<QuestionDepthType, Integer> countDepthTypes(List<ResumeQuestionDraft> drafts) {
        Map<QuestionDepthType, Integer> counts = new EnumMap<>(QuestionDepthType.class);
        for (ResumeQuestionDraft draft : drafts) {
            QuestionDepthType type = draft.depthType();
            if (type == null) {
                continue;
            }
            counts.merge(type, 1, Integer::sum);
        }
        return counts;
    }

    public record ResumeQuestionDraft(
            QuestionType questionType,
            String questionText,
            String ttsText,
            String bestAnswer,
            int orderIndex,
            QuestionDepthType depthType
    ) {
    }
}
