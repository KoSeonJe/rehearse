package com.rehearse.api.domain.interview.repository;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("InterviewRuntimeStateStore - 런타임 상태 저장소")
class InterviewRuntimeStateStoreTest {

    private Cache<Long, InterviewRuntimeState> cache;
    private InterviewRuntimeStateStore store;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().<Long, InterviewRuntimeState>build();
        store = new InterviewRuntimeStateStore(cache);
    }

    @Test
    @DisplayName("get_returns_state_when_present_in_cache")
    void get_returns_state_when_present_in_cache() {
        InterviewRuntimeState seeded = newState("JUNIOR");
        cache.put(1L, seeded);

        InterviewRuntimeState state = store.get(1L);

        assertThat(state).isSameAs(seeded);
    }

    @Test
    @DisplayName("get_throws_IllegalStateException_when_absent")
    void get_throws_IllegalStateException_when_absent() {
        assertThatThrownBy(() -> store.get(999L))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("999");
    }

    @Test
    @DisplayName("update_mutates_state_when_present")
    void update_mutates_state_when_present() {
        cache.put(10L, newState("JUNIOR"));

        store.update(10L, state -> state.getCoveredClaims().add("spring-ioc"));

        assertThat(store.get(10L).getCoveredClaims()).containsExactly("spring-ioc");
    }

    @Test
    @DisplayName("update_throws_IllegalStateException_when_absent")
    void update_throws_IllegalStateException_when_absent() {
        assertThatThrownBy(() -> store.update(42L, state -> state.getCoveredClaims().add("x")))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("42");
    }

    @Test
    @DisplayName("update_does_not_invoke_mutator_when_state_absent")
    void update_does_not_invoke_mutator_when_state_absent() {
        AtomicInteger mutatorCalls = new AtomicInteger(0);

        assertThatThrownBy(() -> store.update(7L, state -> mutatorCalls.incrementAndGet()))
                .isInstanceOf(IllegalStateException.class);

        assertThat(mutatorCalls.get()).isZero();
    }

    @Test
    @DisplayName("evict_removes_entry_and_subsequent_get_throws")
    void evict_removes_entry_and_subsequent_get_throws() {
        cache.put(99L, newState("SENIOR"));

        store.evict(99L);

        assertThatThrownBy(() -> store.get(99L))
                .isInstanceOf(IllegalStateException.class);
    }

    @Test
    @DisplayName("update_concurrent_increments_accumulate_correctly_when_50_threads_increment_counter")
    void update_concurrent_increments_accumulate_correctly() throws Exception {
        int threadCount = 50;
        Long interviewId = 77L;
        cache.put(interviewId, newState("JUNIOR"));

        CountDownLatch start = new CountDownLatch(1);
        CountDownLatch done = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    start.await();
                    store.update(interviewId, state -> state.getPlaygroundTurns().incrementAndGet());
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    done.countDown();
                }
            });
        }
        start.countDown();
        done.await();
        executor.shutdown();

        assertThat(store.get(interviewId).getPlaygroundTurns().get()).isEqualTo(threadCount);
    }

    @Test
    @DisplayName("different_interviewIds_maintain_independent_states")
    void different_interviewIds_maintain_independent_states() {
        cache.put(1L, newState("JUNIOR"));
        cache.put(2L, newState("SENIOR"));

        store.update(1L, state -> state.getCoveredClaims().add("claim-1"));
        store.update(2L, state -> state.getCoveredClaims().add("claim-2"));

        assertThat(store.get(1L).getCurrentLevel()).isEqualTo("JUNIOR");
        assertThat(store.get(1L).getCoveredClaims()).containsExactly("claim-1");
        assertThat(store.get(2L).getCurrentLevel()).isEqualTo("SENIOR");
        assertThat(store.get(2L).getCoveredClaims()).containsExactly("claim-2");
    }

    private InterviewRuntimeState newState(String level) {
        ResumeSkeleton skeleton = new ResumeSkeleton("r1", "hash-" + level, ResumeSkeleton.CandidateLevel.MID, "backend", List.of(), null);
        return new InterviewRuntimeState(level, skeleton);
    }
}
