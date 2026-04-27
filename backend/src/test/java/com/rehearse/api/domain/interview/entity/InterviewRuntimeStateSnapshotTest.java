package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.interview.Claim;
import com.rehearse.api.domain.interview.EvidenceStrength;
import com.rehearse.api.domain.interview.RecommendedNextAction;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewRuntimeState - toSessionStateSnapshot()")
class InterviewRuntimeStateSnapshotTest {

    private InterviewRuntimeState freshState(String level) {
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash", ResumeSkeleton.CandidateLevel.MID, "backend", List.of(), null);
        return new InterviewRuntimeState(level, skeleton);
    }

    private AnswerAnalysis sampleAnalysis(long turnId) {
        return new AnswerAnalysis(
                turnId,
                List.of(new Claim("claim", 3, EvidenceStrength.WEAK, "tag")),
                List.of(),
                List.of(),
                3,
                RecommendedNextAction.DEEP_DIVE
        );
    }

    @Test
    @DisplayName("snapshot_reflects_current_turn_count")
    void snapshot_reflects_current_turn_count() {
        InterviewRuntimeState state = freshState("MID");
        state.getPlaygroundTurns().set(4);

        SessionStateSnapshot snapshot = state.toSessionStateSnapshot();

        assertThat(snapshot.currentTurn()).isEqualTo(4);
        assertThat(snapshot.level()).isEqualTo("MID");
    }

    @Test
    @DisplayName("snapshot_includes_active_chain_when_resume_track")
    void snapshot_includes_active_chain_when_resume_track() {
        InterviewRuntimeState state = freshState("SENIOR");
        state.getActiveChain().add(101L);
        state.getActiveChain().add(102L);

        SessionStateSnapshot snapshot = state.toSessionStateSnapshot();

        assertThat(snapshot.activeChain()).containsExactly(101L, 102L);
    }

    @Test
    @DisplayName("snapshot_returns_empty_perspectives_when_cache_empty")
    void snapshot_returns_empty_perspectives_when_cache_empty() {
        InterviewRuntimeState state = freshState("JUNIOR");
        state.recordAnalysis(1L, sampleAnalysis(1L));

        SessionStateSnapshot snapshot = state.toSessionStateSnapshot();

        assertThat(snapshot.askedPerspectives()).isEmpty();
    }

    @Test
    @DisplayName("snapshot_covered_claims_recent_trimmed_to_50_when_over_100")
    void snapshot_covered_claims_recent_trimmed_to_50_when_over_100() {
        InterviewRuntimeState state = freshState("MID");
        for (int i = 0; i < 120; i++) {
            state.addCoveredClaim("claim-" + i);
        }

        SessionStateSnapshot snapshot = state.toSessionStateSnapshot();

        assertThat(snapshot.coveredClaimsRecent()).hasSize(50);
        // insertion order preserved — most recent 50 are claim-70 through claim-119
        assertThat(snapshot.coveredClaimsRecent().get(0)).isEqualTo("claim-70");
        assertThat(snapshot.coveredClaimsRecent().get(49)).isEqualTo("claim-119");
    }

    @Test
    @DisplayName("snapshot_covered_claims_recent_includes_all_when_under_100")
    void snapshot_covered_claims_recent_includes_all_when_under_100() {
        InterviewRuntimeState state = freshState("MID");
        state.addCoveredClaim("alpha");
        state.addCoveredClaim("beta");
        state.addCoveredClaim("gamma");

        SessionStateSnapshot snapshot = state.toSessionStateSnapshot();

        assertThat(snapshot.coveredClaimsRecent()).containsExactly("alpha", "beta", "gamma");
    }

    @Test
    @DisplayName("snapshot_deduplicates_covered_claims")
    void snapshot_deduplicates_covered_claims() {
        InterviewRuntimeState state = freshState("MID");
        state.addCoveredClaim("alpha");
        boolean second = state.addCoveredClaim("alpha");

        assertThat(second).isFalse();
        assertThat(state.toSessionStateSnapshot().coveredClaimsRecent()).containsExactly("alpha");
    }
}
