package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import lombok.Getter;
import lombok.extern.slf4j.Slf4j;

import java.util.Collections;
import java.util.List;
import java.util.Optional;
import java.util.concurrent.CopyOnWriteArrayList;
import java.util.concurrent.locks.ReentrantLock;
import lombok.AccessLevel;

/**
 * 세션 내 Chain 진행 상태를 추적한다.
 * 인스턴스는 InterviewRuntimeState에 보관되므로 수명 = 세션 수명.
 * 동시성 보호: interviewId 단위 ReentrantLock (per-instance). 세션당 단일 인스턴스이므로
 * 동일 세션의 동시 요청이 있더라도 lock으로 직렬화된다.
 * QuestionGenerationLock 패턴 참조: 락 인스턴스를 컴포넌트 외부에 두지 않고 상태 객체 내부에 둬
 * 락과 상태의 생존 주기를 일치시킨다.
 */
@Slf4j
@Getter
public class ChainStateTracker {

    private static final int LEVEL_STAY_MAX_TURNS = 2;

    private final ReentrantLock lock = new ReentrantLock();

    private String currentProjectId;
    private String currentChainId;
    private int currentLevel;
    private int consecutiveLevelStayCount;
    @Getter(AccessLevel.NONE)
    private final List<String> completedChainIds = new CopyOnWriteArrayList<>();

    public ChainStateTracker() {
        this.currentLevel = 1;
        this.consecutiveLevelStayCount = 0;
    }

    public <T> T withLock(java.util.concurrent.Callable<T> action) {
        lock.lock();
        try {
            return action.call();
        } catch (RuntimeException e) {
            throw e;
        } catch (Exception e) {
            throw new IllegalStateException("ChainStateTracker lock action 실패", e);
        } finally {
            lock.unlock();
        }
    }

    public void initChain(String projectId, String chainId) {
        this.currentProjectId = projectId;
        this.currentChainId = chainId;
        this.currentLevel = 1;
        this.consecutiveLevelStayCount = 0;
        log.info("[ChainStateTracker] chain 초기화: projectId={}, chainId={}, level=1", projectId, chainId);
    }

    public void levelUp() {
        if (currentLevel >= 4) {
            return;
        }
        currentLevel++;
        consecutiveLevelStayCount = 0;
        log.info("[ChainStateTracker] LEVEL_UP → level={}, chainId={}", currentLevel, currentChainId);
    }

    public boolean levelStay() {
        consecutiveLevelStayCount++;
        log.info("[ChainStateTracker] LEVEL_STAY: consecutiveCount={}, chainId={}", consecutiveLevelStayCount, currentChainId);
        return consecutiveLevelStayCount > LEVEL_STAY_MAX_TURNS;
    }

    public void markChainComplete() {
        if (currentChainId != null) {
            completedChainIds.add(currentChainId);
            log.info("[ChainStateTracker] chain 완료: chainId={}", currentChainId);
        }
        currentChainId = null;
        currentLevel = 1;
        consecutiveLevelStayCount = 0;
    }

    public boolean isChainComplete() {
        return currentLevel > 4;
    }

    public boolean isCompleted(String chainId) {
        return completedChainIds.contains(chainId);
    }

    public Optional<ChainReference> resolveNextChain(List<ProjectPlan> projectPlans) {
        for (ProjectPlan plan : projectPlans) {
            InterrogationPhase phase = plan.interrogationPhase();
            for (ChainReference chain : phase.primaryChains()) {
                if (!isCompleted(chain.chainId())) {
                    return Optional.of(chain);
                }
            }
        }
        for (ProjectPlan plan : projectPlans) {
            InterrogationPhase phase = plan.interrogationPhase();
            for (ChainReference chain : phase.backupChains()) {
                if (!isCompleted(chain.chainId())) {
                    return Optional.of(chain);
                }
            }
        }
        return Optional.empty();
    }

    public boolean hasActiveChain() {
        return currentChainId != null;
    }

    public List<String> getCompletedChainIds() {
        return Collections.unmodifiableList(completedChainIds);
    }
}
