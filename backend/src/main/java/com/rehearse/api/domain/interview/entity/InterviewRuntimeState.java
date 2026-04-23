package com.rehearse.api.domain.interview.entity;

import java.util.Collections;
import java.util.List;
import java.util.Map;
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
}
