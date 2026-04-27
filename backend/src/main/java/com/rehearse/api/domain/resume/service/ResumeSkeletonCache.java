package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.repository.InterviewRuntimeStateStore;
import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class ResumeSkeletonCache {

    private final InterviewRuntimeStateStore runtimeStateStore;

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
