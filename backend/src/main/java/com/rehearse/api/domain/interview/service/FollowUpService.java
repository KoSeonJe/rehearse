package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpService {

    private final AiClient aiClient;
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final IntentClassifier intentClassifier;
    private final List<IntentResponseHandler> intentResponseHandlers;

    private final Map<IntentType, IntentResponseHandler> handlerByIntent = new EnumMap<>(IntentType.class);

    @PostConstruct
    void registerHandlers() {
        intentResponseHandlers.forEach(h -> handlerByIntent.put(h.supports(), h));
    }

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
            return FollowUpResponse.aiSkip(followUp.getAnswerText(), followUp.getSkipReason());
        }

        String answerText = followUp.getAnswerText();
        IntentResult intent = intentClassifier.classify(
                request.getQuestionContent(), answerText, request.getPreviousExchanges());

        if (intent.type() != IntentType.ANSWER) {
            return dispatchIntentBranch(intent.type(), id, context, request, answerText);
        }

        if (followUp.getQuestion() == null || followUp.getQuestion().isBlank()) {
            log.warn("AI 응답 스키마 위반: skip=false인데 question이 비어있음. interviewId={}, questionSetId={}",
                    id, request.getQuestionSetId());
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        FollowUpSaveResult saveResult = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), followUp, context.nextOrderIndex());
        boolean exhausted = saveResult.newFollowUpCount() >= context.maxFollowUpRounds();

        log.info("REALTIME 후속 질문 생성 완료: interviewId={}, questionSetId={}, questionId={}, type={}, exhausted={}",
                id, request.getQuestionSetId(), saveResult.question().getId(), followUp.getType(), exhausted);

        return buildAnswerResponse(followUp, saveResult.question(), exhausted);
    }

    private FollowUpResponse dispatchIntentBranch(
            IntentType intentType, Long interviewId,
            FollowUpContext context, FollowUpRequest request, String answerText) {
        IntentResponseHandler handler = handlerByIntent.get(intentType);
        if (handler == null) {
            log.warn("등록된 IntentResponseHandler 없음: intent={}, ANSWER 경로로 fallback", intentType);
            return null;
        }
        int turnIndex = request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
        IntentBranchInput input = new IntentBranchInput(
                interviewId, context, request.getQuestionContent(), answerText,
                turnIndex, request.getPreviousExchanges());
        return handler.handle(input);
    }

    private FollowUpResponse buildAnswerResponse(GeneratedFollowUp followUp, Question savedQuestion, boolean exhausted) {
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
                .followUpExhausted(exhausted)
                .build();
    }
}
