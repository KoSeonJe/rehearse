package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
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
                List.of()
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
                exchanges
        );

        String prompt = builder.buildUserPrompt(req);

        assertThat(prompt).contains("이전 후속 대화");
        assertThat(prompt).contains("참고용, 중복 회피");
        assertThat(prompt).contains("인용 대상 아님");
    }

    @Test
    @DisplayName("system prompt에 질문 생성 절차·답변 인용 강화·few-shot 예시·skip 분기·answerText 필수 규칙이 포함된다")
    void buildSystemPrompt_containsStrengthenedRules() {
        FollowUpGenerationRequest req = new FollowUpGenerationRequest(
                Position.BACKEND,
                TechStack.JAVA_SPRING,
                InterviewLevel.JUNIOR,
                "JVM의 GC 방식에 대해 설명해주세요.",
                "잘 모르겠습니다.",
                null,
                List.of()
        );

        String prompt = builder.buildSystemPrompt(req);

        // 질문 생성 3단계 절차
        assertThat(prompt).contains("핵심 개념 추출");
        assertThat(prompt).contains("인용 구문 작성");
        assertThat(prompt).contains("심화 질문 작성");

        // 가장 최근 답변 인용 강조 (multi-round 대응)
        assertThat(prompt).contains("가장 최근 답변");

        // few-shot 예시
        assertThat(prompt).contains("좋은 예");
        assertThat(prompt).contains("나쁜 예");

        // skip 분기 규칙
        assertThat(prompt).contains("skip=true");
        assertThat(prompt).contains("모르겠다");

        // 응답 형식에 skip/answerText 필드 모두 포함 + skip 시에도 answerText 필수
        assertThat(prompt).contains("\"skip\"");
        assertThat(prompt).contains("\"answerText\"");
        assertThat(prompt).contains("skip 여부와 무관하게 항상 포함");
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
                List.of()
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
                List.of()
        );

        String prompt = builder.buildUserPrompt(req);

        assertThat(prompt).doesNotContain("이전 후속 대화");
    }
}
