package com.rehearse.api.domain.interview.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import org.springframework.stereotype.Component;

import java.util.function.Consumer;
import java.util.function.Supplier;

@Component
public class InterviewRuntimeStateStore {

    private final Cache<Long, InterviewRuntimeState> cache;

    public InterviewRuntimeStateStore(Cache<Long, InterviewRuntimeState> cache) {
        this.cache = cache;
    }

    // 첫 진입 시 init, 재진입 시 기존 상태 반환. computeIfAbsent 가 atomic 이라
    // 동시 호출 시 supplier 1회만 실행. plan-00c 가 명세했으나 미구현으로 남았던 API.
    public InterviewRuntimeState getOrInit(Long interviewId, Supplier<InterviewRuntimeState> initializer) {
        return cache.asMap().computeIfAbsent(interviewId, id -> initializer.get());
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
