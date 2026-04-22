package com.rehearse.api.domain.interview.runtime;

import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.atomic.AtomicInteger;

public class InterviewRuntimeState {

    private final Set<String> coveredClaims = Collections.newSetFromMap(new ConcurrentHashMap<>());
    private final List<Long> activeChain = new CopyOnWriteArrayList<>();
    private volatile String currentLevel;
    private final AtomicInteger playgroundTurns = new AtomicInteger(0);
    private final Map<Long, TurnAnalysis> turnAnalysisCache = new ConcurrentHashMap<>();
    private volatile CachedResumeSkeleton resumeSkeletonCache;

    public Set<String> getCoveredClaims() {
        return coveredClaims;
    }

    public List<Long> getActiveChain() {
        return activeChain;
    }

    public String getCurrentLevel() {
        return currentLevel;
    }

    public void setCurrentLevel(String currentLevel) {
        this.currentLevel = currentLevel;
    }

    public int getPlaygroundTurns() {
        return playgroundTurns.get();
    }

    public void incrementPlaygroundTurns() {
        playgroundTurns.incrementAndGet();
    }

    public Map<Long, TurnAnalysis> getTurnAnalysisCache() {
        return turnAnalysisCache;
    }

    public CachedResumeSkeleton getResumeSkeletonCache() {
        return resumeSkeletonCache;
    }

    public void setResumeSkeletonCache(CachedResumeSkeleton resumeSkeletonCache) {
        this.resumeSkeletonCache = resumeSkeletonCache;
    }
}
