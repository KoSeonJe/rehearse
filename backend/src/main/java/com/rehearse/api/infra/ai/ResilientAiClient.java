package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.context.annotation.Primary;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * Primary AiClient — OpenAI(GPT-4o-mini) 호출 후 실패 시 Claude fallback.
 *
 * <p>서비스 계층은 AiClient 인터페이스만 의존하므로 변경 없음.</p>
 *
 * <p>빈 생성 조건: OpenAiClient 또는 ClaudeApiClient 중 하나 이상 존재.
 * 둘 다 없으면 MockAiClient가 활성화됨.</p>
 *
 * <p>Fallback 전략:
 * <ul>
 *   <li>OpenAI만 있음: OpenAI 호출, 실패 시 에러</li>
 *   <li>Claude만 있음: Claude 호출</li>
 *   <li>둘 다 있음: OpenAI 호출 (1회 재시도, 총 2회) → 실패 시 Claude fallback</li>
 *   <li>Claude도 실패 → SERVICE_UNAVAILABLE (503)</li>
 * </ul>
 */
@Slf4j
@Component
@Primary
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty() or !'${claude.api-key:}'.isEmpty()")
public class ResilientAiClient implements AiClient {

    @Nullable
    private final OpenAiClient openAiClient;

    @Nullable
    private final ClaudeApiClient claudeApiClient;

    @Nullable
    private final SttService sttService;

    public ResilientAiClient(
            @Nullable OpenAiClient openAiClient,
            @Nullable ClaudeApiClient claudeApiClient,
            @Nullable SttService sttService) {
        this.openAiClient = openAiClient;
        this.claudeApiClient = claudeApiClient;
        this.sttService = sttService;

        if (openAiClient == null && claudeApiClient == null) {
            throw new IllegalStateException("OpenAiClient와 ClaudeApiClient 중 하나 이상 설정되어야 합니다.");
        }

        if (openAiClient != null && claudeApiClient != null) {
            log.info("[ResilientAiClient] Primary: OpenAI, Fallback: Claude");
        } else if (openAiClient != null) {
            log.info("[ResilientAiClient] OpenAI only (fallback 없음)");
        } else {
            log.info("[ResilientAiClient] Claude only (OpenAI 미설정)");
        }
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        if (openAiClient == null) {
            return fallbackGenerateQuestions(request);
        }

        try {
            return openAiClient.generateQuestions(request);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) {
                throw e;
            }
            log.warn("[AI Fallback] OpenAI 질문 생성 실패 → Claude 전환: {}", e.getMessage());
            return fallbackGenerateQuestions(request);
        } catch (Exception e) {
            log.warn("[AI Fallback] OpenAI 질문 생성 실패 → Claude 전환: {}", e.getMessage());
            return fallbackGenerateQuestions(request);
        }
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        if (openAiClient == null) {
            return fallbackGenerateFollowUp(request);
        }

        try {
            return openAiClient.generateFollowUpQuestion(request);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) {
                throw e;
            }
            log.warn("[AI Fallback] OpenAI 후속 질문 실패 → Claude 전환: {}", e.getMessage());
            return fallbackGenerateFollowUp(request);
        } catch (Exception e) {
            log.warn("[AI Fallback] OpenAI 후속 질문 실패 → Claude 전환: {}", e.getMessage());
            return fallbackGenerateFollowUp(request);
        }
    }

    @Override
    public GeneratedFollowUp generateFollowUpWithAudio(MultipartFile audioFile, FollowUpGenerationRequest request) {
        if (openAiClient == null) {
            return fallbackWithSttAndClaude(audioFile, request);
        }

        try {
            return openAiClient.generateFollowUpWithAudio(audioFile, request);
        } catch (BusinessException e) {
            if (isNonRetryableError(e)) {
                throw e;
            }
            log.warn("[AI Fallback] GPT-audio 실패 → Whisper+Claude 전환: {}", e.getMessage());
            return fallbackWithSttAndClaude(audioFile, request);
        } catch (Exception e) {
            log.warn("[AI Fallback] GPT-audio 실패 → Whisper+Claude 전환: {}", e.getMessage());
            return fallbackWithSttAndClaude(audioFile, request);
        }
    }

    private GeneratedFollowUp fallbackWithSttAndClaude(MultipartFile audioFile, FollowUpGenerationRequest request) {
        if (sttService == null || claudeApiClient == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            String answerText = sttService.transcribe(audioFile);
            FollowUpGenerationRequest updatedReq = new FollowUpGenerationRequest(
                    request.position(), request.techStack(), request.level(),
                    request.questionContent(), answerText,
                    request.nonVerbalSummary(), request.previousExchanges()
            );
            return claudeApiClient.generateFollowUpQuestion(updatedReq).withAnswerText(answerText);
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Whisper+Claude도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    /**
     * 요청 자체의 문제(CLIENT_ERROR, PARSE_FAILED)는 Claude로 보내도 동일하게 실패하므로 fallback하지 않는다.
     */
    private boolean isNonRetryableError(BusinessException e) {
        return AiErrorCode.CLIENT_ERROR.getCode().equals(e.getCode())
                || AiErrorCode.PARSE_FAILED.getCode().equals(e.getCode());
    }

    private List<GeneratedQuestion> fallbackGenerateQuestions(QuestionGenerationRequest request) {
        if (claudeApiClient == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return claudeApiClient.generateQuestions(request);
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Claude도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }

    private GeneratedFollowUp fallbackGenerateFollowUp(FollowUpGenerationRequest request) {
        if (claudeApiClient == null) {
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        try {
            return claudeApiClient.generateFollowUpQuestion(request);
        } catch (Exception fallbackEx) {
            log.error("[AI Fallback] Claude도 실패 — 이중 장애: {}", fallbackEx.getMessage());
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
    }
}
