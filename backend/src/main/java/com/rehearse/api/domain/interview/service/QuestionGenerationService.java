package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
public class QuestionGenerationService {

    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;
    private final AiClient aiClient;

    @Async("questionGenerationExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionGenerationEvent(QuestionGenerationRequestedEvent event) {
        try {
            generateQuestions(event.getInterviewId(), event.getPosition(), event.getPositionDetail(),
                    event.getLevel(), event.getInterviewTypes(), event.getCsSubTopics(),
                    event.getResumeText(), event.getDurationMinutes());
        } catch (Exception e) {
            log.error("질문 생성 비동기 작업 실패: interviewId={}", event.getInterviewId(), e);
            // self-invocation 방지: 직접 Repository 사용
            interviewRepository.findById(event.getInterviewId()).ifPresent(interview -> {
                interview.failQuestionGeneration(e.getMessage());
                interviewRepository.save(interview);
            });
        }
    }

    @Transactional
    public void generateQuestions(Long interviewId, Position position, String positionDetail,
                                  InterviewLevel level, List<InterviewType> interviewTypes,
                                  List<String> csSubTopics, String resumeText,
                                  Integer durationMinutes) {
        Interview interview = interviewRepository.findById(interviewId)
                .orElseThrow(() -> new IllegalStateException("Interview not found: " + interviewId));

        interview.startQuestionGeneration();
        interviewRepository.flush();

        List<GeneratedQuestion> generatedQuestions = aiClient.generateQuestions(
                position, positionDetail, level, interviewTypes,
                csSubTopics, resumeText, durationMinutes);

        List<QuestionSet> questionSets = createQuestionSets(interview, generatedQuestions);
        questionSetRepository.saveAll(questionSets);

        interview.completeQuestionGeneration();

        log.info("질문 생성 완료: interviewId={}, questionSets={}", interviewId, questionSets.size());
    }

    private List<QuestionSet> createQuestionSets(Interview interview, List<GeneratedQuestion> generatedQuestions) {
        List<QuestionSet> questionSets = new ArrayList<>();

        for (int i = 0; i < generatedQuestions.size(); i++) {
            GeneratedQuestion gq = generatedQuestions.get(i);

            QuestionCategory category = parseQuestionCategory(gq.getQuestionCategory());
            ReferenceType refType = parseReferenceType(gq.getReferenceType());

            QuestionSet questionSet = QuestionSet.builder()
                    .interview(interview)
                    .category(category)
                    .orderIndex(i)
                    .build();

            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText(gq.getContent())
                    .modelAnswer(gq.getModelAnswer())
                    .referenceType(refType)
                    .orderIndex(0)
                    .build();

            questionSet.addQuestion(question);
            questionSets.add(questionSet);
        }

        return questionSets;
    }

    private QuestionCategory parseQuestionCategory(String categoryStr) {
        if (categoryStr != null) {
            try {
                return QuestionCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return QuestionCategory.CS;
    }

    private ReferenceType parseReferenceType(String refTypeStr) {
        if (refTypeStr != null) {
            try {
                return ReferenceType.valueOf(refTypeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ReferenceType.GUIDE;
    }
}
