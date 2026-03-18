package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewQuestion;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final InterviewFinder interviewFinder;
    private final AiClient aiClient;
    private final PdfTextExtractor pdfTextExtractor;

    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request, MultipartFile resumeFile) {
        // 1. PDF에서 텍스트 추출 (resumeFile != null일 때)
        String resumeText = null;
        if (resumeFile != null && !resumeFile.isEmpty()) {
            resumeText = pdfTextExtractor.extract(resumeFile);
        }

        // 2. Interview 엔티티 생성 (resume 저장 안함)
        Interview interview = Interview.builder()
                .position(request.getPosition())
                .positionDetail(request.getPositionDetail())
                .level(request.getLevel())
                .interviewTypes(request.getInterviewTypes())
                .csSubTopics(request.getCsSubTopics())
                .durationMinutes(request.getDurationMinutes())
                .build();

        // 3. Claude API로 질문 생성 (resumeText는 프롬프트에만 사용, 1회성)
        List<GeneratedQuestion> generatedQuestions = aiClient.generateQuestions(
                request.getPosition(),
                request.getPositionDetail(),
                request.getLevel(),
                request.getInterviewTypes(),
                request.getCsSubTopics(),
                resumeText,
                request.getDurationMinutes()
        );

        // 4. 질문 엔티티 변환 및 연결 (레거시 InterviewQuestion 유지 + 새 QuestionSet 생성)
        for (GeneratedQuestion gq : generatedQuestions) {
            InterviewQuestion question = InterviewQuestion.builder()
                    .questionOrder(gq.getOrder())
                    .category(gq.getCategory())
                    .content(gq.getContent())
                    .evaluationCriteria(gq.getEvaluationCriteria())
                    .build();
            interview.addQuestion(question);
        }

        // 5. 저장
        Interview saved = interviewRepository.save(interview);

        // 6. QuestionSet + Question 생성 (질문세트 단위 녹화-분석용)
        List<QuestionSet> questionSets = createQuestionSets(saved, generatedQuestions);

        log.info("면접 세션 생성 완료: id={}, position={}, level={}, types={}, questionSets={}",
                saved.getId(), saved.getPosition(), saved.getLevel(), saved.getInterviewTypes(), questionSets.size());

        return InterviewResponse.from(saved, questionSets);
    }

    public InterviewResponse getInterview(Long id) {
        Interview interview = interviewFinder.findByIdWithQuestions(id);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    @Transactional
    public UpdateStatusResponse updateStatus(Long id, UpdateStatusRequest request) {
        Interview interview = interviewFinder.findByIdWithQuestions(id);

        try {
            interview.updateStatus(request.getStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(InterviewErrorCode.INVALID_STATUS_TRANSITION);
        }

        log.info("면접 세션 상태 변경: id={}, newStatus={}", id, request.getStatus());

        return UpdateStatusResponse.from(interview);
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

        return questionSetRepository.saveAll(questionSets);
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

    @Transactional
    public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request) {
        Interview interview = interviewFinder.findByIdWithQuestions(id);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        GeneratedFollowUp followUp = aiClient.generateFollowUpQuestion(
                request.getQuestionContent(),
                request.getAnswerText(),
                request.getNonVerbalSummary(),
                request.getPreviousExchanges()
        );

        // 후속질문을 Question 엔티티로 저장
        QuestionSet questionSet = questionSetRepository.findById(request.getQuestionSetId())
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));

        QuestionType followUpType = determineFollowUpType(questionSet);
        int nextOrderIndex = questionSet.getQuestions().size();

        Question followUpQuestion = Question.builder()
                .questionType(followUpType)
                .questionText(followUp.getQuestion())
                .modelAnswer(followUp.getModelAnswer())
                .orderIndex(nextOrderIndex)
                .build();

        questionSet.addQuestion(followUpQuestion);
        questionRepository.save(followUpQuestion);

        log.info("후속 질문 생성 및 저장 완료: interviewId={}, questionSetId={}, questionId={}, type={}",
                id, request.getQuestionSetId(), followUpQuestion.getId(), followUp.getType());

        return FollowUpResponse.builder()
                .questionId(followUpQuestion.getId())
                .question(followUp.getQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .build();
    }

    private static final int MAX_FOLLOWUP_ROUNDS = 3;

    private QuestionType determineFollowUpType(QuestionSet questionSet) {
        long followUpCount = questionSet.getQuestions().stream()
                .filter(q -> q.getQuestionType() == QuestionType.FOLLOWUP)
                .count();

        if (followUpCount >= MAX_FOLLOWUP_ROUNDS) {
            throw new BusinessException(QuestionSetErrorCode.MAX_FOLLOWUP_EXCEEDED);
        }
        return QuestionType.FOLLOWUP;
    }
}
