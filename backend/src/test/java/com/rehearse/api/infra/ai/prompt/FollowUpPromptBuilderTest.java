package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.questionset.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ProfileYamlLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class FollowUpPromptBuilderTest {

    private FollowUpPromptBuilder builder;

    @BeforeEach
    void setUp() {
        ProfileYamlLoader loader = new ProfileYamlLoader();
        PersonaResolver personaResolver = new PersonaResolver(loader);
        builder = new FollowUpPromptBuilder(personaResolver);
        ReflectionTestUtils.invokeMethod(builder, "init");
    }

    @Test
    @DisplayName("BACKEND + JAVA_SPRING 요청 시 system prompt의 MEDIUM 페르소나에 백엔드(Java/Spring)가 포함된다")
    void buildSystemPrompt_backendJavaSpring_containsMediumPersonaWithJavaSpring() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "JVM의 GC 방식에 대해 설명해주세요.",
                "GC는 힙 메모리를 관리합니다.",
                null,
                List.of(),
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).contains("백엔드(Java/Spring)");
    }

    @Test
    @DisplayName("previousExchanges가 있을 때 user prompt에 '이전 후속 대화 (참고용)' 섹션이 포함된다")
    void buildUserPrompt_withPreviousExchanges_containsPreviousExchangesSection() {
        List<FollowUpRequest.FollowUpExchange> exchanges = List.of(
                new FollowUpRequest.FollowUpExchange("N+1 문제란?", "쿼리가 N번 추가 실행되는 현상입니다.")
        );
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.MID,
                "JPA의 fetch join에 대해 설명해주세요.",
                "N+1 문제를 해결하는 방법입니다.",
                null,
                exchanges,
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildUserPrompt(req);

        assertThat(prompt).contains("이전 후속 대화");
        assertThat(prompt).contains("참고용, 중복 회피");
        assertThat(prompt).contains("인용 대상 아님");
    }

    @Test
    @DisplayName("CONCEPT 모드(MODEL_ANSWER) system prompt는 경험 전제 프레이밍을 절대 금지하고 DEEP_DIVE/CLARIFICATION만 허용한다")
    void buildSystemPrompt_conceptMode_forbidsExperienceFraming() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "G1 Garbage Collector의 작동 원리를 설명해주세요.",
                "힙을 여러 리전으로 나누고 가비지가 많은 리전부터 수집합니다.",
                null,
                List.of(),
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildSystemPrompt(req);

        // CONCEPT 모드 핵심 지시
        assertThat(prompt).contains("CS 개념 설명형");
        assertThat(prompt).contains("경험 전제 프레이밍 절대 금지");
        assertThat(prompt).contains("DEEP_DIVE, CLARIFICATION 둘뿐");

        // 인용 규칙 & skip 규칙
        assertThat(prompt).contains("짧은 키워드");
        assertThat(prompt).contains("긴 문장을 따옴표로 감싸지 마세요");
        assertThat(prompt).contains("skip=true");

        // CONCEPT few-shot 예시가 포함됨
        assertThat(prompt).contains("G1 Garbage Collector");
        assertThat(prompt).contains("Mixed GC");

        // EXPERIENCE 모드의 Perspective 7종이 포함되어서는 안 됨
        assertThat(prompt).doesNotContain("MAINTAINABILITY: 유지보수성");
        assertThat(prompt).doesNotContain("COLLABORATION: 팀 의사결정");
    }

    @Test
    @DisplayName("EXPERIENCE 모드(GUIDE) system prompt는 기존 Perspective 7종과 CHALLENGE/APPLICATION 유형을 포함한다")
    void buildSystemPrompt_experienceMode_containsPerspectives() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.MID,
                "이전 프로젝트에서 N+1 문제를 해결한 경험을 말씀해주세요.",
                "@BatchSize로 전환했습니다.",
                null,
                List.of(),
                ReferenceType.GUIDE
        );

        String prompt = builder.buildSystemPrompt(req);

        // EXPERIENCE 모드 핵심 지시
        assertThat(prompt).contains("이력서/프로젝트/경험 기반");

        // Perspective 7종
        assertThat(prompt).contains("TRADEOFF");
        assertThat(prompt).contains("MAINTAINABILITY");
        assertThat(prompt).contains("RELIABILITY");
        assertThat(prompt).contains("SCALABILITY");
        assertThat(prompt).contains("TESTING");
        assertThat(prompt).contains("COLLABORATION");
        assertThat(prompt).contains("USER_IMPACT");

        // 후속 질문 유형 4종 전부
        assertThat(prompt).contains("DEEP_DIVE");
        assertThat(prompt).contains("CLARIFICATION");
        assertThat(prompt).contains("CHALLENGE");
        assertThat(prompt).contains("APPLICATION");

        // EXPERIENCE few-shot 예시
        assertThat(prompt).contains("@BatchSize");

        // 인용 규칙 강화
        assertThat(prompt).contains("짧은 키워드");
        assertThat(prompt).contains("긴 문장을 따옴표로 감싸지 마세요");
    }

    @Test
    @DisplayName("mainReferenceType이 null인 경우 안전한 기본값인 CONCEPT 모드로 폴백한다")
    void buildSystemPrompt_nullReferenceType_fallsBackToConceptMode() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "HashMap과 TreeMap의 차이점은?",
                "HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.",
                null,
                List.of(),
                null
        );

        String prompt = builder.buildSystemPrompt(req);

        // CONCEPT 모드로 폴백됐는지 확인
        assertThat(prompt).contains("CS 개념 설명형");
        assertThat(prompt).contains("경험 전제 프레이밍 절대 금지");
        // EXPERIENCE 전용 섹션이 없어야 함
        assertThat(prompt).doesNotContain("이력서/프로젝트/경험 기반");
    }

    @Test
    @DisplayName("system prompt에 답변 인용 강화·skip 분기·answerText 필수 규칙이 포함된다 (양 모드 공통)")
    void buildSystemPrompt_containsCommonRules() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "JVM의 GC 방식에 대해 설명해주세요.",
                "잘 모르겠습니다.",
                null,
                List.of(),
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildSystemPrompt(req);

        // few-shot 예시
        assertThat(prompt).contains("좋은 예");
        assertThat(prompt).contains("나쁜 예");

        // skip 분기 규칙
        assertThat(prompt).contains("skip=true");
        assertThat(prompt).contains("모르겠다");

        // 응답 형식에 skip/answerText 필드 모두 포함
        assertThat(prompt).contains("\"skip\"");
        assertThat(prompt).contains("\"answerText\"");
        assertThat(prompt).contains("JSON 형식으로만 응답");
    }

    @Test
    @DisplayName("user prompt는 '메인 질문' / '현재 답변' 라벨로 현재 답변을 강조한다")
    void buildUserPrompt_labelsHighlightCurrentAnswer() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "Spring의 IoC에 대해 설명해주세요.",
                "의존성을 외부에서 주입받는 방식입니다.",
                null,
                List.of(),
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildUserPrompt(req);

        assertThat(prompt).contains("메인 질문: Spring의 IoC에 대해 설명해주세요.");
        assertThat(prompt).contains("현재 답변: 의존성을 외부에서 주입받는 방식입니다.");
    }

    @Test
    @DisplayName("previousExchanges가 빈 리스트일 때 user prompt에 이전 후속 섹션이 없다")
    void buildUserPrompt_emptyPreviousExchanges_doesNotContainPreviousExchangesSection() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "Spring의 IoC에 대해 설명해주세요.",
                "의존성을 외부에서 주입받는 방식입니다.",
                null,
                List.of(),
                ReferenceType.MODEL_ANSWER
        );

        String prompt = builder.buildUserPrompt(req);

        assertThat(prompt).doesNotContain("이전 후속 대화");
    }
}
