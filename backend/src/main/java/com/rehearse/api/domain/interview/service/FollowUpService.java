package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.global.config.IntentClassifierProperties;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpService {

    private final AiClient aiClient;
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final IntentClassifier intentClassifier;
    private final IntentClassifierProperties intentClassifierProperties;
    private final OffTopicResponseHandler offTopicResponseHandler;
    private final ClarifyResponseHandler clarifyResponseHandler;
    private final GiveUpResponseHandler giveUpResponseHandler;
    private final OffTopicEscalationDetector offTopicEscalationDetector;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, Long userId, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, userId, request.getQuestionSetId());

        FollowUpGenerationRequest followUpReq = new FollowUpGenerationRequest(
                context.position(), context.effectiveTechStack(), context.level(),
                request.getQuestionContent(), null, request.getNonVerbalSummary(),
                request.getPreviousExchanges(), context.mainReferenceType());
        GeneratedFollowUp followUp = aiClient.generateFollowUpWithAudio(audioFile, followUpReq);

        if (followUp.isSkipped()) {
            log.info("후속 질문 스킵: interviewId={}, questionSetId={}, reason={}",
                    id, request.getQuestionSetId(), followUp.getSkipReason());
            return handleAiSkip(followUp);
        }

        String answerText = followUp.getAnswerText();
        IntentResult intent = intentClassifier.classify(
                request.getQuestionContent(), answerText, request.getPreviousExchanges());

        Optional<FollowUpResponse> intentResponse = branchByIntent(
                intent, id, context, request, answerText);
        if (intentResponse.isPresent()) {
            return intentResponse.get();
        }

        // skip=false인데 question null이면 DB NOT NULL 위반 방지
        if (followUp.getQuestion() == null || followUp.getQuestion().isBlank()) {
            log.warn("AI 응답 스키마 위반: skip=false인데 question이 비어있음. interviewId={}, questionSetId={}",
                    id, request.getQuestionSetId());
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        Question savedQuestion = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), followUp, context.nextOrderIndex());

        log.info("REALTIME 후속 질문 생성 완료: interviewId={}, questionSetId={}, questionId={}, type={}",
                id, request.getQuestionSetId(), savedQuestion.getId(), followUp.getType());

        return buildAnswerResponse(followUp, savedQuestion);
    }

    private FollowUpResponse handleAiSkip(GeneratedFollowUp followUp) {
        return FollowUpResponse.builder()
                .answerText(followUp.getAnswerText())
                .skip(true)
                .skipReason(followUp.getSkipReason())
                .presentToUser(false)
                .build();
    }

    private Optional<FollowUpResponse> branchByIntent(
            IntentResult intent, Long interviewId,
            FollowUpContext context, FollowUpRequest request, String answerText) {
        return switch (intent.type()) {
            case OFF_TOPIC -> {
                int consecutive = offTopicEscalationDetector.countRecentConsecutive(request.getPreviousExchanges());
                if (offTopicEscalationDetector.shouldEscalate(consecutive, intentClassifierProperties.offTopicConsecutiveLimit())) {
                    log.info("OFF_TOPIC 연속 {}회 → GIVE_UP escalation", consecutive + 1);
                    yield Optional.of(giveUpResponseHandler.handle(context, request.getQuestionContent(), answerText));
                }
                int turnIndex = request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
                yield Optional.of(offTopicResponseHandler.handle(interviewId, turnIndex, request.getQuestionContent(), answerText));
            }
            case CLARIFY_REQUEST ->
                    Optional.of(clarifyResponseHandler.handle(context, request.getQuestionContent(), answerText));
            case GIVE_UP ->
                    Optional.of(giveUpResponseHandler.handle(context, request.getQuestionContent(), answerText));
            case ANSWER -> Optional.empty();
        };
    }

    private FollowUpResponse buildAnswerResponse(GeneratedFollowUp followUp, Question savedQuestion) {
        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.getQuestion())
                .ttsQuestion(followUp.getTtsQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .answerText(followUp.getAnswerText())
                .modelAnswer(savedQuestion.getModelAnswer())
                .skip(false)
                .presentToUser(true)
                .build();
    }
}
