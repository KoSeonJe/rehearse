package com.rehearse.api.infra.ai.adapter;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.models.service.ResumeQuestionGenerator;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.OpenAiResponsesOutputTextExtractor;
import com.rehearse.api.infra.ai.client.OpenAiResumeQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder;
import com.rehearse.api.infra.ai.schema.GeneratedResumeQuestionsSchema;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.stereotype.Component;

import java.util.Base64;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class OpenAiResumeQuestionGenerator implements ResumeQuestionGenerator {

    private static final String CALL_TYPE = "resume_question_generator_v2";

    private final OpenAiResumeQuestionGeneratorClient client;
    private final OpenAiResponsesOutputTextExtractor outputTextExtractor;
    private final AiResponseParser aiResponseParser;
    private final ResumeQuestionPromptBuilder promptBuilder;

    @Override
    public GeneratedResumeQuestions generate(
            byte[] pdfBytes, int openerCount, int mainCount, Position position, TechStack techStack) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            log.error("[{}] PDF bytes 비어있음", CALL_TYPE);
            throw new BusinessException(AiErrorCode.CLIENT_ERROR);
        }

        long startedAt = System.currentTimeMillis();
        String systemPrompt = promptBuilder.buildSystemPrompt(position, techStack);
        String userInstruction = buildUserInstruction(openerCount, mainCount);
        String base64Pdf = Base64.getEncoder().encodeToString(pdfBytes);

        log.info("[{}] LLM 호출 시작: pdfBytes={}, openerCount={}, mainCount={}, position={}, techStack={}",
                CALL_TYPE, pdfBytes.length, openerCount, mainCount, position, techStack);

        String responseBody = client.call(
                systemPrompt, userInstruction, base64Pdf, GeneratedResumeQuestionsSchema.spec());
        String outputText = outputTextExtractor.extract(responseBody);
        GeneratedResumeQuestions parsed = aiResponseParser.parseJsonResponse(outputText, GeneratedResumeQuestions.class);

        log.info("[{}] LLM 호출 완료: openers={}, mains={}, elapsedMs={}",
                CALL_TYPE, parsed.openers().size(), parsed.mains().size(),
                System.currentTimeMillis() - startedAt);
        return parsed;
    }

    private String buildUserInstruction(int openerCount, int mainCount) {
        return "첨부된 이력서 PDF 를 직접 분석하여 opener "
                + openerCount + "개 + main " + mainCount
                + "개 질문을 JSON 한 객체로 응답하세요.\n"
                + "OPENER_COUNT: " + openerCount + "\n"
                + "MAIN_COUNT: " + mainCount;
    }
}
