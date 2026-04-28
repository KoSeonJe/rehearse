package com.rehearse.api.domain.feedback.session.synthesis;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackPayload;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackParseException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackParser {

    // F5-1: 한국어 어미 결합 패턴
    private static final List<Pattern> ABSTRACT_PATTERNS = List.of(
            Pattern.compile("더\\s*공부(하세요|하면|해야|할\\s*필요)"),
            Pattern.compile("꾸준히\\s*(노력|연습|학습|공부)(하세요|하면|해야)?"),
            Pattern.compile("열심히\\s*(하세요|공부|연습|노력)"),
            Pattern.compile("더\\s*많이\\s*(연습|공부|시도|노력)(하세요|해야)?"),
            Pattern.compile("(심도\\s*있게|보다\\s*깊이|기본기\\s*다지|역량\\s*강화|복습하세요)")
    );

    // F5-2: 1글자 한글 토큰 제거, lowercase 비교
    private static final List<String> DELIVERY_KEYWORDS = List.of(
            "필러워드", "filler word", "filler", "tonepattern", "speechpace",
            "toneconfidence", "fillerwords", "speech pace"
    );

    // F5-3: 단어 경계 dimension 참조 패턴
    private static final Pattern DIMENSION_PATTERN = Pattern.compile(
            "\\b(D1|D2|D3|D4|D5|D6|D7|D8|D9|D10|D11|D12|D13|D14)\\b"
    );

    private final ObjectMapper objectMapper;

    public SessionFeedbackPayload parse(String json, SessionFeedbackInput input) {
        SessionFeedbackPayload payload = deserialize(json);
        validateCardinality(payload, input);
        validateNoAbstractPhrases(payload);
        validateContentDeliverySourceSeparation(payload, input);
        validateCrossCategoryNarrative(payload, input);
        return payload;
    }

    private SessionFeedbackPayload deserialize(String json) {
        try {
            return objectMapper.readValue(json, SessionFeedbackPayload.class);
        } catch (Exception e) {
            log.warn("SessionFeedback JSON 역직렬화 실패: {}", e.getMessage());
            throw new SessionFeedbackParseException("JSON 구조 불일치: " + e.getMessage());
        }
    }

    // F5-4: 5섹션 cardinality 검증
    private void validateCardinality(SessionFeedbackPayload payload, SessionFeedbackInput input) {
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
        if (payload.delivery() == null
                && (input.deliveryAnalysis() != null || input.visionAnalysis() != null)) {
            throw new SessionFeedbackParseException("section=delivery 누락 (delivery/vision 입력 존재)");
        }
    }

    private void validateNoAbstractPhrases(SessionFeedbackPayload payload) {
        String fullText = extractAllObservationText(payload);
        for (Pattern pattern : ABSTRACT_PATTERNS) {
            if (pattern.matcher(fullText).find()) {
                log.warn("추상 표현 감지 — 재시도 필요: pattern={}", pattern.pattern());
                throw new SessionFeedbackParseException("추상 표현 감지: " + pattern.pattern());
            }
        }
    }

    private void validateContentDeliverySourceSeparation(SessionFeedbackPayload payload, SessionFeedbackInput input) {
        if (input.deliveryAnalysis() == null && input.visionAnalysis() == null) {
            return;
        }

        String deliveryText = buildDeliverySourceText(input);
        if (deliveryText.isBlank()) {
            return;
        }

        String contentText = buildContentObservationText(payload);
        if (hasSignificantOverlap(contentText, deliveryText)) {
            log.warn("Content 섹션에 Delivery 소스 텍스트 교차 참조 감지 — 재시도 필요");
            throw new SessionFeedbackParseException("section=content/delivery 소스 교차 참조 위반");
        }

        if (payload.delivery() != null) {
            String deliveryObservation = buildDeliveryObservationText(payload);
            if (containsDimensionReference(deliveryObservation)) {
                log.warn("Delivery 섹션에 Rubric 차원 코드 재인용 감지 — 재시도 필요");
                throw new SessionFeedbackParseException("section=delivery Rubric 차원 코드 재인용 위반");
            }
        }
    }

    // F5-5: cross-category narrative 검증
    private void validateCrossCategoryNarrative(SessionFeedbackPayload payload, SessionFeedbackInput input) {
        if (input.scoresByCategory() == null || input.scoresByCategory().size() < 2) {
            return;
        }
        if (payload.overall() == null || payload.overall().narrative() == null) {
            return;
        }
        String narrative = payload.overall().narrative();
        long matchCount = input.scoresByCategory().keySet().stream()
                .filter(narrative::contains)
                .count();
        if (matchCount < 2) {
            log.warn("section=overall.narrative cross-category 언급 부족: found={}", matchCount);
            throw new SessionFeedbackParseException(
                    "section=overall.narrative cross-category 카테고리명 2개 이상 포함 필요");
        }
    }

    private String extractAllObservationText(SessionFeedbackPayload payload) {
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

    private String buildContentObservationText(SessionFeedbackPayload payload) {
        StringBuilder sb = new StringBuilder();
        if (payload.strengths() != null) {
            payload.strengths().forEach(s -> {
                if (s.observation() != null) sb.append(s.observation()).append(" ");
            });
        }
        if (payload.gaps() != null) {
            payload.gaps().forEach(g -> {
                if (g.observation() != null) sb.append(g.observation()).append(" ");
            });
        }
        return sb.toString();
    }

    private String buildDeliveryObservationText(SessionFeedbackPayload payload) {
        if (payload.delivery() == null) return "";
        StringBuilder sb = new StringBuilder();
        if (payload.delivery().fillerWords() != null) sb.append(payload.delivery().fillerWords()).append(" ");
        if (payload.delivery().tonePattern() != null) sb.append(payload.delivery().tonePattern()).append(" ");
        if (payload.delivery().action() != null) sb.append(payload.delivery().action());
        return sb.toString();
    }

    private String buildDeliverySourceText(SessionFeedbackInput input) {
        StringBuilder sb = new StringBuilder();
        if (input.deliveryAnalysis() != null) sb.append(input.deliveryAnalysis()).append(" ");
        if (input.visionAnalysis() != null) sb.append(input.visionAnalysis());
        return sb.toString();
    }

    private boolean hasSignificantOverlap(String contentText, String deliveryText) {
        // F5-2: lowercase 비교, 1글자 한글 토큰 제거
        String lowerContent = contentText.toLowerCase();
        String lowerDelivery = deliveryText.toLowerCase();
        for (String keyword : DELIVERY_KEYWORDS) {
            String lowerKeyword = keyword.toLowerCase();
            if (lowerContent.contains(lowerKeyword) && lowerDelivery.contains(lowerKeyword)) {
                return true;
            }
        }
        return false;
    }

    // F5-3: 단어 경계 dimension 매칭
    private boolean containsDimensionReference(String deliveryText) {
        return DIMENSION_PATTERN.matcher(deliveryText).find();
    }
}
