package com.rehearse.api.domain.question.service;

import org.springframework.stereotype.Component;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.locks.ReentrantLock;

@Component
public class QuestionGenerationLock {

    // cacheKey 종류는 유한(수십 개)하므로 제거 없이 상주해도 메모리 문제 없음
    private final ConcurrentHashMap<String, ReentrantLock> locks = new ConcurrentHashMap<>();

    public ReentrantLock acquire(String key) {
        ReentrantLock lock = locks.computeIfAbsent(key, k -> new ReentrantLock());
        lock.lock();
        return lock;
    }

    public void release(ReentrantLock lock) {
        lock.unlock();
    }
}
