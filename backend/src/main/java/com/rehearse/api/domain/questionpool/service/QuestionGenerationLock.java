package com.rehearse.api.domain.questionpool.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class QuestionGenerationLock {

    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock acquire(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    public void release(String key, ReentrantLock lock) {
        lock.unlock();
        locks.remove(key, lock);
    }
}
