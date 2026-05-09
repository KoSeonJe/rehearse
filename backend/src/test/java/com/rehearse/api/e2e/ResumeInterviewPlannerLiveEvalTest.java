package com.rehearse.api.e2e;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.ClaimType;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.Priority;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.StepType;
import com.rehearse.api.domain.resume.service.ResumeInterviewPlanner;
import com.rehearse.api.global.support.AbstractMySqlContainerTest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.EnabledIfEnvironmentVariable;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;
import java.util.stream.Stream;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Live LLM eval — Resume planner OPENER 가 진부 어휘 회귀 없이 project_name 을 anchor 한다 (#466 phase 2).
 *
 * <h3>실행 방법</h3>
 * <pre>
 * export OPENAI_API_KEY=sk-...
 * RUN_LIVE_API=true ./gradlew test \
 *     --tests "com.rehearse.api.e2e.ResumeInterviewPlannerLiveEvalTest"
 * </pre>
 */
@EnabledIfEnvironmentVariable(named = "RUN_LIVE_API", matches = "true")
@SpringBootTest
@ActiveProfiles({"test", "llm-e2e"})
class ResumeInterviewPlannerLiveEvalTest extends AbstractMySqlContainerTest {

    private static final Pattern INTRO_VERB = Pattern.compile("(설명|소개|어떤\\s*프로젝트|간단히)");
    private static final Pattern ROLE_HINT = Pattern.compile("(역할|맡으)");
    private static final Pattern STALE_AFFECT = Pattern.compile("(인상\\s*깊|어려웠던|기억에\\s*남)");

    @Autowired
    private ResumeInterviewPlanner planner;

    private record Fixture(String label, ResumeSkeleton skeleton, String expectedProjectNameTokenAnyOf) {}

    private static ResumeSkeleton singleProjectSkeleton(String resumeId, String projectId, String projectName, String chainTopic) {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT"),
                new ChainStep(2, StepType.HOW, "HOW"),
                new ChainStep(3, StepType.WHY_MECH, "WHY"),
                new ChainStep(4, StepType.TRADEOFF, "TRADEOFF")
        );
        List<ResumeClaim> claims = List.of(
                new ResumeClaim("성능 개선 경험", ClaimType.IMPLEMENTATION, Priority.HIGH)
        );
        Project project = new Project(projectId, projectName,
                List.of(), "", "", List.of(), claims,
                List.of(new InterrogationChain(chainTopic, 0.9, steps)));
        return new ResumeSkeleton(resumeId, "hash-" + resumeId, CandidateLevel.MID, "backend",
                List.of(project), Map.of());
    }

    private static Stream<Fixture> fixtures() {
        return Stream.of(
                new Fixture("redis-cache",
                        singleProjectSkeleton("r1", "p_redis", "주문 조회 캐싱 개선 프로젝트", "Redis Cache-Aside"),
                        "주문 조회 캐싱 개선 프로젝트"),
                new Fixture("websocket-board",
                        singleProjectSkeleton("r2", "p_ws", "실시간 협업 보드 프로젝트", "WebSocket Pub/Sub"),
                        "실시간 협업 보드 프로젝트"),
                new Fixture("iot-firmware",
                        singleProjectSkeleton("r3", "p_iot", "산업용 IoT 게이트웨이 펌웨어", "FreeRTOS task"),
                        "산업용 IoT 게이트웨이 펌웨어"),
                new Fixture("payment-batch",
                        singleProjectSkeleton("r4", "p_pay", "결제 정산 배치 시스템", "Spring Batch chunk"),
                        "결제 정산 배치 시스템"),
                new Fixture("search-platform",
                        singleProjectSkeleton("r5", "p_search", "통합 검색 플랫폼", "Elasticsearch indexing"),
                        "통합 검색 플랫폼")
        );
    }

    @Test
    @DisplayName("fixture 5종 — OPENER 가 project_name 명시 + 설명/역할 어휘 + 진부 어휘 0건")
    void planner_opener_anchorsProjectNameAndAvoidsStaleAffect() {
        fixtures().forEach(fx -> {
            InterviewPlan plan = planner.plan(fx.skeleton(), 30);
            String opener = plan.projectPlans().get(0).playgroundPhase().openerQuestion();

            assertThat(opener)
                    .as("[%s] project_name anchor — '%s'", fx.label(), opener)
                    .contains(fx.expectedProjectNameTokenAnyOf());

            assertThat(INTRO_VERB.matcher(opener).find())
                    .as("[%s] 설명/소개 어휘 1+ — '%s'", fx.label(), opener)
                    .isTrue();

            assertThat(ROLE_HINT.matcher(opener).find())
                    .as("[%s] 역할 어휘 1+ — '%s'", fx.label(), opener)
                    .isTrue();

            assertThat(STALE_AFFECT.matcher(opener).find())
                    .as("[%s] 진부 어휘 0건 — '%s'", fx.label(), opener)
                    .isFalse();
        });
    }

    @Test
    @DisplayName("멤버십 — 동일 skeleton 2회 호출 시 양쪽 모두 skeleton.projects[].name 중 1개 anchor + 복수 등장 0건")
    void planner_opener_membership_singleAnchorAcrossInvocations() {
        ResumeSkeleton skeleton = multiProjectSkeleton();
        List<String> projectNames = skeleton.projects().stream().map(Project::projectName).toList();

        InterviewPlan first = planner.plan(skeleton, 30);
        InterviewPlan second = planner.plan(skeleton, 30);

        String openerA = first.projectPlans().get(0).playgroundPhase().openerQuestion();
        String openerB = second.projectPlans().get(0).playgroundPhase().openerQuestion();

        assertSingleAnchor(openerA, projectNames, "first");
        assertSingleAnchor(openerB, projectNames, "second");
    }

    private static void assertSingleAnchor(String opener, List<String> projectNames, String label) {
        long matchedNames = projectNames.stream().filter(opener::contains).count();
        assertThat(matchedNames)
                .as("[%s] 정확히 1개 project_name 만 anchor — opener='%s'", label, opener)
                .isEqualTo(1L);
    }

    private static ResumeSkeleton multiProjectSkeleton() {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT"),
                new ChainStep(2, StepType.HOW, "HOW"));
        List<ResumeClaim> claims = List.of(new ResumeClaim("c1", ClaimType.IMPLEMENTATION, Priority.HIGH));
        Project p1 = new Project("p_a", "주문 조회 캐싱 개선 프로젝트",
                List.of(), "", "", List.of(), claims,
                List.of(new InterrogationChain("Redis Cache-Aside", 0.9, steps)));
        Project p2 = new Project("p_b", "실시간 협업 보드 프로젝트",
                List.of(), "", "", List.of(), claims,
                List.of(new InterrogationChain("WebSocket Pub/Sub", 0.9, steps)));
        return new ResumeSkeleton("r_multi", "hash-multi", CandidateLevel.MID, "backend",
                List.of(p1, p2), Map.of());
    }
}
