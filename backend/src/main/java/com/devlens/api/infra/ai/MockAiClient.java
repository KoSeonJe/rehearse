package com.devlens.api.infra.ai;

import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.domain.interview.entity.Position;
import com.devlens.api.domain.interview.dto.FollowUpRequest;
import com.devlens.api.infra.ai.dto.*;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.core.type.TypeReference;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import jakarta.annotation.PostConstruct;
import java.util.List;

@Slf4j
@Component
@ConditionalOnMissingBean(ClaudeApiClient.class)
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private final ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        log.warn("=== MockAiClient 활성화: API 키 없이 Mock 데이터로 동작합니다 ===");
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(Position position, String positionDetail,
                                                      InterviewLevel level, List<InterviewType> interviewTypes,
                                                      List<String> csSubTopics, String resumeText,
                                                      Integer durationMinutes) {
        log.info("[Mock] generateQuestions 호출 - position={}, level={}, types={}, resume={}, duration={}",
                position, level, interviewTypes, resumeText != null ? "있음" : "없음", durationMinutes);

        int questionCount = ClaudePromptBuilder.calculateQuestionCount(durationMinutes, interviewTypes.size());

        String json = """
                [
                  {"content": "[Mock] Java에서 GC(Garbage Collection)의 동작 원리를 설명해주세요.", "category": "JVM", "order": 1, "evaluationCriteria": "GC 알고리즘과 힙 구조 이해"},
                  {"content": "[Mock] RESTful API 설계 원칙 중 가장 중요하다고 생각하는 것은 무엇인가요?", "category": "API 설계", "order": 2, "evaluationCriteria": "REST 원칙 이해와 실무 적용"},
                  {"content": "[Mock] 데이터베이스 인덱스의 장단점을 설명해주세요.", "category": "데이터베이스", "order": 3, "evaluationCriteria": "인덱스 구조와 성능 트레이드오프 이해"},
                  {"content": "[Mock] 동시성 문제를 해결하기 위한 방법들을 설명해주세요.", "category": "운영체제", "order": 4, "evaluationCriteria": "락, 세마포어, CAS 등 동시성 제어 이해"},
                  {"content": "[Mock] 최근 진행한 프로젝트에서 가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.", "category": "경험", "order": 5, "evaluationCriteria": "문제 해결 과정과 학습 능력"}
                ]
                """;

        List<GeneratedQuestion> allQuestions = parseJson(json, new TypeReference<>() {});
        return allQuestions.subList(0, Math.min(questionCount, allQuestions.size()));
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText,
                                                       String nonVerbalSummary,
                                                       List<FollowUpRequest.FollowUpExchange> previousExchanges) {
        log.info("[Mock] generateFollowUpQuestion 호출 - previousExchanges={}", previousExchanges != null ? previousExchanges.size() : 0);

        String json = """
                {"question": "[Mock] 방금 말씀하신 내용에서 성능 최적화를 위해 구체적으로 어떤 접근을 하셨나요?", "reason": "답변의 기술적 깊이를 확인하기 위함", "type": "DEEP_DIVE"}
                """;

        return parseJson(json, new TypeReference<>() {});
    }

    @Override
    public GeneratedReport generateReport(String feedbackSummary) {
        log.info("[Mock] generateReport 호출");

        String json = """
                {
                  "overallScore": 72,
                  "summary": "[Mock] 전반적으로 기술적 기초가 탄탄하며, 특히 자료구조와 알고리즘에 대한 이해도가 높습니다. 다만 시스템 설계 관련 답변에서 더 구체적인 사례를 들어 설명하면 좋겠습니다.",
                  "strengths": ["CS 기초 지식이 탄탄함", "논리적이고 구조화된 답변 방식", "질문 의도를 정확히 파악함"],
                  "improvements": ["시스템 설계 시 트레이드오프 분석 보완 필요", "비언어적 표현(시선, 자세) 안정감 개선", "답변 시 구체적인 수치나 사례 추가"]
                }
                """;

        return parseJson(json, new TypeReference<>() {});
    }

    @Override
    public List<GeneratedFeedback> generateFeedback(String answersJson) {
        log.info("[Mock] generateFeedback 호출");

        String json = """
                [
                  {"timestampSeconds": 5.0, "category": "CONTENT", "severity": "INFO", "content": "[Mock] 질문 의도를 정확히 파악하고 답변을 시작했습니다.", "suggestion": null},
                  {"timestampSeconds": 15.0, "category": "VERBAL", "severity": "SUGGESTION", "content": "[Mock] 답변 초반에 결론을 먼저 말하면 더 효과적입니다.", "suggestion": "PREP 기법(Point-Reason-Example-Point)을 활용해보세요."},
                  {"timestampSeconds": 30.0, "category": "NON_VERBAL", "severity": "WARNING", "content": "[Mock] 시선이 자주 아래로 향하는 경향이 있습니다.", "suggestion": "카메라를 바라보며 답변하는 연습을 해보세요."},
                  {"timestampSeconds": 45.0, "category": "CONTENT", "severity": "INFO", "content": "[Mock] 구체적인 사례를 들어 설명한 점이 좋습니다.", "suggestion": null},
                  {"timestampSeconds": 60.0, "category": "VERBAL", "severity": "SUGGESTION", "content": "[Mock] '음...', '그...' 같은 간투사 사용이 잦습니다.", "suggestion": "짧은 침묵으로 대체하면 더 자신감 있게 들립니다."},
                  {"timestampSeconds": 80.0, "category": "NON_VERBAL", "severity": "INFO", "content": "[Mock] 자세가 안정적이고 자연스러운 제스처를 사용했습니다.", "suggestion": null}
                ]
                """;

        return parseJson(json, new TypeReference<>() {});
    }

    private <T> T parseJson(String json, TypeReference<T> typeRef) {
        try {
            return objectMapper.readValue(json, typeRef);
        } catch (JsonProcessingException e) {
            throw new RuntimeException("Mock JSON 파싱 실패", e);
        }
    }
}
