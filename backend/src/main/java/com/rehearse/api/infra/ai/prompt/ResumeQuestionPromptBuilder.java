package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ResolvedProfile;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeQuestionPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/resume/resume-question-generator.txt";

    private final PersonaResolver personaResolver;
    private String template;

    @PostConstruct
    void init() {
        try (var stream = getClass().getResourceAsStream(TEMPLATE_PATH)) {
            if (stream == null) {
                throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 파일을 찾을 수 없습니다.");
            }
            template = new String(stream.readAllBytes());
            log.info("Resume 질문 생성 프롬프트 템플릿 로드 완료");
        } catch (IOException e) {
            throw new IllegalStateException(TEMPLATE_PATH + " 템플릿 로드 실패", e);
        }
    }

    public String buildSystemPrompt(Position position, TechStack techStack) {
        ResolvedProfile profile = personaResolver.resolve(position, techStack);
        return template
                .replace("{PERSONA_BLOCK}", profile.fullPersona())
                .replace("{EVALUATION_PERSPECTIVE}", profile.evaluationPerspective())
                .replace("{FOLLOW_UP_DEPTH}", profile.followUpDepth());
    }
}
