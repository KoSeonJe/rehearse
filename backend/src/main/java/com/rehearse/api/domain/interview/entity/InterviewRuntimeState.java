package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.AnswerAnalysis;
import com.rehearse.api.domain.resume.entity.ChainStateTracker;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.ResumeMode;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;

import java.time.Instant;
import java.util.ArrayList;
import java.util.Deque;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Getter
public class InterviewRuntimeState {

    // ConcurrentLinkedDeque preserves insertion order for recency trimming (L2 covered_claims_recent).
    // Deduplication is enforced via a companion set on add paths.
    private final Deque<String> coveredClaims;
    private final Set<String> coveredClaimsSet;
    private final List<Long> activeChain;
    private final String currentLevel;
    private final AtomicInteger playgroundTurns;
    private final Map<Long, TurnAnalysis> turnAnalysisCache;
    private volatile ResumeSkeleton resumeSkeletonCache;
    private volatile InterviewPlan interviewPlanCache;

    // Keyed by windowEnd index (exclusive upper bound of the older-turns window that was compacted).
    // windowEnd = exchanges.size() - RECENT_WINDOW, i.e. the count of turns fed to the compactor.
    private final Map<Integer, String> compactedDialogueSummaries = new ConcurrentHashMap<>();

    // Guards against duplicate async compaction submissions for the same windowEnd.
    private final Set<Integer> compactionInFlight = ConcurrentHashMap.newKeySet();

    private volatile Instant startedAt;
    private volatile ResumeMode resumeMode = ResumeMode.PLAYGROUND;
    private final ChainStateTracker chainStateTracker = new ChainStateTracker();
    private final AtomicInteger playgroundCumulativeLength = new AtomicInteger(0);

    public InterviewRuntimeState(String currentLevel, ResumeSkeleton resumeSkeletonCache) {
        coveredClaims = new ConcurrentLinkedDeque<>();
        coveredClaimsSet = ConcurrentHashMap.newKeySet();
        activeChain = new CopyOnWriteArrayList<>();
        playgroundTurns = new AtomicInteger(0);
        turnAnalysisCache = new ConcurrentHashMap<>();
        this.currentLevel = currentLevel;
        this.resumeSkeletonCache = resumeSkeletonCache;
    }

    public void setResumeSkeleton(ResumeSkeleton skeleton) {
        this.resumeSkeletonCache = skeleton;
    }

    public void setInterviewPlan(InterviewPlan plan) {
        this.interviewPlanCache = plan;
    }

    public void setStartedAt(Instant startedAt) {
        this.startedAt = startedAt;
    }

    public int addPlaygroundAnswerLength(int length) {
        return playgroundCumulativeLength.addAndGet(length);
    }

    public int getPlaygroundCumulativeLength() {
        return playgroundCumulativeLength.get();
    }

    public void transitionTo(ResumeMode newMode) {
        this.resumeMode = newMode;
    }

    public boolean addCoveredClaim(String claim) {
        if (claim == null || claim.isBlank()) {
            return false;
        }
        if (coveredClaimsSet.add(claim)) {
            coveredClaims.addLast(claim);
            return true;
        }
        return false;
    }

    public void recordAnalysis(Long turnId, TurnAnalysis analysis) {
        if (turnId == null || analysis == null) {
            throw new IllegalArgumentException("turnId/analysis 는 null 일 수 없습니다.");
        }
        turnAnalysisCache.put(turnId, analysis);
    }

    public Optional<AnswerAnalysis> getAnswerAnalysis(Long turnId) {
        if (turnId == null) {
            return Optional.empty();
        }
        TurnAnalysis cached = turnAnalysisCache.get(turnId);
        return cached instanceof AnswerAnalysis answerAnalysis
                ? Optional.of(answerAnalysis)
                : Optional.empty();
    }

    public SessionStateSnapshot toSessionStateSnapshot() {
        List<String> recentClaims = recentClaims(50);
        List<Long> chainSnapshot = List.copyOf(activeChain);
        // asked_perspectives cannot be derived from InterviewRuntimeState: AnswerAnalysis stores
        // missingPerspectives (what was NOT asked), not selectedPerspective (what was chosen by the
        // generator). The selectedPerspective lives only in FollowUpRequest.FollowUpExchange
        // (a request-time DTO). L3/L4 layers receive previousExchanges directly and should derive
        // asked_perspectives there.
        return new SessionStateSnapshot(
                currentLevel,
                playgroundTurns.get(),
                recentClaims,
                chainSnapshot,
                List.of()
        );
    }

    public Optional<String> getCompactedSummary(int windowEnd) {
        return Optional.ofNullable(compactedDialogueSummaries.get(windowEnd));
    }

    public void putCompactedSummary(int windowEnd, String summary) {
        compactedDialogueSummaries.put(windowEnd, summary);
    }

    public boolean hasCompactionInFlight(int windowEnd) {
        return compactionInFlight.contains(windowEnd);
    }

    // Atomic check-and-set: returns true if this caller acquired the in-flight slot,
    // false if another caller already started compaction for the same windowEnd.
    // Use this from concurrent producers (e.g. DialogueCompactor.compactAsync) to avoid
    // TOCTOU race against hasCompactionInFlight + markCompactionStarted.
    public boolean tryStartCompaction(int windowEnd) {
        return compactionInFlight.add(windowEnd);
    }

    public void markCompactionStarted(int windowEnd) {
        compactionInFlight.add(windowEnd);
    }

    public void markCompactionFinished(int windowEnd) {
        compactionInFlight.remove(windowEnd);
    }

    private List<String> recentClaims(int maxCount) {
        int size = coveredClaims.size();
        if (size <= maxCount) {
            return new ArrayList<>(coveredClaims);
        }
        List<String> result = new ArrayList<>(maxCount);
        Iterator<String> it = coveredClaims.descendingIterator();
        for (int i = 0; i < maxCount && it.hasNext(); i++) {
            result.add(0, it.next());
        }
        return result;
    }
}
