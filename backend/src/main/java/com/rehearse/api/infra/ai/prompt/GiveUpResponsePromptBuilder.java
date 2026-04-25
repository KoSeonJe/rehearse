package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;

@Slf4j
@Component
public class GiveUpResponsePromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/giveup-response.txt";

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
        log.info("GiveUp Response 프롬프트 템플릿 로드 완료");
    }

    public String buildSystemPrompt() {
        return systemPromptTemplate;
    }

    public String buildUserPrompt(FollowUpContext context, String mainQuestion, String answerText) {
        StringBuilder sb = new StringBuilder();
        sb.append("직무: ").append(context.position().name())
          .append(" | 레벨: ").append(context.level().name()).append("\n\n");
        sb.append("<<<MAIN_QUESTION>>>\n").append(mainQuestion).append("\n<<<END_MAIN_QUESTION>>>\n\n");
        sb.append("<<<USER_UTTERANCE>>>\n")
          .append(answerText != null ? answerText : "(없음)")
          .append("\n<<<END_USER_UTTERANCE>>>\n\n");
        sb.append("응시자가 포기 의사를 밝혔습니다. SCAFFOLD 또는 REVEAL_AND_MOVE_ON 모드를 선택하여 적절히 응답하세요.");
        return sb.toString();
    }
}
