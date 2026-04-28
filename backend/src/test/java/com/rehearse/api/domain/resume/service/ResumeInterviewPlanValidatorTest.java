package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

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
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ResumeInterviewPlanValidator - cross-aggregate invariant 검증")
class ResumeInterviewPlanValidatorTest {

    private ResumeInterviewPlanValidator validator;
    private ResumeSkeleton skeleton;

    @BeforeEach
    void setUp() {
        validator = new ResumeInterviewPlanValidator();
        skeleton = createSkeleton();
    }

    @Test
    @DisplayName("validate_정상통과_when_chain_and_claim_match_skeleton")
    void validate_passes_when_chain_and_claim_match_skeleton() {
        InterviewPlan plan = createPlan(
                List.of("p1_c1"),
                ChainReference.synthesizeChainId("p1", "Redis"),
                ChainReference.synthesizeChainId("p1", "JPA")
        );

        assertThatCode(() -> validator.validate(skeleton, plan)).doesNotThrowAnyException();
    }

    @Test
    @DisplayName("validate_ORPHAN_CHAIN_when_chain_id_not_in_skeleton")
    void validate_throws_orphan_chain_when_chain_not_in_skeleton() {
        InterviewPlan plan = createPlan(
                List.of("p1_c1"),
                ChainReference.synthesizeChainId("p1", "NonExistent"),
                ChainReference.synthesizeChainId("p1", "JPA")
        );

        assertThatThrownBy(() -> validator.validate(skeleton, plan))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.ORPHAN_CHAIN.getCode()));
    }

    @Test
    @DisplayName("validate_ORPHAN_CLAIM_when_claim_id_not_in_skeleton_project")
    void validate_throws_orphan_claim_when_claim_not_in_project() {
        InterviewPlan plan = createPlan(
                List.of("p1_c1", "p1_c_unknown"),
                ChainReference.synthesizeChainId("p1", "Redis"),
                ChainReference.synthesizeChainId("p1", "JPA")
        );

        assertThatThrownBy(() -> validator.validate(skeleton, plan))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> assertThat(((BusinessException) e).getCode())
                        .isEqualTo(ResumePlannerErrorCode.ORPHAN_CLAIM.getCode()));
    }

    @Test
    @DisplayName("validate_빈_claim_coverage_허용")
    void validate_allows_empty_claim_coverage() {
        InterviewPlan plan = createPlan(
                List.of(),
                ChainReference.synthesizeChainId("p1", "Redis"),
                ChainReference.synthesizeChainId("p1", "JPA")
        );

        assertThatCode(() -> validator.validate(skeleton, plan)).doesNotThrowAnyException();
    }

    private ResumeSkeleton createSkeleton() {
        List<ChainStep> steps = List.of(
                new ChainStep(1, StepType.WHAT, "WHAT"),
                new ChainStep(2, StepType.HOW, "HOW"),
                new ChainStep(3, StepType.WHY_MECH, "WHY"),
                new ChainStep(4, StepType.TRADEOFF, "T")
        );
        List<ResumeClaim> claims = List.of(
                new ResumeClaim("p1_c1", "claim", ClaimType.IMPLEMENTATION, Priority.HIGH, List.of())
        );
        List<InterrogationChain> chains = List.of(
                new InterrogationChain("Redis", 0.9, steps),
                new InterrogationChain("JPA", 0.8, steps)
        );
        Project project = new Project("p1", claims, chains);
        return new ResumeSkeleton("r1", "h1", CandidateLevel.JUNIOR, "backend", List.of(project), Map.of());
    }

    private InterviewPlan createPlan(List<String> claimCoverage, String primaryChainId, String backupChainId) {
        ChainReference primary = new ChainReference(primaryChainId, "Redis", 1, List.of(1, 2));
        ChainReference backup = new ChainReference(backupChainId, "JPA", 2, List.of(1, 2));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(primary), List.of(backup));
        PlaygroundPhase playground = new PlaygroundPhase("opener", claimCoverage);
        ProjectPlan project = new ProjectPlan("p1", "Project A", 1, playground, interrogation);
        return new InterviewPlan("plan_test", 30, List.of(project));
    }
}
