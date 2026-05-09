package com.rehearse.api.e2e;

import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder;
import com.rehearse.api.infra.ai.prompt.ResumeChainInterrogatorPromptBuilder.InterrogationResult;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live LLM eval — Resume chain interrogator 가 실제 호출 시 거짓 다리 phrasing 을 생성하지 않는다 (#466).
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * RUN_LIVE_API=true ./gradlew test \
 *     --tests "com.rehearse.api.e2e.ResumeChainInterrogatorLiveEvalTest"
 * </pre>
 *
 * RUN_LIVE_API 미설정 시 자동 skip — CI 비용 / 비결정성 회피.
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumeChainInterrogatorLiveEvalTest extends AbstractMySqlContainerTest {

    private static final Pattern FALSE_BRIDGE = Pattern.compile("방금\\s*답변\\s*하신|방금\\s*말씀\\s*하신");
    private static final Pattern CLARIFICATION_HINT = Pattern.compile("(다시|구체적으로|좀 더 자세히|어떤 의미)");

    @Autowired
    private ResumeChainInterrogatorPromptBuilder builder;

    private record Fixture(String label, String chainTopic, String previousAnswer, List<String> chainTopicTokens, List<String> answerNouns) {}

    private static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture(
                        "redis-cache",
                        "Redis Cache-Aside TTL 정책",
                        "주문 조회 캐싱은 TTL 5분으로 설정했고, single-flight 로 cache stampede 를 막았습니다.",
                        List.of("Redis", "Cache", "TTL"),
                        List.of("주문", "캐싱", "TTL", "stampede")
                ),
                new Fixture(
                        "websocket-pubsub",
                        "WebSocket Redis Pub/Sub 브로드캐스트",
                        "다중 인스턴스 환경에서 Redis Pub/Sub 으로 메시지를 브로드캐스트했습니다.",
                        List.of("WebSocket", "Pub/Sub"),
                        List.of("인스턴스", "Redis", "메시지")
                ),
                new Fixture(
                        "kafka-partition",
                        "Kafka partition hot key 완화",
                        "hot room 분리와 partition 수 증가 두 가지를 비교 후 hot room 분리를 채택했습니다.",
                        List.of("Kafka", "partition"),
                        List.of("partition", "hot", "분리")
                ),
                new Fixture(
                        "freertos-mqtt",
                        "FreeRTOS task 우선순위 + MQTT QoS",
                        "센서 폴링과 통신 task 우선순위를 분리하고 MQTT QoS 1 + 로컬 큐로 패킷 유실을 막았습니다.",
                        List.of("FreeRTOS", "MQTT", "QoS"),
                        List.of("센서", "폴링", "큐")
                ),
                new Fixture(
                        "jpa-fetch",
                        "JPA fetch join N+1 해소",
                        "주문 목록 API 의 N+1 을 fetch join 으로 해결해 p95 가 800ms 에서 120ms 로 줄었습니다.",
                        List.of("JPA", "fetch", "N+1"),
                        List.of("주문", "목록", "p95")
                )
        );
    }

    @Test
    @DisplayName("fixture 5종 — chain 첫 질문에 거짓 다리 phrasing 부재 + 직전 답변 명사·chain topic 동시 등장")
    void fixtures_chainQuestion_avoidsFalseBridgeAndCarriesContext() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-eval-skeleton", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);

        fixtures().forEach(fx -> {
            InterrogationResult result = builder.build(
                    100L, state, List.<FollowUpExchange>of(),
                    "Live 테스트 프로젝트", fx.chainTopic(), 1, 1, fx.previousAnswer(), 0);

            assertThat(result.question())
                    .as("[%s] question 비어 있지 않음", fx.label())
                    .isNotBlank();

            assertThat(FALSE_BRIDGE.matcher(result.question()).find())
                    .as("[%s] 거짓 다리 phrasing 0건 — '%s'", fx.label(), result.question())
                    .isFalse();

            boolean hasAnyTopicToken = fx.chainTopicTokens().stream().anyMatch(result.question()::contains);
            assertThat(hasAnyTopicToken)
                    .as("[%s] chain topic 토큰 1+ 등장 — '%s'", fx.label(), result.question())
                    .isTrue();

            boolean hasAnyAnswerNoun = fx.answerNouns().stream().anyMatch(result.question()::contains);
            assertThat(hasAnyAnswerNoun)
                    .as("[%s] 직전 답변 명사 1+ 반영 — '%s'", fx.label(), result.question())
                    .isTrue();
        });
    }

    @Test
    @DisplayName("analysis empty (answer_quality=1) 시 chain 질문이 명료화 어휘 1+ 포함")
    void emptyAnalysis_promptsClarification() {
        ResumeSkeleton skeleton = new ResumeSkeleton(
                "live-eval-skeleton", "hash", CandidateLevel.MID, "backend", List.of(), null);
        InterviewRuntimeState state = new InterviewRuntimeState("MID", skeleton);

        InterrogationResult result = builder.build(
                101L, state, List.<FollowUpExchange>of(),
                "Live 테스트 프로젝트", "Redis Cache-Aside TTL 정책", 1, 1, "음... 그게 뭔지 잘 모르겠어요.", 0);

        assertThat(result.question()).isNotBlank();
        assertThat(CLARIFICATION_HINT.matcher(result.question()).find())
                .as("answer_quality=1 (명료화 정책) 시 명료화 어휘 1+ — '%s'", result.question())
                .isTrue();
        assertThat(FALSE_BRIDGE.matcher(result.question()).find())
                .as("거짓 다리 phrasing 부재")
                .isFalse();
    }
}
