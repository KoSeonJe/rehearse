package com.rehearse.api.domain.interview.service;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewRuntimeStateCache.getOrInit - 첫 진입 시 lazy 초기화")
class InterviewRuntimeStateCacheGetOrInitTest {

    private Cache<Long, InterviewRuntimeState> cache;
    private InterviewRuntimeStateCache store;

    @BeforeEach
    void setUp() {
        cache = Caffeine.newBuilder().<Long, InterviewRuntimeState>build();
        store = new InterviewRuntimeStateCache(cache);
    }

    @Test
    @DisplayName("returns_existing_state_when_present")
    void returns_existing_state_when_present() {
        InterviewRuntimeState seeded = new InterviewRuntimeState("MID", null);
        cache.put(7L, seeded);

        InterviewRuntimeState result = store.getOrInit(7L, () -> new InterviewRuntimeState("JUNIOR", null));

        assertThat(result).isSameAs(seeded);
        assertThat(result.getCurrentLevel()).isEqualTo("MID");
    }

    @Test
    @DisplayName("creates_new_state_when_absent")
    void creates_new_state_when_absent() {
        InterviewRuntimeState result = store.getOrInit(42L, () -> new InterviewRuntimeState("SENIOR", null));

        assertThat(result.getCurrentLevel()).isEqualTo("SENIOR");
        assertThat(cache.getIfPresent(42L)).isSameAs(result);
    }

    @Test
    @DisplayName("does_not_call_supplier_when_state_already_present")
    void does_not_call_supplier_when_state_already_present() {
        cache.put(3L, new InterviewRuntimeState("JUNIOR", null));
        AtomicInteger supplierCallCount = new AtomicInteger(0);

        store.getOrInit(3L, () -> {
            supplierCallCount.incrementAndGet();
            return new InterviewRuntimeState("MID", null);
        });

        assertThat(supplierCallCount.get()).isZero();
    }

    @Test
    @DisplayName("invokes_supplier_exactly_once_under_concurrent_init")
    void invokes_supplier_exactly_once_under_concurrent_init() throws InterruptedException {
        int threadCount = 100;
        AtomicInteger supplierCallCount = new AtomicInteger(0);
        CountDownLatch startGate = new CountDownLatch(1);
        CountDownLatch finishGate = new CountDownLatch(threadCount);
        ExecutorService pool = Executors.newFixedThreadPool(20);

        try {
            for (int i = 0; i < threadCount; i++) {
                pool.submit(() -> {
                    try {
                        startGate.await();
                        store.getOrInit(99L, () -> {
                            supplierCallCount.incrementAndGet();
                            return new InterviewRuntimeState("MID", null);
                        });
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    } finally {
                        finishGate.countDown();
                    }
                });
            }

            startGate.countDown();
            assertThat(finishGate.await(5, TimeUnit.SECONDS)).isTrue();
        } finally {
            pool.shutdownNow();
        }

        assertThat(supplierCallCount.get()).isOne();
    }
}
