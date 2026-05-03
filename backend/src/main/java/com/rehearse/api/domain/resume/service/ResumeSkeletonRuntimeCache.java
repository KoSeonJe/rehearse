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
        ResumeSkeleton cached = runtimeStateStore.get(interviewId).getResumeSkeletonCache();
        if (cached != null && fileHash.equals(cached.fileHash())) {
            return cached;
        }
        return null;
    }

    public void write(Long interviewId, ResumeSkeleton skeleton) {
        try {
            runtimeStateStore.update(interviewId, state -> state.setResumeSkeleton(skeleton));
        } catch (IllegalStateException e) {
            throw new IllegalStateException(
                    "Runtime state must be seeded before skeleton cache write: interviewId=" + interviewId, e);
        }
    }
}
