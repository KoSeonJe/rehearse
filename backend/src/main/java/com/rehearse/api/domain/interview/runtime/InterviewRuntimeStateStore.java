package com.rehearse.api.domain.interview.runtime;

import com.github.benmanes.caffeine.cache.Cache;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

// update() 는 Caffeine.asMap().compute() 로 동일 interviewId 에 대한 read-modify-write 를 직렬화한다.
@Component
public class InterviewRuntimeStateStore {

    private final Cache<Long, InterviewRuntimeState> cache;

    public InterviewRuntimeStateStore(Cache<Long, InterviewRuntimeState> cache) {
        this.cache = cache;
    }

    public InterviewRuntimeState getOrInit(Long interviewId, Supplier<InterviewRuntimeState> init) {
        return cache.get(interviewId, id -> init.get());
    }

    public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
        cache.asMap().compute(interviewId, (id, existing) -> {
            InterviewRuntimeState state = (existing != null) ? existing : new InterviewRuntimeState();
            mutator.accept(state);
            return state;
        });
    }

    public void evict(Long interviewId) {
        cache.invalidate(interviewId);
    }
}
