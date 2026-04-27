package com.rehearse.api.infra.ai.prompt;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class ResumeExtractorPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/resume/resume-extractor.txt";

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
        log.info("Resume Extractor 프롬프트 템플릿 로드 완료");
    }

    public String buildSystemPrompt() {
        return systemPromptTemplate;
    }

    public String buildUserPrompt(String normalizedResumeText) {
        return "<<<RESUME_TEXT>>>\n"
                + (normalizedResumeText != null ? normalizedResumeText : "(없음)")
                + "\n<<<END_RESUME_TEXT>>>\n"
                + "\n위 이력서를 분석해 JSON 한 객체로만 응답하세요.";
    }
}
