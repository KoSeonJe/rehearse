package com.rehearse.api.domain.resume.entity;

import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;

import java.util.List;
import java.util.Map;

public record ResumeSkeleton(
        String resumeId,
        String fileHash,
        CandidateLevel candidateLevel,
        String targetDomain,
        List<Project> projects,
        Map<String, List<String>> interrogationPriorityMap
) {

    public List<String> priorityIds(String priority) {
        if (interrogationPriorityMap == null) {
            return List.of();
        }
        return interrogationPriorityMap.getOrDefault(priority, List.of());
    }

    public static ResumeSkeleton fromEntity(ResumeSkeletonEntity entity, ResumeSkeleton parsed) {
        return new ResumeSkeleton(
                parsed.resumeId(),
                entity.getFileHash(),
                parsed.candidateLevel(),
                parsed.targetDomain(),
                parsed.projects(),
                parsed.interrogationPriorityMap()
        );
    }
}
