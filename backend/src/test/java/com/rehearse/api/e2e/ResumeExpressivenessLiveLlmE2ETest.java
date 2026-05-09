package com.rehearse.api.e2e;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assumptions.assumeTrue;

import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.ResumeExtractionService;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import com.rehearse.api.support.fixtures.ResumeFixtures;
import com.rehearse.api.support.fixtures.ResumeFixtures.ResumeFixture;
import java.util.List;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

/**
 * Resume 표현력 인프라 (Phase 1) 회귀 — extractor LLM 산출물에 이력서 어휘군 ≥ 4/5 포함되는지 검증.
 *
 * <p>3 fixture (백엔드 / 풀스택 / 임베디드) 중 ≥ 2 fixture 통과 = 회귀 합격 (LLM 비결정성 완충).
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * RUN_LIVE_API=true ./gradlew test --tests "ResumeExpressivenessLiveLlmE2ETest"
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumeExpressivenessLiveLlmE2ETest extends AbstractMySqlContainerTest {

    @Autowired
    private ResumeExtractionService extractionService;

    @BeforeAll
    static void requireApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        assumeTrue(key != null && !key.isBlank() && !"disabled".equals(key),
                "OPENAI_API_KEY 환경변수 누락 — Live LLM E2E 스킵");
    }

    @Test
    @DisplayName("3 fixture × extractor 산출물 어휘군 ≥ 4/5 매칭 — 3건 중 ≥ 2건 통과")
    void extractor_artifacts_contain_resume_vocabulary_for_at_least_two_fixtures() {
        List<ResumeFixture> fixtures = ResumeFixtures.all();
        long passed = fixtures.stream()
                .filter(this::vocabularyMatchesArtifacts)
                .count();

        assertThat(passed)
                .as("3 fixture 중 ≥ 2건 통과 (LLM 비결정성 완충). 통과 수=%d", passed)
                .isGreaterThanOrEqualTo(2);
    }

    private boolean vocabularyMatchesArtifacts(ResumeFixture fixture) {
        ResumeSkeleton skeleton = extractionService.extract(
                fixture.resumeText(), "live-expressiveness-" + fixture.label() + "-" + System.currentTimeMillis());

        String artifacts = collectArtifacts(skeleton);
        long matched = fixture.vocabulary().stream()
                .filter(token -> containsIgnoreCase(artifacts, token))
                .count();

        return matched >= fixture.passThreshold();
    }

    private String collectArtifacts(ResumeSkeleton skeleton) {
        StringBuilder sb = new StringBuilder();
        for (Project project : skeleton.projects()) {
            sb.append(safe(project.projectName())).append('\n');
            sb.append(String.join(" ", project.techStack())).append('\n');
            sb.append(safe(project.role())).append('\n');
            sb.append(safe(project.architecture())).append('\n');
            sb.append(String.join(" ", project.decisions())).append('\n');
            for (ResumeClaim claim : project.claims()) {
                sb.append(claim.text()).append('\n');
            }
        }
        return sb.toString();
    }

    private boolean containsIgnoreCase(String haystack, String needle) {
        return haystack.toLowerCase().contains(needle.toLowerCase());
    }

    private String safe(String s) {
        return s == null ? "" : s;
    }
}
