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

    private static final String DEPTH_GUIDE_5_TYPES = """
            ## main 질문 깊이 5 유형 (분배 룰)
            아래 5 유형을 main 질문에 분배하여 출제한다. 각 main 질문은 정확히 하나의 유형으로 분류된다.

            1. TRADEOFF — 의사결정에 따른 트레이드오프 검증
               예: "선택한 정책의 잃은 것이 무엇이며, 그 비용을 어떻게 감수했나요?"
            2. LIMITATION — 한계 / 실패 시나리오
               예: "이 구성에서 어떤 입력 / 부하 / 장애가 들어오면 깨지나요?"
            3. QUANTITATIVE — 수치 / 측정 검증
               예: "p95 800ms → 120ms 를 어떤 부하 조건 / 데이터 분포에서 측정했나요?"
            4. ALTERNATIVE — 대안 비교
               예: "Polling 대신 WebSocket 을 채택했다면 어떤 점에서 달라졌을까요?"
            5. PRINCIPLE — 동작 원리
               예: "Redis Lua 가 동시성을 보장하는 내부 원리를 설명해주세요."

            분배 가이드:
            - 5 유형 중 한 유형이 main 5건 묶음에서 4건 이상 점유하지 않도록 한다 (편중 금지).
            - RESUME_SKELETON.projects[].depth_signals (tradeoffs / alternatives / quantitative / decision_rationale) 가 있으면 그 내용을 인용해 TRADEOFF / ALTERNATIVE / QUANTITATIVE 질문을 우선 생성한다.
            - 각 main 질문 객체에 반드시 `depth_type` 필드를 TRADEOFF / LIMITATION / QUANTITATIVE / ALTERNATIVE / PRINCIPLE 중 하나로 명시한다.
            """;

    private static final String FORBIDDEN_PATTERNS = """
            ## 표층 질문 금지 패턴
            아래 형식의 main 질문은 생성 금지. 직무 / 깊이 가치가 낮아 응시자의 학습 가치가 떨어진다.

            - "왜 X 를 사용 / 선택 / 채택 했나요?" (선택 이유 단순 묻기)
            - "X 의 장점은 무엇인가요?" / "X 의 단점은 무엇인가요?" (일반 책 지식 묻기)
            - "X 와 Y 의 차이점은 무엇인가요?" (단순 비교 정의 묻기 — ALTERNATIVE 유형으로 응시자 의사결정 맥락에서 묻는 것은 허용)
            - "X 를 어떻게 도입했나요?" (도입 절차 단순 묻기)

            대신 다음 방향으로 묻는다:
            - 그 선택으로 무엇을 잃었는지 (TRADEOFF)
            - 어떤 조건에서 깨지는지 (LIMITATION)
            - 어떤 수치 / 데이터로 검증했는지 (QUANTITATIVE)
            - 다른 대안과 비교 시 본인 구성의 약점은 무엇인지 (ALTERNATIVE)
            - 내부 동작 원리를 설명할 수 있는지 (PRINCIPLE)
            """;

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
                .replace("{FOLLOW_UP_DEPTH}", profile.followUpDepth())
                .replace("{DEPTH_GUIDE_5_TYPES}", DEPTH_GUIDE_5_TYPES)
                .replace("{FORBIDDEN_PATTERNS}", FORBIDDEN_PATTERNS);
    }
}
