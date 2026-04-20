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

@Component
@RequiredArgsConstructor
public class FollowUpGenerationAdapter {

    private static final int MAX_TOKENS = 1024;
    private static final double TEMPERATURE = 0.7;

    private final FollowUpPromptBuilder promptBuilder;
    private final AiResponseParser responseParser;

    public GeneratedFollowUp adapt(AiClient client, FollowUpGenerationRequest req) {
        String systemPrompt = promptBuilder.buildSystemPrompt(req);
        String userPrompt = promptBuilder.buildUserPrompt(req);

        ChatRequest chatRequest = buildChatRequest(systemPrompt, userPrompt);
        ChatResponse response = client.chat(chatRequest);
        return responseParser.parseOrRetry(response, GeneratedFollowUp.class, client, chatRequest);
    }

    public GeneratedFollowUp adaptWithAudio(
            AiClient client,
            MultipartFile audioFile,
            FollowUpGenerationRequest req,
            SttService sttService) {

        String answerText;
        if (sttService != null) {
            answerText = sttService.transcribe(audioFile);
        } else {
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

    // modelOverride 지정하지 않음 — 각 provider 의 application.yml 기본 모델 사용.
    // (provider 별 호환 모델 ID 가 다르므로 여기서 강제하면 잘못된 경로에 잘못된 모델이 전달됨)
    private ChatRequest buildChatRequest(String systemPrompt, String userPrompt) {
        return ChatRequest.builder()
                .messages(List.of(
                        ChatMessage.of(ChatMessage.Role.SYSTEM, systemPrompt),
                        ChatMessage.of(ChatMessage.Role.USER, userPrompt)
                ))
                .maxTokens(MAX_TOKENS)
                .temperature(TEMPERATURE)
                .responseFormat(ResponseFormat.JSON_OBJECT)
                .callType("generate_followup")
                .build();
    }
}
