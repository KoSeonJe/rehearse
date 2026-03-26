package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.entity.QuestionGenerationStatus;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.domain.questionset.service.QuestionSetService;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.ArrayList;
import java.util.Collections;
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
    private final QuestionSetService questionSetService;
    private final AiClient aiClient;
    private final PdfTextExtractor pdfTextExtractor;
    private final SttService sttService;
    private final ApplicationEventPublisher eventPublisher;
    private final FollowUpTransactionHandler followUpTransactionHandler;

    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request, MultipartFile resumeFile) {
        String resumeText = null;
        if (resumeFile != null && !resumeFile.isEmpty()) {
            resumeText = pdfTextExtractor.extract(resumeFile);
        }

        if (request.getTechStack() != null && !request.getTechStack().isAllowedFor(request.getPosition())) {
            throw new BusinessException(InterviewErrorCode.INVALID_TECH_STACK);
        }

        Interview interview = Interview.builder()
                .position(request.getPosition())
                .positionDetail(request.getPositionDetail())
                .level(request.getLevel())
                .interviewTypes(request.getInterviewTypes())
                .csSubTopics(request.getCsSubTopics())
                .durationMinutes(request.getDurationMinutes())
                .techStack(request.getTechStack())
                .build();

        Interview saved = interviewRepository.save(interview);

        eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(
                saved.getId(),
                request.getPosition(),
                request.getPositionDetail(),
                request.getLevel(),
                request.getInterviewTypes(),
                request.getCsSubTopics(),
                resumeText,
                request.getDurationMinutes(),
                request.getTechStack()
        ));

        log.info("면접 세션 생성 완료 (질문 생성 이벤트 발행): id={}, position={}, level={}, types={}",
                saved.getId(), saved.getPosition(), saved.getLevel(), saved.getInterviewTypes());

        return InterviewResponse.from(saved, Collections.emptyList());
    }

    public InterviewResponse getInterview(Long id) {
        Interview interview = interviewFinder.findById(id);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    public InterviewResponse getInterviewByPublicId(String publicId) {
        Interview interview = interviewFinder.findByPublicId(publicId);
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(interview.getId());
        return InterviewResponse.from(interview, questionSets);
    }

    @Transactional
    public UpdateStatusResponse updateStatus(Long id, UpdateStatusRequest request) {
        Interview interview = interviewFinder.findById(id);

        if (request.getStatus() == InterviewStatus.IN_PROGRESS
                && interview.getQuestionGenerationStatus() != QuestionGenerationStatus.COMPLETED) {
            throw new BusinessException(InterviewErrorCode.QUESTION_GENERATION_NOT_COMPLETED);
        }

        try {
            interview.updateStatus(request.getStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(InterviewErrorCode.INVALID_STATUS_TRANSITION);
        }

        log.info("면접 세션 상태 변경: id={}, newStatus={}", id, request.getStatus());

        return UpdateStatusResponse.from(interview);
    }

    @Transactional
    public InterviewResponse retryQuestionGeneration(Long id) {
        Interview interview = interviewFinder.findById(id);

        if (interview.getQuestionGenerationStatus() != QuestionGenerationStatus.FAILED) {
            throw new BusinessException(InterviewErrorCode.QUESTION_GENERATION_NOT_FAILED);
        }

        interview.resetForRetry();

        eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(
                interview.getId(),
                interview.getPosition(),
                interview.getPositionDetail(),
                interview.getLevel(),
                new ArrayList<>(interview.getInterviewTypes()),
                new ArrayList<>(interview.getCsSubTopics()),
                null,
                interview.getDurationMinutes(),
                interview.getTechStack()
        ));

        log.info("질문 생성 재시도 이벤트 발행: id={}", id);

        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdWithQuestions(id);
        return InterviewResponse.from(interview, questionSets);
    }

    @Transactional(propagation = org.springframework.transaction.annotation.Propagation.NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request, MultipartFile audioFile) {
        // Phase 1: answerText 결정 (트랜잭션 없음 — 외부 API 호출 가능)
        String answerText = resolveAnswerText(request, audioFile);

        // Phase 2: DB 조회 + 검증 (짧은 readOnly 트랜잭션)
        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, request.getQuestionSetId());

        // Phase 3: AI API 호출 (트랜잭션 없음)
        FollowUpGenerationRequest followUpReq = new FollowUpGenerationRequest(
                context.position(),
                context.effectiveTechStack(),
                context.level(),
                request.getQuestionContent(),
                answerText,
                request.getNonVerbalSummary(),
                request.getPreviousExchanges()
        );
        GeneratedFollowUp followUp = aiClient.generateFollowUpQuestion(followUpReq);

        // Phase 4: 결과 저장 (짧은 write 트랜잭션)
        Question savedQuestion = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), followUp, context.nextOrderIndex());

        log.info("REALTIME 후속 질문 생성 완료: interviewId={}, questionSetId={}, questionId={}, type={}",
                id, request.getQuestionSetId(), savedQuestion.getId(), followUp.getType());

        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.getQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .answerText(answerText)
                .modelAnswer(savedQuestion.getModelAnswer())
                .build();
    }

    private String resolveAnswerText(FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile != null && !audioFile.isEmpty()) {
            String text = sttService.transcribe(audioFile);
            if (text == null || text.isBlank()) {
                throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
            }
            return text;
        }
        if (request.getAnswerText() != null && !request.getAnswerText().isBlank()) {
            return request.getAnswerText();
        }
        throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
    }

    @Transactional
    public void skipRemainingQuestionSets(Long id) {
        Interview interview = interviewFinder.findById(id);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(InterviewErrorCode.NOT_IN_PROGRESS);
        }

        questionSetService.skipRemaining(id);

        log.info("미응답 질문세트 스킵 처리: interviewId={}", id);
    }
}
