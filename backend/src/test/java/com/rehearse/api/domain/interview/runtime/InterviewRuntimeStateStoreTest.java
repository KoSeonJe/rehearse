package com.rehearse.api.domain.interview.runtime;

import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.Future;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewRuntimeStateStore - 런타임 상태 저장소")
class InterviewRuntimeStateStoreTest {

    private InterviewRuntimeStateStore store;

    @BeforeEach
    void setUp() {
        store = new InterviewRuntimeStateStore(new SimpleMeterRegistry());
        store.init();
    }

    @Test
    @DisplayName("getOrInit_초기값_반환_when_새로운_인터뷰ID")
    void getOrInit_returnsNewState_when_interviewIdNotPresent() {
        InterviewRuntimeState state = store.getOrInit(1L, InterviewRuntimeState::new);

        assertThat(state).isNotNull();
        assertThat(state.getCoveredClaims()).isEmpty();
        assertThat(state.getActiveChain()).isEmpty();
        assertThat(state.getPlaygroundTurns()).isZero();
    }

    @Test
    @DisplayName("getOrInit_동일_인스턴스_반환_when_같은_인터뷰ID_재요청")
    void getOrInit_returnsSameInstance_when_sameInterviewIdRequested() {
        InterviewRuntimeState first = store.getOrInit(1L, InterviewRuntimeState::new);
        InterviewRuntimeState second = store.getOrInit(1L, InterviewRuntimeState::new);

        assertThat(first).isSameAs(second);
    }

    @Test
    @DisplayName("update_상태_변경_반영_when_mutator_적용")
    void update_appliesMutation_when_mutatorProvided() {
        store.getOrInit(10L, InterviewRuntimeState::new);

        store.update(10L, state -> {
            state.getCoveredClaims().add("spring-ioc");
            state.setCurrentLevel("JUNIOR");
        });

        InterviewRuntimeState result = store.getOrInit(10L, InterviewRuntimeState::new);
        assertThat(result.getCoveredClaims()).containsExactly("spring-ioc");
        assertThat(result.getCurrentLevel()).isEqualTo("JUNIOR");
    }

    @Test
    @DisplayName("evict_캐시제거_when_세션종료")
    void evict_removesState_when_called() {
        store.getOrInit(99L, InterviewRuntimeState::new);
        store.update(99L, state -> state.setCurrentLevel("SENIOR"));

        store.evict(99L);

        InterviewRuntimeState fresh = store.getOrInit(99L, InterviewRuntimeState::new);
        assertThat(fresh.getCurrentLevel()).isNull();
    }

    @Test
    @DisplayName("update_race_condition_없음_when_100스레드_동시_update")
    void update_noRaceCondition_when_100ThreadsConcurrentlyUpdate() throws Exception {
        int threadCount = 100;
        Long interviewId = 42L;
        store.getOrInit(interviewId, InterviewRuntimeState::new);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < threadCount; i++) {
            final String claim = "claim-" + i;
            futures.add(executor.submit(() -> {
                try {
                    startLatch.await();
                    store.update(interviewId, state -> state.getCoveredClaims().add(claim));
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            }));
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        for (Future<?> f : futures) {
            f.get();
        }

        InterviewRuntimeState finalState = store.getOrInit(interviewId, InterviewRuntimeState::new);
        assertThat(finalState.getCoveredClaims()).hasSize(threadCount);
    }

    @Test
    @DisplayName("stats_hitCount_증가_when_캐시히트_발생")
    void stats_hitsIncrement_when_cacheHitOccurs() {
        store.getOrInit(7L, InterviewRuntimeState::new);

        long hitsBefore = store.stats().hitCount();
        store.getOrInit(7L, InterviewRuntimeState::new);
        long hitsAfter = store.stats().hitCount();

        assertThat(hitsAfter).isGreaterThan(hitsBefore);
    }

    @Test
    @DisplayName("서로_다른_인터뷰ID_독립_상태_유지_when_별도_세션")
    void getOrInit_independentStates_when_differentInterviewIds() {
        store.update(1L, state -> state.setCurrentLevel("JUNIOR"));
        store.update(2L, state -> state.setCurrentLevel("SENIOR"));

        assertThat(store.getOrInit(1L, InterviewRuntimeState::new).getCurrentLevel()).isEqualTo("JUNIOR");
        assertThat(store.getOrInit(2L, InterviewRuntimeState::new).getCurrentLevel()).isEqualTo("SENIOR");
    }
}
