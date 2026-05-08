package com.rehearse.api.e2e;

import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundResponderResult;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

/**
 * Live LLM E2E — Resume playground opener / responder 가 실제 OpenAI 호출까지 정상 흐른다.
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * ./gradlew test --tests "com.rehearse.api.e2e.ResumePlaygroundLiveLlmE2ETest" \
 *     -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
 * </pre>
 *
 * 비용 발생 + 비결정적 → CI 미실행 (@Disabled). 컨텍스트 회귀 의심 시 수동 실행.
 */
@Disabled("Live LLM 호출 — 수동 실행만. OPENAI_API_KEY 필요.")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumePlaygroundLiveLlmE2ETest extends AbstractMySqlContainerTest {

    @Autowired
    private ResumePlaygroundPromptBuilder builder;

    @BeforeAll
    static void requireApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        assumeTrue(key != null && !key.isBlank() && !"disabled".equals(key),
                "OPENAI_API_KEY 환경변수 누락 — Live LLM E2E 스킵");
    }

    @Test
    @DisplayName("buildOpener 가 실제 OpenAI 응답으로 PlaygroundOpenerResult 를 채운다 + AC1/AC2/AC3 Opener 톤 + model_answer 분량/톤/맥락/구조 단언")
    void buildOpener_returns_non_blank_question_from_live_openai() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-resume-1", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        Project project = new Project("proj-live-1", "Live 테스트 프로젝트", List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase(
                "프로젝트에 대해 자유롭게 소개해주세요.", List.of("기술 스택", "주요 의사결정"));

        PlaygroundOpenerResult result = builder.buildOpener(9999L, state, project, phase);

        assertThat(result).isNotNull();
        assertThat(result.question()).isNotBlank();
        assertThat(result.ttsQuestion()).isNotBlank();

        assertThat(result.question())
                .as("AC1 — projectName 호명")
                .contains("Live 테스트 프로젝트");
        assertThat(result.question())
                .as("AC2 — narrow 기술 심문 어휘 부재")
                .doesNotContain("기술적 결정")
                .doesNotContain("트레이드오프")
                .doesNotContain("메커니즘")
                .doesNotContain("어떻게 동작");
        assertThat(result.question())
                .as("AC3 Opener — intro 의도 어휘군 1+ 매치")
                .containsAnyOf("역할", "맡으셨", "설명", "소개");

        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 200~300자 (LLM 비결정 ±30 완충)")
                .isNotNull();
        assertThat(result.modelAnswer().length())
                .as("AC1 model_answer 길이 — 170~330")
                .isBetween(170, 330);
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 1인칭 답변 예시 어미 1+")
                .containsAnyOf("했습니다", "경험이 있습니다", "을 적용했습니다", "을 진행했습니다", "을 측정했습니다");
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 가이드 톤 어구 부재")
                .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
        assertThat(result.modelAnswer())
                .as("AC2 model_answer — projectName 명시")
                .contains("Live 테스트 프로젝트");
        assertThat(result.modelAnswer())
                .as("AC5 model_answer — 구조 단서 1+ 매치")
                .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
    }

    @Test
    @DisplayName("buildResponder 가 실제 OpenAI 응답으로 감정·서사 oriented 후속 질문을 채운다 + AC2/AC3 Responder 톤 단언")
    void buildResponder_returns_emotion_oriented_question_from_live_openai() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-resume-2", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        Project project = new Project("proj-live-2", "Live 테스트 프로젝트", List.of(), List.of());
        String userAnswer = "이 프로젝트에서 결제 게이트웨이 연동을 담당했고, 트래픽 급증 상황에서 안정화 작업을 했습니다.";

        PlaygroundResponderResult result = builder.buildResponder(
                9998L, state, List.<FollowUpExchange>of(),
                project, userAnswer, List.of("결제 게이트웨이", "트래픽 안정화"),
                1, userAnswer.length());

        assertThat(result).isNotNull();
        assertThat(result.question()).isNotBlank();
        assertThat(result.question())
                .as("AC2 — narrow 기술 심문 어휘 부재")
                .doesNotContain("기술적 결정")
                .doesNotContain("트레이드오프")
                .doesNotContain("메커니즘")
                .doesNotContain("어떻게 동작");
        assertThat(result.question())
                .as("AC3 Responder — 감정·서사 의도 어휘군 1+ 매치")
                .containsAnyOf("어려웠던", "기억", "인상", "경험");
    }

    @Test
    @DisplayName("buildResponder model_answer 가 USER_ANSWER 핵심 명사 1+ 재인용 + AC1/AC5 분량/톤/구조 단언")
    void buildResponder_returns_first_person_model_answer_from_live_openai() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-resume-3", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        Project project = new Project("proj-live-3", "Live 테스트 프로젝트", List.of(), List.of());
        String userAnswer = "결제 시스템에서 동시성 문제를 해결한 경험이 있습니다. 락 전략을 비교해 분산락으로 결정했습니다.";

        PlaygroundResponderResult result = builder.buildResponder(
                9997L, state, List.<FollowUpExchange>of(),
                project, userAnswer, List.of("결제 시스템", "동시성", "분산락"),
                1, userAnswer.length());

        assertThat(result).isNotNull();
        assertThat(result.modelAnswer()).isNotBlank();
        assertThat(result.modelAnswer().length())
                .as("AC1 model_answer 길이 — 170~330")
                .isBetween(170, 330);
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 1인칭 답변 예시 어미 1+")
                .containsAnyOf("했습니다", "경험이 있습니다", "을 적용했습니다", "을 진행했습니다", "을 측정했습니다");
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 가이드 톤 어구 부재")
                .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
        assertThat(result.modelAnswer())
                .as("AC3 model_answer — USER_ANSWER 핵심 명사 1+ 재인용")
                .containsAnyOf("결제 시스템", "동시성", "분산락");
        assertThat(result.modelAnswer())
                .as("AC5 model_answer — 구조 단서 1+ 매치")
                .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습");
    }
}
