package com.rehearse.api.e2e;

import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumePlaygroundPromptBuilder.PlaygroundOpenerResult;
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
 * Live LLM E2E — Resume playground opener 가 실제 OpenAI 호출까지 정상 흐른다.
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
@ActiveProfiles("llm-e2e")
class ResumePlaygroundLiveLlmE2ETest {

    @Autowired
    private ResumePlaygroundPromptBuilder builder;

    @BeforeAll
    static void requireApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        assumeTrue(key != null && !key.isBlank() && !"disabled".equals(key),
                "OPENAI_API_KEY 환경변수 누락 — Live LLM E2E 스킵");
    }

    @Test
    @DisplayName("buildOpener 가 실제 OpenAI 응답으로 PlaygroundOpenerResult 를 채운다")
    void buildOpener_returns_non_blank_question_from_live_openai() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-resume-1", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        Project project = new Project("proj-live-1", List.of(), List.of());
        PlaygroundPhase phase = new PlaygroundPhase(
                "프로젝트에 대해 자유롭게 소개해주세요.", List.of("기술 스택", "주요 의사결정"));

        PlaygroundOpenerResult result = builder.buildOpener(9999L, state, project, phase);

        assertThat(result).isNotNull();
        assertThat(result.question()).isNotBlank();
        assertThat(result.ttsQuestion()).isNotBlank();
    }
}
