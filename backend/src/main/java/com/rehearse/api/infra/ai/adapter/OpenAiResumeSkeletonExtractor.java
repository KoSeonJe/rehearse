package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.OpenAiResponsesOutputTextExtractor;
import com.rehearse.api.infra.ai.client.OpenAiResumeExtractorClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.StreamUtils;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.Base64;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiResumeSkeletonExtractor implements ResumeSkeletonExtractor {

    private static final String SYSTEM_PROMPT_PATH = "prompts/template/resume/resume-extractor.txt";

    private final OpenAiResumeExtractorClient client;
    private final OpenAiResponsesOutputTextExtractor outputTextExtractor;
    private final AiResponseParser aiResponseParser;

    private String systemPrompt;

    public OpenAiResumeSkeletonExtractor(
            OpenAiResumeExtractorClient client,
            OpenAiResponsesOutputTextExtractor outputTextExtractor,
            AiResponseParser aiResponseParser) {
        this.client = client;
        this.outputTextExtractor = outputTextExtractor;
        this.aiResponseParser = aiResponseParser;
    }

    @PostConstruct
    void init() {
        try {
            systemPrompt = StreamUtils.copyToString(
                    new ClassPathResource(SYSTEM_PROMPT_PATH).getInputStream(),
                    StandardCharsets.UTF_8);
        } catch (IOException e) {
            throw new IllegalStateException("resume-extractor 프롬프트 로드 실패", e);
        }
    }

    @Override
    public GeneratedResumeSkeleton extract(byte[] pdfBytes, String fileHash) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);
        String responseBody = client.call(systemPrompt, base64Pdf);
        String outputText = outputTextExtractor.extract(responseBody);
        return aiResponseParser.parseJsonResponse(outputText, GeneratedResumeSkeleton.class);
    }
}
