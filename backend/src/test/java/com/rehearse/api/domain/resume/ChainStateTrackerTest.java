package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.domain.ChainReference;
import com.rehearse.api.domain.resume.domain.InterrogationPhase;
import com.rehearse.api.domain.resume.domain.ProjectPlan;
import com.rehearse.api.domain.resume.domain.PlaygroundPhase;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.*;

@DisplayName("ChainStateTracker - 세션 내 Chain 진행 상태 관리")
class ChainStateTrackerTest {

    private ChainStateTracker tracker;

    @BeforeEach
    void setUp() {
        tracker = new ChainStateTracker();
    }

    @Nested
    @DisplayName("Chain 레벨 전이")
    class LevelTransition {

        @Test
        @DisplayName("초기 상태는 level=1, consecutiveLevelStayCount=0 이다")
        void initialState_level1() {
            assertThat(tracker.getCurrentLevel()).isEqualTo(1);
            assertThat(tracker.getConsecutiveLevelStayCount()).isEqualTo(0);
            assertThat(tracker.hasActiveChain()).isFalse();
        }

        @Test
        @DisplayName("initChain 호출 시 chain 상태가 초기화된다")
        void initChain_resetsState() {
            tracker.initChain("proj1", "proj1::redis");

            assertThat(tracker.getCurrentProjectId()).isEqualTo("proj1");
            assertThat(tracker.getCurrentChainId()).isEqualTo("proj1::redis");
            assertThat(tracker.getCurrentLevel()).isEqualTo(1);
            assertThat(tracker.hasActiveChain()).isTrue();
        }

        @Test
        @DisplayName("levelUp 연속 호출 시 L1→L2→L3→L4 순서로 전이된다")
        void levelUp_L1_to_L4_sequence() {
            tracker.initChain("proj1", "proj1::redis");

            tracker.levelUp();
            assertThat(tracker.getCurrentLevel()).isEqualTo(2);

            tracker.levelUp();
            assertThat(tracker.getCurrentLevel()).isEqualTo(3);

            tracker.levelUp();
            assertThat(tracker.getCurrentLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("level=4 에서 levelUp 호출 시 4를 넘지 않는다")
        void levelUp_doesNotExceed4() {
            tracker.initChain("proj1", "proj1::redis");
            tracker.levelUp();
            tracker.levelUp();
            tracker.levelUp();

            tracker.levelUp();

            assertThat(tracker.getCurrentLevel()).isEqualTo(4);
        }

        @Test
        @DisplayName("levelUp 후 consecutiveLevelStayCount 가 0으로 초기화된다")
        void levelUp_resetConsecutiveCount() {
            tracker.initChain("proj1", "proj1::redis");
            tracker.levelStay();

            tracker.levelUp();

            assertThat(tracker.getConsecutiveLevelStayCount()).isEqualTo(0);
        }

        @Test
        @DisplayName("levelStay 연속 2회 이하 시 false 반환 — 한계 미초과")
        void levelStay_withinLimit_returnsFalse() {
            tracker.initChain("proj1", "proj1::redis");

            boolean exceeded1 = tracker.levelStay();
            boolean exceeded2 = tracker.levelStay();

            assertThat(exceeded1).isFalse();
            assertThat(exceeded2).isFalse();
        }

        @Test
        @DisplayName("levelStay 연속 3회(한계 초과) 시 true 반환")
        void levelStay_exceededLimit_returnsTrue() {
            tracker.initChain("proj1", "proj1::redis");
            tracker.levelStay();
            tracker.levelStay();

            boolean exceeded = tracker.levelStay();

            assertThat(exceeded).isTrue();
        }
    }

    @Nested
    @DisplayName("Chain 완료 및 다음 Chain 결정")
    class ChainCompletion {

        @Test
        @DisplayName("markChainComplete 후 completedChainIds에 기록되고 activeChain 해제된다")
        void markChainComplete_recordsAndClears() {
            tracker.initChain("proj1", "proj1::redis");

            tracker.markChainComplete();

            assertThat(tracker.getCompletedChainIds()).contains("proj1::redis");
            assertThat(tracker.hasActiveChain()).isFalse();
            assertThat(tracker.getCurrentLevel()).isEqualTo(1);
        }

        @Test
        @DisplayName("resolveNextChain 은 완료되지 않은 primary chain 을 우선 반환한다")
        void resolveNextChain_returnsPrimaryFirst() {
            ChainReference primary = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
            ChainReference backup = new ChainReference("proj1::kafka", "Kafka", 2, List.of(1, 2));
            InterrogationPhase phase = new InterrogationPhase(List.of(primary), List.of(backup));
            PlaygroundPhase playground = new PlaygroundPhase("프로젝트 소개해주세요", List.of());
            ProjectPlan plan = new ProjectPlan("proj1", "Redis Cache 프로젝트", 1, playground, phase);

            Optional<ChainReference> next = tracker.resolveNextChain(List.of(plan));

            assertThat(next).isPresent();
            assertThat(next.get().chainId()).isEqualTo("proj1::redis");
        }

        @Test
        @DisplayName("primary chain 완료 후 resolveNextChain 은 backup chain 을 반환한다")
        void resolveNextChain_fallsBackToBackup() {
            ChainReference primary = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
            ChainReference backup = new ChainReference("proj1::kafka", "Kafka", 2, List.of(1, 2));
            InterrogationPhase phase = new InterrogationPhase(List.of(primary), List.of(backup));
            PlaygroundPhase playground = new PlaygroundPhase("프로젝트 소개해주세요", List.of());
            ProjectPlan plan = new ProjectPlan("proj1", "Redis Cache 프로젝트", 1, playground, phase);

            tracker.initChain("proj1", "proj1::redis");
            tracker.markChainComplete();

            Optional<ChainReference> next = tracker.resolveNextChain(List.of(plan));

            assertThat(next).isPresent();
            assertThat(next.get().chainId()).isEqualTo("proj1::kafka");
        }

        @Test
        @DisplayName("모든 chain 완료 시 resolveNextChain 은 empty 를 반환한다")
        void resolveNextChain_allCompleted_returnsEmpty() {
            ChainReference primary = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3, 4));
            InterrogationPhase phase = new InterrogationPhase(List.of(primary), List.of());
            PlaygroundPhase playground = new PlaygroundPhase("프로젝트 소개해주세요", List.of());
            ProjectPlan plan = new ProjectPlan("proj1", "Redis Cache 프로젝트", 1, playground, phase);

            tracker.initChain("proj1", "proj1::redis");
            tracker.markChainComplete();

            Optional<ChainReference> next = tracker.resolveNextChain(List.of(plan));

            assertThat(next).isEmpty();
        }
    }

    @Nested
    @DisplayName("동시성 보호 (withLock)")
    class Concurrency {

        @Test
        @DisplayName("다중 스레드에서 withLock 직렬화 — levelUp 이 race condition 없이 누적된다")
        void withLock_serializes_concurrent_levelUps() throws InterruptedException {
            tracker.initChain("proj1", "proj1::redis");

            int threadCount = 4;
            CountDownLatch latch = new CountDownLatch(threadCount);
            AtomicInteger levelUpCalls = new AtomicInteger(0);
            ExecutorService executor = Executors.newFixedThreadPool(threadCount);

            for (int i = 0; i < threadCount; i++) {
                executor.submit(() -> {
                    tracker.withLock(() -> {
                        if (tracker.getCurrentLevel() < 4) {
                            tracker.levelUp();
                            levelUpCalls.incrementAndGet();
                        }
                        return null;
                    });
                    latch.countDown();
                });
            }

            latch.await();
            executor.shutdown();

            assertThat(tracker.getCurrentLevel()).isEqualTo(Math.min(1 + levelUpCalls.get(), 4));
        }
    }
}
