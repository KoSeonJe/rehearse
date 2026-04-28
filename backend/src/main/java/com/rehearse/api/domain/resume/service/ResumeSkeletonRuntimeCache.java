package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeSkeletonRuntimeCache {

    private final InterviewRuntimeStateCache runtimeStateStore;

    public ResumeSkeleton read(Long interviewId, String fileHash) {
        try {
            ResumeSkeleton cached = runtimeStateStore.get(interviewId).getResumeSkeletonCache();
            if (cached != null && fileHash.equals(cached.fileHash())) {
                return cached;
            }
        } catch (IllegalStateException e) {
        }
        return null;
    }

    public void write(Long interviewId, ResumeSkeleton skeleton) {
        try {
            runtimeStateStore.update(interviewId, state -> state.setResumeSkeleton(skeleton));
        } catch (IllegalStateException e) {
            log.warn("RuntimeState 미초기화로 캐시 갱신 스킵: interviewId={}", interviewId);
        }
    }
}
