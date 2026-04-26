package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.dto.FollowUpSaveResult;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.vo.IntentResult;
import com.rehearse.api.domain.interview.vo.IntentType;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.AnswerAnalysisJsonRenderer;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
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
import java.util.Objects;
import java.util.Optional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpService {

    private static final String STEP_B_CALL_TYPE = "follow_up_generator_v3";
    private static final double STEP_B_TEMPERATURE = 0.6;
    private static final int STEP_B_MAX_TOKENS = 1024;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final AnswerAnalyzer answerAnalyzer;
    private final FollowUpTransactionHandler followUpTransactionHandler;
    private final IntentClassifier intentClassifier;
    private final List<IntentResponseHandler> intentResponseHandlers;
    private final InterviewContextBuilder contextBuilder;
    private final InterviewRuntimeStateStore runtimeStateStore;

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
        runtimeStateStore.getOrInit(id, () -> new InterviewRuntimeState(context.level().name(), null));

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

        List<Perspective> askedPerspectives = extractAskedPerspectives(request.getPreviousExchanges());
        AnswerAnalysis analysis = answerAnalyzer.analyze(
                id,
                resolveTurnId(context),
                request.getQuestionContent(),
                context.mainReferenceType(),
                answerText,
                askedPerspectives
        );

        if (analysis.recommendedNextAction() == RecommendedNextAction.SKIP) {
            log.info("Step A 가 SKIP 권고 → Step B 미호출. interviewId={}, questionSetId={}", id, request.getQuestionSetId());
            return FollowUpResponse.aiSkip(answerText, "analyzer_recommend_skip");
        }

        FollowUpGenerationRequest stepBReq = new FollowUpGenerationRequest(
                context.position(), context.effectiveTechStack(), context.level(),
                request.getQuestionContent(), answerText, request.getNonVerbalSummary(),
                request.getPreviousExchanges(), context.mainReferenceType());
        GeneratedFollowUp stepBFollowUp = generateStepBFollowUp(stepBReq, analysis, askedPerspectives);

        if (stepBFollowUp.isSkipped()) {
            log.info("Step B 가 skip 반환: interviewId={}, questionSetId={}, reason={}",
                    id, request.getQuestionSetId(), stepBFollowUp.getSkipReason());
            return FollowUpResponse.aiSkip(answerText, stepBFollowUp.getSkipReason());
        }

        if (stepBFollowUp.getQuestion() == null || stepBFollowUp.getQuestion().isBlank()) {
            log.warn("Step B 응답 스키마 위반: skip=false인데 question이 비어있음. interviewId={}, questionSetId={}",
                    id, request.getQuestionSetId());
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }

        FollowUpSaveResult saveResult = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), stepBFollowUp, context.nextOrderIndex());
        boolean exhausted = saveResult.newFollowUpCount() >= context.maxFollowUpRounds();

        log.info("REALTIME 후속 질문 생성 완료(v3): interviewId={}, questionSetId={}, questionId={}, type={}, perspective={}, targetClaim={}, exhausted={}",
                id, request.getQuestionSetId(), saveResult.question().getId(),
                stepBFollowUp.getType(), stepBFollowUp.getSelectedPerspective(),
                stepBFollowUp.getTargetClaimIdx(), exhausted);

        return buildAnswerResponse(stepBFollowUp, saveResult.question(), exhausted);
    }

    private GeneratedFollowUp generateStepBFollowUp(
            FollowUpGenerationRequest req,
            AnswerAnalysis analysis,
            List<Perspective> askedPerspectives
    ) {
        String answerAnalysisJson = AnswerAnalysisJsonRenderer.render(analysis, askedPerspectives);

        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                STEP_B_CALL_TYPE,
                Map.of(),
                req.previousExchanges() != null ? req.previousExchanges() : List.of(),
                Map.of(
                        "answerAnalysisJson", answerAnalysisJson,
                        "askedPerspectives", askedPerspectives.stream()
                                .map(Enum::name)
                                .collect(java.util.stream.Collectors.joining(", "))
                ),
                null
        ));

        ChatRequest chatRequest = ChatRequest.builder()
                .messages(built.messages())
                .callType(STEP_B_CALL_TYPE)
                .temperature(STEP_B_TEMPERATURE)
                .maxTokens(STEP_B_MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();

        ChatResponse response = aiClient.chat(chatRequest);
        GeneratedFollowUp parsed = aiResponseParser.parseOrRetry(
                response, GeneratedFollowUp.class, aiClient, chatRequest);
        return parsed.withAnswerText(req.answerText());
    }

    private static Long resolveTurnId(FollowUpContext context) {
        if (context.currentMainQuestionId() != null) {
            return context.currentMainQuestionId();
        }
        return (long) context.nextOrderIndex();
    }

    private static List<Perspective> extractAskedPerspectives(List<FollowUpRequest.FollowUpExchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return List.of();
        }
        return exchanges.stream()
                .map(FollowUpRequest.FollowUpExchange::getSelectedPerspective)
                .filter(Objects::nonNull)
                .map(FollowUpService::safeParsePerspective)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList();
    }

    private static Optional<Perspective> safeParsePerspective(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Perspective.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
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
                .selectedPerspective(followUp.getSelectedPerspective())
                .build();
    }
}
