package com.rehearse.api.domain.interview.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.stats.CacheStats;
import com.rehearse.api.domain.interview.lock.InterviewLockService;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * 면접 세션 런타임 상태 저장소.
 *
 * <p>동일 {@code interviewId} 에 대한 {@link #update} 실행은
 * {@link InterviewLockService} 로 직렬화된다.
 * 호출자는 별도 락 없이 {@code store.update()} 만 호출하면 된다.
 */
@Component
public class InterviewRuntimeStateStore {

    private final Cache<Long, InterviewRuntimeState> cache;
    private final InterviewLockService lockService;

    public InterviewRuntimeStateStore(Cache<Long, InterviewRuntimeState> cache,
                                      InterviewLockService lockService) {
        this.cache = cache;
        this.lockService = lockService;
    }

    public InterviewRuntimeState getOrInit(Long interviewId, Supplier<InterviewRuntimeState> init) {
        return cache.get(interviewId, id -> init.get());
    }

    public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
        lockService.withLock(interviewId, () -> {
            InterviewRuntimeState state = cache.get(interviewId, id -> new InterviewRuntimeState());
            mutator.accept(state);
            return null;
        });
    }

    public void evict(Long interviewId) {
        cache.invalidate(interviewId);
    }

    CacheStats stats() {
        return cache.stats();
    }
}
