package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;

@Slf4j
@Component
public class IntentClassifierPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/intent-classifier.txt";

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
        log.info("Intent Classifier 프롬프트 템플릿 로드 완료");
    }

    public String buildSystemPrompt() {
        return systemPromptTemplate;
    }

    public String buildUserPrompt(String mainQuestion, String answerText, List<FollowUpExchange> previousExchanges) {
        StringBuilder sb = new StringBuilder();
        sb.append("<<<MAIN_QUESTION>>>\n").append(mainQuestion).append("\n<<<END_MAIN_QUESTION>>>\n\n");
        sb.append("<<<USER_UTTERANCE>>>\n")
          .append(answerText != null ? answerText : "(없음)")
          .append("\n<<<END_USER_UTTERANCE>>>\n");

        if (previousExchanges != null && !previousExchanges.isEmpty()) {
            sb.append("\n이전 후속 대화 (맥락 참고용):\n");
            for (int i = 0; i < previousExchanges.size(); i++) {
                FollowUpExchange ex = previousExchanges.get(i);
                sb.append("[").append(i + 1).append("] Q: ").append(ex.getQuestion()).append("\n");
                sb.append("<<<PREVIOUS_TURN>>>\n")
                  .append(ex.getAnswer())
                  .append("\n<<<END_PREVIOUS_TURN>>>\n");
            }
        }

        sb.append("\n위 답변의 의도를 분류하세요.");
        return sb.toString();
    }
}
