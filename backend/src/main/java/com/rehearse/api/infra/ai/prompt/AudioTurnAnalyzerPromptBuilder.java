package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.question.entity.ReferenceType;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Component
public class AudioTurnAnalyzerPromptBuilder {

    private static final String TEMPLATE_PATH = "/prompts/template/audio-turn-analyzer.txt";

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
        log.info("Audio Turn Analyzer 프롬프트 템플릿 로드 완료");
    }

    public String buildSystemPrompt() {
        return systemPromptTemplate;
    }

    public String buildUserPromptText(
            String mainQuestion,
            ReferenceType questionReferenceType,
            List<Perspective> askedPerspectives
    ) {
        StringBuilder sb = new StringBuilder();
        sb.append("<<<MAIN_QUESTION>>>\n")
          .append(mainQuestion != null ? mainQuestion : "(없음)")
          .append("\n<<<END_MAIN_QUESTION>>>\n");
        sb.append("QUESTION_REFERENCE_TYPE: ").append(toReferenceLabel(questionReferenceType)).append("\n");
        sb.append("ASKED_PERSPECTIVES: ")
          .append(formatPerspectives(askedPerspectives))
          .append("\n");
        sb.append("\n첨부된 오디오를 전사하고 위 메타데이터를 참고해 JSON 한 객체로만 응답하세요.");
        return sb.toString();
    }

    private static String toReferenceLabel(ReferenceType refType) {
        if (refType == null) {
            return "CONCEPT";
        }
        return switch (refType) {
            case GUIDE -> "EXPERIENCE";
            case MODEL_ANSWER -> "CONCEPT";
        };
    }

    private static String formatPerspectives(List<Perspective> askedPerspectives) {
        if (askedPerspectives == null || askedPerspectives.isEmpty()) {
            return "(없음)";
        }
        return askedPerspectives.stream()
                .map(Enum::name)
                .collect(Collectors.joining(", "));
    }
}
