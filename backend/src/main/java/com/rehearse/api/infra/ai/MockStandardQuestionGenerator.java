package com.rehearse.api.infra.ai;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.models.service.StandardQuestionGenerator;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.adapter.ClaudeStandardQuestionGenerator;
import com.rehearse.api.infra.ai.adapter.OpenAiStandardQuestionGenerator;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.GeneratedQuestionsWrapper;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnMissingBean({OpenAiStandardQuestionGenerator.class, ClaudeStandardQuestionGenerator.class})
public class MockStandardQuestionGenerator implements StandardQuestionGenerator {

    private static final String MOCK_QUESTIONS_JSON = """
            {"questions": [
              {"content": "[Mock] Java에서 GC(Garbage Collection)의 동작 원리를 설명해주세요.", "category": "JVM", "order": 1, "evaluation_criteria": "GC 알고리즘과 힙 구조 이해", "question_category": "CS", "best_answer": "Java GC는 힙 메모리에서 더 이상 참조되지 않는 객체를 자동으로 해제합니다."},
              {"content": "[Mock] RESTful API 설계 원칙 중 가장 중요하다고 생각하는 것은 무엇인가요?", "category": "API 설계", "order": 2, "evaluation_criteria": "REST 원칙 이해와 실무 적용", "question_category": "CS", "best_answer": "리소스 중심 URI 설계, HTTP 메서드의 의미론적 사용이 핵심입니다."},
              {"content": "[Mock] 데이터베이스 인덱스의 장단점을 설명해주세요.", "category": "데이터베이스", "order": 3, "evaluation_criteria": "인덱스 구조와 성능 트레이드오프 이해", "question_category": "CS", "best_answer": "인덱스는 B-Tree 구조로 검색 속도를 개선하지만 INSERT/UPDATE 오버헤드가 있습니다."},
              {"content": "[Mock] 동시성 문제를 해결하기 위한 방법들을 설명해주세요.", "category": "운영체제", "order": 4, "evaluation_criteria": "락, 세마포어, CAS 등 동시성 제어 이해", "question_category": "CS", "best_answer": "뮤텍스, 세마포어, CAS 기반 락프리 알고리즘이 있습니다."},
              {"content": "[Mock] 최근 진행한 프로젝트에서 가장 어려웠던 기술적 문제와 해결 과정을 설명해주세요.", "category": "경험", "order": 5, "evaluation_criteria": "문제 해결 과정과 학습 능력", "question_category": "RESUME", "best_answer": "STAR 기법으로 구조화하여 답변하세요."}
            ]}
            """;

    private final ObjectMapper objectMapper;

    @PostConstruct
    void init() {
        log.warn("=== MockStandardQuestionGenerator 활성화: API 키 없이 Mock 질문으로 동작합니다 ===");
    }

    @Override
    public List<GeneratedQuestion> generate(QuestionGenerationRequest request) {
        try {
            GeneratedQuestionsWrapper wrapper = objectMapper.readValue(MOCK_QUESTIONS_JSON, GeneratedQuestionsWrapper.class);
            return wrapper.questions();
        } catch (JsonProcessingException e) {
            log.error("[Mock] Mock 질문 JSON 파싱 실패", e);
            throw new BusinessException(AiErrorCode.PARSE_FAILED);
        }
    }
}
