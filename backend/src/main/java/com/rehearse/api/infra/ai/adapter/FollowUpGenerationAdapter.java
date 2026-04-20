package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

/**
 * FollowUpGenerationRequest → ChatRequest 변환 후 AiClient.chat() 경유로 후속 질문 생성.
 *
 * <p>텍스트 경로와 오디오 경로(STT 포함) 모두 지원한다.</p>
 */
@Component
@RequiredArgsConstructor
public class FollowUpGenerationAdapter {

    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;
    private static final String FOLLOW_UP_MODEL = "claude-haiku-4-5-20251001";

    private final FollowUpPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    /**
     * 텍스트 기반 후속 질문 생성.
     */
    public GeneratedFollowUp adapt(AiClient client, FollowUpGenerationRequest req) {
        String systemPrompt = promptBuilder.buildSystemPrompt(req);
        String userPrompt = promptBuilder.buildUserPrompt(req);

        ChatRequest chatRequest = buildChatRequest(systemPrompt, userPrompt);
        ChatResponse response = client.chat(chatRequest);
        return responseParser.parseOrRetry(response, GeneratedFollowUp.class, client, chatRequest);
    }

    /**
     * 오디오 기반 후속 질문 생성 (STT → 텍스트 변환 후 chat() 경유).
     *
     * @param sttService STT 서비스 (null 이면 오디오 전사 불가)
     */
    public GeneratedFollowUp adaptWithAudio(
            AiClient client,
            MultipartFile audioFile,
            FollowUpGenerationRequest req,
            SttService sttService) {

        String answerText;
        if (sttService != null) {
            answerText = sttService.transcribe(audioFile);
        } else {
            // STT 서비스 없이 오디오 전용 프롬프트 사용 (OpenAI audio 모델 경로 등)
            answerText = "[첨부된 오디오를 전사하여 사용하세요]";
        }

        FollowUpGenerationRequest updatedReq = new FollowUpGenerationRequest(
                req.position(), req.techStack(), req.level(),
                req.questionContent(), answerText,
                req.nonVerbalSummary(), req.previousExchanges(),
                req.mainReferenceType()
        );

        String systemPrompt = promptBuilder.buildSystemPrompt(updatedReq);
        String userPrompt = (sttService != null)
                ? promptBuilder.buildUserPrompt(updatedReq)
                : promptBuilder.buildUserPromptForAudio(req);

        ChatRequest chatRequest = buildChatRequest(systemPrompt, userPrompt);
        ChatResponse response = client.chat(chatRequest);
        GeneratedFollowUp result = responseParser.parseOrRetry(
                response, GeneratedFollowUp.class, client, chatRequest);

        return (sttService != null) ? result.withAnswerText(answerText) : result;
    }

    private ChatRequest buildChatRequest(String systemPrompt, String userPrompt) {
        return ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.SYSTEM, systemPrompt),
                        ChatMessage.of(ChatMessage.Role.USER, userPrompt)
                ))
                .modelOverride(FOLLOW_UP_MODEL)
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType("generate_followup")
                .build();
    }
}
