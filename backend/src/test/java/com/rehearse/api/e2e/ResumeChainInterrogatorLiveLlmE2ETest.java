package com.rehearse.api.e2e;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
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
 * Live LLM E2E — Resume chain interrogator 가 실제 OpenAI 호출까지 정상 흐른다.
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * ./gradlew test --tests "com.rehearse.api.e2e.ResumeChainInterrogatorLiveLlmE2ETest" \
 *     -Djunit.jupiter.conditions.deactivate=org.junit.*DisabledCondition
 * </pre>
 *
 * 비용 발생 + 비결정적 → CI 미실행 (@Disabled). model_answer 품질 회귀 의심 시 수동 실행.
 */
@Disabled("Live LLM 호출 — 수동 실행만. OPENAI_API_KEY 필요.")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumeChainInterrogatorLiveLlmE2ETest extends AbstractMySqlContainerTest {

    @Autowired
    private ResumeChainInterrogatorPromptBuilder builder;

    @BeforeAll
    static void requireApiKey() {
        String key = System.getenv("OPENAI_API_KEY");
        assumeTrue(key != null && !key.isBlank() && !"disabled".equals(key),
                "OPENAI_API_KEY 환경변수 누락 — Live LLM E2E 스킵");
    }

    @Test
    @DisplayName("build 가 L4 chain topic + 레벨 깊이에 맞는 model_answer 를 채운다 + AC1/AC4/AC5 단언")
    void build_returns_level_appropriate_model_answer_for_l4() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-chain-1", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);
        String chainTopic = "Kafka partition hot key 완화";
        String userAnswer = "hot room 분리 외에 partition 수를 더 늘리거나 컨슈머별 in-memory queue 로 흡수하는 방법도 봤습니다. "
                + "전자는 broker 디스크 I/O 가 비례 증가하고, 후자는 인스턴스 죽으면 메시지 유실 위험이 있어 운영 채팅 도메인엔 적합하지 않다고 판단했습니다.";

        InterrogationResult result = builder.build(
                9990L, state, List.<FollowUpExchange>of(),
                "Live 테스트 프로젝트", chainTopic, 4, 4, userAnswer, 0);

        assertThat(result).isNotNull();
        assertThat(result.modelAnswer()).isNotBlank();
        assertThat(result.modelAnswer().length())
                .as("AC1 model_answer 길이 — 150~350")
                .isBetween(150, 350);
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 1인칭 답변 예시 어미 1+")
                .containsAnyOf("했습니다", "경험이 있습니다", "을 적용했습니다", "을 진행했습니다", "을 측정했습니다");
        assertThat(result.modelAnswer())
                .as("AC1 model_answer — 가이드 톤 어구 부재")
                .doesNotContain("해보세요", "하면 좋습니다", "이야기해보세요", "설명해보세요");
        assertThat(result.modelAnswer())
                .as("AC4 model_answer — chain topic 명시")
                .contains(chainTopic);
        assertThat(result.modelAnswer())
                .as("AC4 model_answer — L4 어휘 1+ (트레이드오프 / 비교 / 측정 / 장단점 / 대안 / 선택)")
                .containsAnyOf("트레이드오프", "비교", "측정", "장점", "단점", "대안", "선택", "판단");
        assertThat(result.modelAnswer())
                .as("AC5 model_answer — 구조 단서 1+ 매치")
                .containsAnyOf("STAR", "Situation", "Task", "Action", "Result", "결정", "근거", "트레이드오프", "학습", "장점", "단점", "대안", "선택", "결과", "성과", "향상", "개선", "도입", "적용");
    }
}
