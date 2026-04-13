package com.rehearse.api.domain.interview.generation.pool.service;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class QuestionGenerationLockTest {

    private QuestionGenerationLock questionGenerationLock;

    @BeforeEach
    void setUp() {
        questionGenerationLock = new QuestionGenerationLock();
    }

    @Nested
    @DisplayName("acquire 메서드")
    class Acquire {

        @Test
        @DisplayName("새 키로 acquire하면 잠긴 상태의 ReentrantLock을 반환한다")
        void acquire_newKey_returnsLockedLock() {
            // given
            String key = "backend:cs:junior";

            // when
            ReentrantLock lock = questionGenerationLock.acquire(key);

            // then
            assertThat(lock.isLocked()).isTrue();

            // cleanup
            questionGenerationLock.release(lock);
        }

        @Test
        @DisplayName("같은 키로 2회 acquire하면 동일한 Lock 인스턴스를 반환한다")
        void acquire_sameKey_returnsSameLock() {
            // given
            String key = "backend:cs:junior";
            ReentrantLock firstLock = questionGenerationLock.acquire(key);
            questionGenerationLock.release(firstLock);

            // when
            ReentrantLock secondLock = questionGenerationLock.acquire(key);

            // then
            assertThat(secondLock).isSameAs(firstLock);

            // cleanup
            questionGenerationLock.release(secondLock);
        }

        @Test
        @DisplayName("다른 키로 acquire하면 서로 다른 Lock 인스턴스를 반환한다")
        void acquire_differentKeys_returnsDifferentLocks() {
            // given
            String key1 = "backend:cs:junior";
            String key2 = "frontend:cs:mid";

            // when
            ReentrantLock lock1 = questionGenerationLock.acquire(key1);
            questionGenerationLock.release(lock1);
            ReentrantLock lock2 = questionGenerationLock.acquire(key2);
            questionGenerationLock.release(lock2);

            // then
            assertThat(lock1).isNotSameAs(lock2);
        }

        @Test
        @DisplayName("release 후 동일 키로 재획득하면 정상적으로 획득된다")
        void acquire_afterRelease_canReacquire() {
            // given
            String key = "backend:cs:junior";
            ReentrantLock lock = questionGenerationLock.acquire(key);
            questionGenerationLock.release(lock);

            // when
            ReentrantLock reacquiredLock = questionGenerationLock.acquire(key);

            // then
            assertThat(reacquiredLock.isLocked()).isTrue();

            // cleanup
            questionGenerationLock.release(reacquiredLock);
        }

        @Test
        @DisplayName("2개 스레드가 같은 키로 동시에 acquire하면 하나만 진행되고 나머지는 대기한다")
        void acquire_concurrentSameKey_onlyOneProceeds() throws InterruptedException {
            // given
            String key = "backend:cs:junior";
            CountDownLatch firstAcquired = new CountDownLatch(1);
            CountDownLatch secondBlocked = new CountDownLatch(1);
            boolean[] secondThreadProceeded = {false};

            ReentrantLock firstLock = questionGenerationLock.acquire(key);

            Thread secondThread = new Thread(() -> {
                secondBlocked.countDown();
                ReentrantLock lock = questionGenerationLock.acquire(key);
                secondThreadProceeded[0] = true;
                questionGenerationLock.release(lock);
            });

            // when
            secondThread.start();
            secondBlocked.await();
            Thread.sleep(50); // 두 번째 스레드가 블로킹 상태임을 확인

            // then
            assertThat(secondThreadProceeded[0]).isFalse();

            // cleanup: 첫 번째 lock 해제 후 두 번째 스레드가 진행됨을 확인
            questionGenerationLock.release(firstLock);
            secondThread.join(1000);
            assertThat(secondThreadProceeded[0]).isTrue();
        }
    }

    @Nested
    @DisplayName("release 메서드")
    class Release {

        @Test
        @DisplayName("acquire한 lock을 release하면 isLocked가 false가 된다")
        void release_unlocksLock() {
            // given
            ReentrantLock lock = questionGenerationLock.acquire("backend:cs:junior");
            assertThat(lock.isLocked()).isTrue();

            // when
            questionGenerationLock.release(lock);

            // then
            assertThat(lock.isLocked()).isFalse();
        }

        @Test
        @DisplayName("이미 해제된 lock을 release하면 IllegalMonitorStateException이 발생한다")
        void release_alreadyUnlocked_throwsException() {
            // given
            ReentrantLock lock = questionGenerationLock.acquire("backend:cs:junior");
            questionGenerationLock.release(lock);

            // when & then
            assertThatThrownBy(() -> questionGenerationLock.release(lock))
                    .isInstanceOf(IllegalMonitorStateException.class);
        }
    }
}
