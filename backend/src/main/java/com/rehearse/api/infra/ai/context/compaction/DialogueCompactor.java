package com.rehearse.api.infra.ai.context.compaction;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.dto.ChatMessage;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import com.rehearse.api.infra.ai.dto.ResponseFormat;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class DialogueCompactor {

    private static final String TEMPLATE_PATH = "/prompts/template/compaction-summarizer.txt";
    private static final String CALL_TYPE = "compaction_summarizer";
    private static final double TEMPERATURE = 0.3;
    private static final int MAX_TOKENS = 800;

    private final AiClient aiClient;
    private final AiResponseParser aiResponseParser;
    private final ObjectMapper objectMapper;
    private final ContextEngineeringMetrics contextMetrics;

    private String systemPromptTemplate;

    @PostConstruct
    void init() {
        try (InputStream stream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 파일을 찾을 수 없습니다.");
            }
            systemPromptTemplate = new String(stream.readAllBytes());
        } catch (IOException e) {
            throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 로드 실패", e);
        }
        log.info("Compaction Summarizer 프롬프트 템플릿 로드 완료");
    }

    @Async("compactionExecutor")
    public void compactAsync(Long interviewId, int windowEnd,
                             List<FollowUpExchange> windowed,
                             InterviewRuntimeState state) {
        if (!state.tryStartCompaction(windowEnd)) {
            log.debug("compaction already in-flight for interviewId={}, windowEnd={}", interviewId, windowEnd);
            return;
        }

        // TODO when sync fallback added (deferred from Task 3), record "sync_fallback"
        contextMetrics.recordCompaction("async");
        try {
            String userPrompt = buildUserPrompt(windowed);
            ChatRequest chatRequest = ChatRequest.builder()
                    .messages(List.of(
                            ChatMessage.ofCached(ChatMessage.Role.SYSTEM, systemPromptTemplate),
                            ChatMessage.of(ChatMessage.Role.USER, userPrompt)
                    ))
                    .temperature(TEMPERATURE)
                    .maxTokens(MAX_TOKENS)
                    .responseFormat(ResponseFormat.JSON_OBJECT)
                    .callType(CALL_TYPE)
                    .build();

            ChatResponse response = aiClient.chat(chatRequest);
            CompactionSummaryResult result = aiResponseParser.parseOrRetry(
                    response, CompactionSummaryResult.class, aiClient, chatRequest);

            String summary = result.toCompactString();
            state.putCompactedSummary(windowEnd, summary);
            log.info("compaction completed: interviewId={}, windowEnd={}, summaryLength={}",
                    interviewId, windowEnd, summary.length());
        } catch (Exception e) {
            log.warn("compaction failed: interviewId={}, windowEnd={}, reason={}", interviewId, windowEnd, e.getMessage());
        } finally {
            state.markCompactionFinished(windowEnd);
        }
    }

    private String buildUserPrompt(List<FollowUpExchange> exchanges) {
        List<Map<String, Object>> turns = new ArrayList<>(exchanges.size());
        for (int i = 0; i < exchanges.size(); i++) {
            FollowUpExchange ex = exchanges.get(i);
            turns.add(Map.of(
                    "turn", i + 1,
                    "q", ex.getQuestion() != null ? ex.getQuestion() : "",
                    "a", ex.getAnswer() != null ? ex.getAnswer() : ""
            ));
        }
        try {
            return "<<<DIALOGUE>>>\n" + objectMapper.writeValueAsString(turns) + "\n<<<END_DIALOGUE>>>";
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("대화 직렬화 실패", e);
        }
    }
}
