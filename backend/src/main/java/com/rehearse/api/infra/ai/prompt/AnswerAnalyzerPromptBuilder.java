package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.question.entity.ReferenceType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class AnswerAnalyzerPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/follow-up-step-a-analyzer.txt";

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
        log.info("Answer Analyzer 프롬프트 템플릿 로드 완료");
    }

    public String buildSystemPrompt() {
        return systemPromptTemplate;
    }

    public String buildUserPrompt(
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<<<MAIN_QUESTION>>>\n")
          .append(mainQuestion != null ? mainQuestion : "(없음)")
          .append("\n<<<END_MAIN_QUESTION>>>\n");
        sb.append("QUESTION_REFERENCE_TYPE: ").append(PromptFormatters.toReferenceLabel(questionReferenceType)).append("\n");
        sb.append("<<<USER_ANSWER>>>\n")
          .append(userAnswer != null ? userAnswer : "(없음)")
          .append("\n<<<END_USER_ANSWER>>>\n");
        sb.append("\n위 답변을 분석해 JSON 한 객체로만 응답하세요.");
        return sb.toString();
    }
}
