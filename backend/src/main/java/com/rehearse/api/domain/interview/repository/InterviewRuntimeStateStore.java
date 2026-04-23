package com.rehearse.api.domain.interview.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;

@Component
public class InterviewRuntimeStateStore {

    private final Cache<Long, InterviewRuntimeState> cache;

    public InterviewRuntimeStateStore(Cache<Long, InterviewRuntimeState> cache) {
        this.cache = cache;
    }

    public InterviewRuntimeState get(Long interviewId) {
        InterviewRuntimeState state = cache.getIfPresent(interviewId);
        if (state == null) {
            throw new IllegalStateException(
                    "InterviewRuntimeState not initialized for interviewId=" + interviewId);
        }
        return state;
    }

    public void update(Long interviewId, Consumer<InterviewRuntimeState> mutator) {
        cache.asMap().compute(interviewId, (id, existing) -> {
            if (existing == null) {
                throw new IllegalStateException(
                        "InterviewRuntimeState not initialized for interviewId=" + id);
            }
            mutator.accept(existing);
            return existing;
        });
    }

    public void evict(Long interviewId) {
        cache.invalidate(interviewId);
    }
}
