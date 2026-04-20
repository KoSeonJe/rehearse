package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.ChatRequest;
import com.rehearse.api.infra.ai.dto.ChatResponse;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(ResilientAiClient.class)
public class MockAiClient extends AbstractAiClient {

    public MockAiClient(
            QuestionGenerationAdapter questionAdapter,
            FollowUpGenerationAdapter followUpAdapter) {
        super(questionAdapter, followUpAdapter, null);
    }

    @PostConstruct
    void init() {
        log.warn("=== MockAiClient 활성화: API 키 없이 Mock 데이터로 동작합니다 ===");
    }

    @Override
    public ChatResponse chat(ChatRequest request) {
        log.info("[Mock] chat 호출 - callType={}, messages={}", request.callType(), request.messages().size());

        String content = switch (request.callType()) {
            case "generate_questions" -> mockQuestionsJson();
            case "generate_followup" -> mockFollowUpJson();
            default -> "[Mock] " + request.callType() + " response";
        };
        return new ChatResponse(
                content,
                ChatResponse.Usage.empty(),
                "mock",
                "mock-model",
                false,
                false
        );
    }

    private String mockQuestionsJson() {
        return """
                {"questions": [
                  {"content": "[Mock] Java에서 GC(Garbage Collection)의 동작 원리를 설명해주세요.", "category": "JVM", "order": 1, "evaluationCriteria": "GC 알고리즘과 힙 구조 이해", "questionCategory": "CS", "modelAnswer": "Java GC는 힙 메모리에서 더 이상 참조되지 않는 객체를 자동으로 해제합니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] RESTful API 설계 원칙 중 가장 중요하다고 생각하는 것은 무엇인가요?", "category": "API 설계", "order": 2, "evaluationCriteria": "REST 원칙 이해와 실무 적용", "questionCategory": "CS", "modelAnswer": "리소스 중심 URI 설계, HTTP 메서드의 의미론적 사용이 핵심입니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 데이터베이스 인덱스의 장단점을 설명해주세요.", "category": "데이터베이스", "order": 3, "evaluationCriteria": "인덱스 구조와 성능 트레이드오프 이해", "questionCategory": "CS", "modelAnswer": "인덱스는 B-Tree 구조로 검색 속도를 개선하지만 INSERT/UPDATE 오버헤드가 있습니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 동시성 문제를 해결하기 위한 방법들을 설명해주세요.", "category": "운영체제", "order": 4, "evaluationCriteria": "락, 세마포어, CAS 등 동시성 제어 이해", "questionCategory": "CS", "modelAnswer": "뮤텍스, 세마포어, CAS 기반 락프리 알고리즘이 있습니다.", "referenceType": "GUIDE"},
                  {"content": "[Mock] 최근 진행한 프로젝트에서 가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.", "category": "경험", "order": 5, "evaluationCriteria": "문제 해결 과정과 학습 능력", "questionCategory": "RESUME", "modelAnswer": "STAR 기법으로 구조화하여 답변하세요.", "referenceType": "MODEL_ANSWER"}
                ]}
                """;
    }

    private String mockFollowUpJson() {
        return """
                {"question": "[Mock] 방금 말씀하신 내용에서 성능 최적화를 위해 구체적으로 어떤 접근을 하셨나요?", "reason": "답변의 기술적 깊이를 확인하기 위함", "type": "DEEP_DIVE", "modelAnswer": "[Mock] 성능 최적화를 위해 캐싱, 쿼리 최적화, 비동기 처리 등을 설명할 수 있어야 합니다."}
                """;
    }
}
