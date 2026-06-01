package com.rehearse.api.domain.feedback.session.synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackParser {

    private static final List<Pattern> ABSTRACT_PATTERNS = List.of(
            Pattern.compile("더\\s*공부(하세요|하면|해야|할\\s*필요)"),
            Pattern.compile("꾸준히\\s*(노력|연습|학습|공부)(하세요|하면|해야)?"),
            Pattern.compile("열심히\\s*(하세요|공부|연습|노력)"),
            Pattern.compile("더\\s*많이\\s*(연습|공부|시도|노력)(하세요|해야)?"),
            Pattern.compile("(심도\\s*있게|보다\\s*깊이|기본기\\s*다지|역량\\s*강화|복습하세요)")
    );

    private final ObjectMapper objectMapper;

    public GeneratedSessionFeedback parse(String json, SessionFeedbackInput input) {
        GeneratedSessionFeedback payload = deserialize(json);
        validateCardinality(payload, input);
        validateNoAbstractPhrases(payload);
        return payload;
    }

    private GeneratedSessionFeedback deserialize(String json) {
        try {
            return objectMapper.readValue(json, GeneratedSessionFeedback.class);
        } catch (com.fasterxml.jackson.databind.exc.ValueInstantiationException e) {
            Throwable cause = e.getCause();
            String causeMsg = cause != null ? cause.getMessage() : e.getMessage();
            log.warn("SessionFeedback record invariant 위반: {}", causeMsg);
            if (causeMsg != null && causeMsg.contains("overall")) {
                throw new SessionFeedbackParseException("section=overall 누락");
            }
            throw new SessionFeedbackParseException("JSON 구조 불일치: " + causeMsg);
        } catch (Exception e) {
            log.warn("SessionFeedback JSON 역직렬화 실패: {}", e.getMessage());
            throw new SessionFeedbackParseException("JSON 구조 불일치: " + e.getMessage());
        }
    }

    private void validateCardinality(GeneratedSessionFeedback payload, SessionFeedbackInput input) {
        if (payload.overall() == null) {
            throw new SessionFeedbackParseException("section=overall 누락");
        }
        if (payload.strengths() == null || payload.strengths().isEmpty()) {
            throw new SessionFeedbackParseException("section=strengths 누락 또는 빈 배열");
        }
        if (payload.gaps() == null || payload.gaps().isEmpty()) {
            throw new SessionFeedbackParseException("section=gaps 누락 또는 빈 배열");
        }
        if (payload.weekPlan() == null || payload.weekPlan().isEmpty()) {
            throw new SessionFeedbackParseException("section=week_plan 누락 또는 빈 배열");
        }
        // delivery는 input에 delivery/vision 데이터가 없을 때만 null 허용
        if (payload.delivery() == null && hasDeliveryInput(input)) {
            throw new SessionFeedbackParseException("section=delivery 누락 (delivery/vision 입력 존재)");
        }
    }

    private void validateNoAbstractPhrases(GeneratedSessionFeedback payload) {
        String fullText = extractAllObservationText(payload);
        for (Pattern pattern : ABSTRACT_PATTERNS) {
            if (pattern.matcher(fullText).find()) {
                log.warn("추상 표현 감지 — 재시도 필요: pattern={}", pattern.pattern());
                throw new SessionFeedbackParseException("추상 표현 감지: " + pattern.pattern());
            }
        }
    }

    private String extractAllObservationText(GeneratedSessionFeedback payload) {
        StringBuilder sb = new StringBuilder();
        if (payload.overall() != null && payload.overall().narrative() != null) {
            sb.append(payload.overall().narrative());
        }
        if (payload.strengths() != null) {
            payload.strengths().forEach(s -> {
                if (s.observation() != null) sb.append(s.observation());
                if (s.whyMatters() != null) sb.append(s.whyMatters());
            });
        }
        if (payload.gaps() != null) {
            payload.gaps().forEach(g -> {
                if (g.observation() != null) sb.append(g.observation());
                if (g.concreteAction() != null) sb.append(g.concreteAction());
            });
        }
        return sb.toString();
    }

    private boolean hasDeliveryInput(SessionFeedbackInput input) {
        return input.deliveryAnalysis() != null
                || input.visionAnalysis() != null;
    }
}
