package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import ch.qos.logback.classic.Level;
import ch.qos.logback.classic.Logger;
import ch.qos.logback.classic.spi.ILoggingEvent;
import ch.qos.logback.core.read.ListAppender;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.ChainStep;
import com.rehearse.api.domain.resume.entity.ClaimType;
import com.rehearse.api.domain.resume.entity.InterrogationChain;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.Priority;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeClaim;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.StepType;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import java.util.List;
import java.util.Map;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.slf4j.LoggerFactory;

@DisplayName("ResumeInterviewPlanValidator - cross-aggregate invariant 검증")
class ResumeInterviewPlanValidatorTest {

    private static final String CLAIM_TEXT = "Redis Cache-Aside 도입으로 응답 시간 50% 단축";

    private ResumeInterviewPlanValidator validator;
    private ResumeSkeleton skeleton;
    private ListAppender<ILoggingEvent> logAppender;

    @BeforeEach
    void setUp() {
        validator = new ResumeInterviewPlanValidator();
        skeleton = createSkeleton();
        Logger logger = (Logger) LoggerFactory.getLogger(ResumeInterviewPlanValidator.class);
        logAppender = new ListAppender<>();
        logAppender.start();
        logger.addAppender(logAppender);
    }

    @AfterEach
    void tearDown() {
        Logger logger = (Logger) LoggerFactory.getLogger(ResumeInterviewPlanValidator.class);
        logger.detachAppender(logAppender);
    }

    @Nested
    @DisplayName("정상 통과")
    class HappyPath {

        @Test
        @DisplayName("chain_id 와 claim text 가 모두 skeleton 에 매칭되면 통과한다")
        void validate_passes_when_chain_and_claim_match_skeleton() {
            InterviewPlan plan = createPlan(
                    List.of(CLAIM_TEXT),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            assertThatCode(() -> validator.validate(skeleton, plan)).doesNotThrowAnyException();
            assertThat(droppedWarnCount()).isZero();
        }

        @Test
        @DisplayName("expectedClaimsCoverage 가 비어 있어도 통과한다")
        void validate_allows_empty_claim_coverage() {
            InterviewPlan plan = createPlan(
                    List.of(),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            assertThatCode(() -> validator.validate(skeleton, plan)).doesNotThrowAnyException();
            assertThat(droppedWarnCount()).isZero();
        }
    }

    @Nested
    @DisplayName("ORPHAN_CHAIN hard-fail")
    class OrphanChain {

        @Test
        @DisplayName("chain_id 가 skeleton 에 없으면 ORPHAN_CHAIN 으로 실패한다")
        void validate_throws_orphan_chain_when_chain_not_in_skeleton() {
            InterviewPlan plan = createPlan(
                    List.of(CLAIM_TEXT),
                    ChainReference.synthesizeChainId("p1", "NonExistent"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            assertThatThrownBy(() -> validator.validate(skeleton, plan))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> assertThat(((BusinessException) e).getCode())
                            .isEqualTo(ResumePlannerErrorCode.ORPHAN_CHAIN.getCode()));
        }
    }

    @Nested
    @DisplayName("claim coverage graceful drop")
    class GracefulDrop {

        @Test
        @DisplayName("정확 일치 claim text 는 매칭으로 통과한다")
        void match_exact_text() {
            InterviewPlan plan = createPlan(
                    List.of(CLAIM_TEXT),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            validator.validate(skeleton, plan);
            assertThat(droppedWarnCount()).isZero();
        }

        @Test
        @DisplayName("substring 일치 claim text 는 매칭으로 통과한다")
        void match_substring_text() {
            InterviewPlan plan = createPlan(
                    List.of("Cache-Aside 도입으로 응답 시간 50% 단축"),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            validator.validate(skeleton, plan);
            assertThat(droppedWarnCount()).isZero();
        }

        @Test
        @DisplayName("Jaccard 유사도 0.6 이상이면 매칭으로 통과한다")
        void match_jaccard_similar_text() {
            InterviewPlan plan = createPlan(
                    List.of("Redis Cache-Aside 도입으로 응답 시간 단축"),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            validator.validate(skeleton, plan);
            assertThat(droppedWarnCount()).isZero();
        }

        @Test
        @DisplayName("매칭 실패한 claim 은 drop 하고 WARN 로그를 남긴다 (예외 전파 X)")
        void drop_unmatched_claim_with_warn_log() {
            InterviewPlan plan = createPlan(
                    List.of(CLAIM_TEXT, "전혀 관련 없는 자유 작성 항목"),
                    ChainReference.synthesizeChainId("p1", "Redis"),
                    ChainReference.synthesizeChainId("p1", "JPA")
            );

            assertThatCode(() -> validator.validate(skeleton, plan)).doesNotThrowAnyException();
            assertThat(droppedClaimCountFromLog()).isEqualTo(1);
        }
    }

    private long droppedWarnCount() {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .filter(e -> e.getFormattedMessage().contains("graceful drop"))
                .count();
    }

    private int droppedClaimCountFromLog() {
        return logAppender.list.stream()
                .filter(e -> e.getLevel() == Level.WARN)
                .filter(e -> e.getFormattedMessage().contains("graceful drop"))
                .map(ILoggingEvent::getFormattedMessage)
                .map(msg -> msg.replaceAll(".*droppedClaimCount=(\\d+).*", "$1"))
                .map(Integer::parseInt)
                .findFirst()
                .orElse(0);
    }

    private ResumeSkeleton createSkeleton() {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT"),
                new ChainStep(2, StepType.HOW, "HOW"),
                new ChainStep(3, StepType.WHY_MECH, "WHY"),
                new ChainStep(4, StepType.TRADEOFF, "T")
        );
        List<ResumeClaim> claims = List.of(
                new ResumeClaim(CLAIM_TEXT, ClaimType.IMPACT_METRIC, Priority.HIGH)
        );
        List<InterrogationChain> chains = List.of(
                new InterrogationChain("Redis", 0.9, steps),
                new InterrogationChain("JPA", 0.8, steps)
        );
        Project project = new Project("p1", "프로젝트 A",
                List.of(), "", "", List.of(), claims, chains);
        return new ResumeSkeleton("r1", "h1", CandidateLevel.JUNIOR, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createPlan(List<String> claimCoverage, String primaryChainId, String backupChainId) {
        ChainReference primary = new ChainReference(primaryChainId, "Redis", 1, List.of(1, 2));
        ChainReference backup = new ChainReference(backupChainId, "JPA", 2, List.of(1, 2));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(primary), List.of(backup));
        PlaygroundPhase playground = new PlaygroundPhase("opener", claimCoverage);
        ProjectPlan project = new ProjectPlan("p1", "Project A", 1, playground, interrogation);
        return new InterviewPlan("plan_test", List.of(project));
    }
}
