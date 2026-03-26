package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.prompt.QuestionCountCalculator;
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
@ConditionalOnMissingBean(ResilientAiClient.class)
@RequiredArgsConstructor
public class MockAiClient implements AiClient {

    private final ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        log.warn("=== MockAiClient 활성화: API 키 없이 Mock 데이터로 동작합니다 ===");
    }

    @Override
    public List<GeneratedQuestion> generateQuestions(QuestionGenerationRequest request) {
        log.info("[Mock] generateQuestions 호출 - position={}, level={}, types={}, techStack={}, resume={}, duration={}",
                request.position(), request.level(), request.interviewTypes(), request.techStack(),
                request.resumeText() != null ? "있음" : "없음", request.durationMinutes());

        int questionCount = QuestionCountCalculator.calculate(request.durationMinutes(), request.interviewTypes().size());

        String json = """
                [
                  {"content": "[Mock] Java에서 GC(Garbage Collection)의 동작 원리를 설명해주세요.", "category": "JVM", "order": 1, "evaluationCriteria": "GC 알고리즘과 힙 구조 이해", "questionCategory": "CS", "modelAnswer": "Java GC는 힙 메모리에서 더 이상 참조되지 않는 객체를 자동으로 해제합니다. Young Gen(Eden+Survivor)에서 Minor GC, Old Gen에서 Major GC가 발생하며, G1GC는 리전 단위로 관리합니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] RESTful API 설계 원칙 중 가장 중요하다고 생각하는 것은 무엇인가요?", "category": "API 설계", "order": 2, "evaluationCriteria": "REST 원칙 이해와 실무 적용", "questionCategory": "CS", "modelAnswer": "리소스 중심 URI 설계, HTTP 메서드의 의미론적 사용, 상태 코드 활용, HATEOAS 등이 핵심입니다. 특히 멱등성과 안전성 개념을 이해하고 적용하는 것이 중요합니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 데이터베이스 인덱스의 장단점을 설명해주세요.", "category": "데이터베이스", "order": 3, "evaluationCriteria": "인덱스 구조와 성능 트레이드오프 이해", "questionCategory": "CS", "modelAnswer": "인덱스는 B-Tree/Hash 구조로 검색 속도를 O(log n)으로 개선합니다. 장점은 SELECT 성능 향상, 단점은 INSERT/UPDATE/DELETE 시 인덱스 갱신 오버헤드와 추가 저장 공간입니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 동시성 문제를 해결하기 위한 방법들을 설명해주세요.", "category": "운영체제", "order": 4, "evaluationCriteria": "락, 세마포어, CAS 등 동시성 제어 이해", "questionCategory": "CS", "modelAnswer": "뮤텍스, 세마포어, 모니터 등의 동기화 기법과 CAS(Compare-And-Swap) 기반 락프리 알고리즘이 있습니다. Java에서는 synchronized, ReentrantLock, Atomic 클래스 등을 활용합니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 최근 진행한 프로젝트에서 가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.", "category": "경험", "order": 5, "evaluationCriteria": "문제 해결 과정과 학습 능력", "questionCategory": "RESUME", "modelAnswer": "프로젝트 배경, 직면한 기술적 문제, 시도한 해결 방법들, 최종 해결책과 그 이유, 결과와 배운 점을 STAR 기법으로 구조화하여 답변하세요.", "referenceType": "MODEL_ANSWER"}
                ]
                """;

        List<GeneratedQuestion> allQuestions = parseJson(json, new TypeReference<>() {});
        return allQuestions.subList(0, Math.min(questionCount, allQuestions.size()));
    }

    @Override
    public GeneratedFollowUp generateFollowUpQuestion(FollowUpGenerationRequest request) {
        log.info("[Mock] generateFollowUpQuestion 호출 - previousExchanges={}",
                request.previousExchanges() != null ? request.previousExchanges().size() : 0);

        String json = """
                {"question": "[Mock] 방금 말씀하신 내용에서 성능 최적화를 위해 구체적으로 어떤 접근을 하셨나요?", "reason": "답변의 기술적 깊이를 확인하기 위함", "type": "DEEP_DIVE", "modelAnswer": "[Mock] 성능 최적화를 위해 캐싱 전략, 쿼리 최적화, 비동기 처리 등의 접근 방식을 구체적으로 설명할 수 있어야 합니다."}
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
