package com.rehearse.api.domain.interview.lock;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

/**
 * 면접 세션 단위 비관적 락 서비스.
 *
 * <h3>Lock Acquisition Contract</h3>
 * <ul>
 *   <li>lock outer → txn inner: {@code withLock} / {@code tryLock} 은 반드시
 *       {@code @Transactional} 메서드 <em>바깥</em>에서 획득해야 한다.
 *       트랜잭션 커밋 전에 락이 해제되면 후속 스레드가 미완료 상태를 읽는다.</li>
 *   <li>단일 interviewId 원칙: 락 블록 안에서는 <strong>동일 {@code interviewId}</strong>의
 *       DB 작업만 수행한다. 복수 interview 업데이트는 DB 트랜잭션 isolation에 의존한다.</li>
 *   <li>재진입 허용: {@code ReentrantLock} 기반이므로 동일 스레드에서 중첩 호출 가능 — 데드락 없음.</li>
 * </ul>
 */
@Service
public class InterviewLockService {

    private static final int STRIPE_COUNT = 256;

    private final ReentrantLock[] stripes;

    public InterviewLockService() {
        stripes = new ReentrantLock[STRIPE_COUNT];
        for (int i = 0; i < STRIPE_COUNT; i++) {
            stripes[i] = new ReentrantLock();
        }
    }

    /**
     * 무한 대기 락. 호출자가 이미 동일 스레드에서 락을 보유 중이면 재진입한다.
     */
    public <T> T withLock(Long interviewId, Supplier<T> action) {
        Lock lock = stripes[stripe(interviewId)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    /**
     * 제한 시간 내 락 획득을 시도한다.
     *
     * @param interviewId 락 대상 면접 ID
     * @param timeout     최대 대기 시간
     * @param action      락 보유 중 실행할 작업
     * @throws LockAcquisitionException timeout 경과 또는 인터럽트 발생 시
     */
    public <T> T tryLock(Long interviewId, Duration timeout, Supplier<T> action) {
        Lock lock = stripes[stripe(interviewId)];
        try {
            if (!lock.tryLock(timeout.toMillis(), TimeUnit.MILLISECONDS)) {
                throw new LockAcquisitionException(interviewId, timeout);
            }
            try {
                return action.get();
            } finally {
                lock.unlock();
            }
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new LockAcquisitionException(interviewId, e);
        }
    }

    private int stripe(Long interviewId) {
        return (int) (Long.hashCode(interviewId) & (STRIPE_COUNT - 1));
    }
}
