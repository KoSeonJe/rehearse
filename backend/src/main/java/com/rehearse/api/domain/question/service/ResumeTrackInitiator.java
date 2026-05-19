package com.rehearse.api.domain.question.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeIngestionService;
import com.rehearse.api.domain.resume.service.ResumeQuestionPersister;
import com.rehearse.api.domain.resume.service.ResumeSkeletonSampler;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.BuiltContext;
import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.FocusHints;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeTrackInitiator {

    private static final String CALL_TYPE = "resume_question_generator";
    private static final double TEMPERATURE = 0.8;
    private static final int MAX_TOKENS = 14800;
    private static final int OPENER_COUNT = 1;
    private static final int MIN_MAIN_COUNT = 7;
    private static final int MAX_MAIN_COUNT = 40;
    private static final int DEFAULT_DURATION_MINUTES = 30;

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final ResumeIngestionService resumeIngestionService;
    private final ResumeQuestionPersister resumeQuestionPersister;
    private final ResumeSkeletonSampler resumeSkeletonSampler;
    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final InterviewContextBuilder contextBuilder;
    private final ObjectMapper objectMapper;

    public void initiate(Long interviewId, String resumeFileHash, byte[] resumePdfBytes, Integer durationMinutes) {
        try {
            ResumeSkeleton skeleton = resumeIngestionService.ingestPdf(interviewId, resumePdfBytes, resumeFileHash);

            int minutes = durationMinutes != null ? durationMinutes : DEFAULT_DURATION_MINUTES;
            int mainCount = Math.max(MIN_MAIN_COUNT, Math.min(MAX_MAIN_COUNT, minutes / 3 + 2));

            GeneratedResumeQuestions generated = generateViaLlm(interviewId, skeleton, OPENER_COUNT, mainCount);
            persistGenerated(interviewId, generated);
            transactionHandler.completeGeneration(interviewId);
        } catch (Exception e) {
            log.error("[ResumeTrackInitiator] 면접 시작 실패: interviewId={}, reason={}",
                    interviewId, e.getMessage());
            transactionHandler.failGeneration(interviewId, "이력서 트랙 시작 실패: " + e.getMessage());
            throw e;
        }
    }

    private GeneratedResumeQuestions generateViaLlm(Long interviewId, ResumeSkeleton skeleton, int openerCount, int mainCount) {
        ResumeSkeleton sampledSkeleton = resumeSkeletonSampler.sampleDecisions(skeleton, interviewId);
        String skeletonJson = serializeSkeleton(sampledSkeleton);
        String primaryProjectName = sampledSkeleton.projects().isEmpty()
                ? null
                : sampledSkeleton.projects().get(0).projectName();
        BuiltContext built = contextBuilder.build(new ContextBuildRequest(
                CALL_TYPE,
                new FocusHints.ResumeQuestionGeneratorHints(skeletonJson, openerCount, mainCount, primaryProjectName),
                null
        ));
        ChatRequest request = ChatRequest.builder()
                .messages(built.messages())
                .callType(CALL_TYPE)
                .temperature(TEMPERATURE)
                .maxTokens(MAX_TOKENS)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .build();
        ChatResponse response = aiClient.chat(request);
        return aiResponseParser.parseOrRetry(response, GeneratedResumeQuestions.class, aiClient, request);
    }

    private void persistGenerated(Long interviewId, GeneratedResumeQuestions generated) {
        List<ResumeQuestionPersister.ResumeQuestionDraft> drafts = new ArrayList<>(
                generated.openers().size() + generated.mains().size());
        int order = 0;
        for (var opener : generated.openers()) {
            drafts.add(new ResumeQuestionPersister.ResumeQuestionDraft(
                    QuestionType.RESUME_OPENER, opener.question(), opener.ttsQuestion(),
                    opener.bestAnswer(), order++));
        }
        for (var main : generated.mains()) {
            drafts.add(new ResumeQuestionPersister.ResumeQuestionDraft(
                    QuestionType.RESUME_MAIN, main.question(), main.ttsQuestion(),
                    main.bestAnswer(), order++));
        }
        resumeQuestionPersister.persistAll(interviewId, drafts);
        log.info("[ResumeTrackInitiator] 질문 적재 완료: interviewId={}, openers={}, mains={}",
                interviewId, generated.openers().size(), generated.mains().size());
    }

    private String serializeSkeleton(ResumeSkeleton skeleton) {
        try {
            return objectMapper.writeValueAsString(skeleton);
        } catch (JsonProcessingException e) {
            log.error("[ResumeTrackInitiator] skeleton 직렬화 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }
}
