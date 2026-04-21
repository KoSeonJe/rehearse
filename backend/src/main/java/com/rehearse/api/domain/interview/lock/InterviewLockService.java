package com.rehearse.api.domain.interview.lock;

import org.springframework.stereotype.Service;

import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;
import java.util.function.Supplier;

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

    public <T> T withLock(Long interviewId, Supplier<T> action) {
        Lock lock = stripes[stripe(interviewId)];
        lock.lock();
        try {
            return action.get();
        } finally {
            lock.unlock();
        }
    }

    private int stripe(Long interviewId) {
        return (int) (Long.hashCode(interviewId) & (STRIPE_COUNT - 1));
    }
}
