package com.rehearse.api.domain.interview.lock;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewLockService - 면접 동시성 제어")
class InterviewLockServiceTest {

    private InterviewLockService lockService;

    @BeforeEach
    void setUp() {
        lockService = new InterviewLockService();
    }

    @Test
    @DisplayName("withLock_결과값_반환_when_정상_실행")
    void withLock_returnsResult_when_actionSucceeds() {
        String result = lockService.withLock(1L, () -> "interview-result");

        assertThat(result).isEqualTo("interview-result");
    }

    @Test
    @DisplayName("withLock_직렬화_when_동일_interviewId_동시_접근")
    void withLock_serializes_when_sameInterviewIdConcurrentAccess() throws Exception {
        int threadCount = 50;
        Long interviewId = 10L;
        List<Integer> executionOrder = Collections.synchronizedList(new ArrayList<>());
        AtomicInteger counter = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            executor.submit(() -> {
                try {
                    startLatch.await();
                    lockService.withLock(interviewId, () -> {
                        int current = counter.get();
                        executionOrder.add(current);
                        counter.incrementAndGet();
                        return null;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(executionOrder).hasSize(threadCount);
        for (int i = 0; i < executionOrder.size() - 1; i++) {
            assertThat(executionOrder.get(i + 1)).isEqualTo(executionOrder.get(i) + 1);
        }
    }

    @Test
    @DisplayName("withLock_병렬_실행_when_서로_다른_interviewId")
    void withLock_allowsParallelExecution_when_differentInterviewIds() throws Exception {
        int threadCount = 20;
        AtomicInteger concurrentCount = new AtomicInteger(0);
        AtomicInteger maxConcurrent = new AtomicInteger(0);

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(threadCount);
        ExecutorService executor = Executors.newFixedThreadPool(threadCount);

        for (int i = 0; i < threadCount; i++) {
            final long differentId = (long) i;
            executor.submit(() -> {
                try {
                    startLatch.await();
                    lockService.withLock(differentId, () -> {
                        int current = concurrentCount.incrementAndGet();
                        maxConcurrent.updateAndGet(max -> Math.max(max, current));
                        try { Thread.sleep(20); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                        concurrentCount.decrementAndGet();
                        return null;
                    });
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                } finally {
                    doneLatch.countDown();
                }
            });
        }

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(maxConcurrent.get()).isGreaterThan(1);
    }

    @Test
    @DisplayName("withLock_예외전파_when_action_예외발생_후_락_해제")
    void withLock_releasesLock_when_actionThrowsException() {
        Long interviewId = 5L;

        try {
            lockService.withLock(interviewId, () -> {
                throw new RuntimeException("업스트림 오류");
            });
        } catch (RuntimeException ignored) {
        }

        String result = lockService.withLock(interviewId, () -> "락 해제됨");
        assertThat(result).isEqualTo("락 해제됨");
    }

    @Test
    @DisplayName("stripe_해시_충돌_시에도_직렬화_보장_when_동일stripe_다른ID")
    void withLock_stillSerializes_when_differentIdsMappedToSameStripe() throws Exception {
        // 256 stripe에서 interviewId 0과 256은 동일 stripe로 매핑됨
        Long id1 = 0L;
        Long id2 = 256L;
        List<String> log = Collections.synchronizedList(new ArrayList<>());

        CountDownLatch startLatch = new CountDownLatch(1);
        CountDownLatch doneLatch = new CountDownLatch(2);
        ExecutorService executor = Executors.newFixedThreadPool(2);

        executor.submit(() -> {
            try {
                startLatch.await();
                lockService.withLock(id1, () -> {
                    log.add("start-0");
                    try { Thread.sleep(30); } catch (InterruptedException ie) { Thread.currentThread().interrupt(); }
                    log.add("end-0");
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        executor.submit(() -> {
            try {
                startLatch.await();
                Thread.sleep(5);
                lockService.withLock(id2, () -> {
                    log.add("start-256");
                    log.add("end-256");
                    return null;
                });
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            } finally {
                doneLatch.countDown();
            }
        });

        startLatch.countDown();
        doneLatch.await();
        executor.shutdown();

        assertThat(log).containsExactly("start-0", "end-0", "start-256", "end-256");
    }
}
