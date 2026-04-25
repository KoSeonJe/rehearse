package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.AnswerAnalysis;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;
import lombok.Getter;

@Getter
public class InterviewRuntimeState {

    private final Set<String> coveredClaims;
    private final List<Long> activeChain;
    private final String currentLevel;
    private final AtomicInteger playgroundTurns;
    private final Map<Long, TurnAnalysis> turnAnalysisCache;
    private final CachedResumeSkeleton resumeSkeletonCache;

    public InterviewRuntimeState(String currentLevel, CachedResumeSkeleton resumeSkeletonCache) {
        coveredClaims = Collections.newSetFromMap(new ConcurrentHashMap<>());
        activeChain = new CopyOnWriteArrayList<>();
        playgroundTurns = new AtomicInteger(0);
        turnAnalysisCache = new ConcurrentHashMap<>();
        this.currentLevel = currentLevel;
        this.resumeSkeletonCache = resumeSkeletonCache;
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
}
