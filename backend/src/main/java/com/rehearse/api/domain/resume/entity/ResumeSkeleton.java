package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record ResumeSkeleton(
        String resumeId,
        String fileHash,
        CandidateLevel candidateLevel,
        String targetDomain,
        List<Project> projects
) {

    public ResumeSkeleton {
        projects = projects == null ? List.of() : List.copyOf(projects);
    }

    public static ResumeSkeleton fromEntity(ResumeSkeletonEntity entity, ResumeSkeleton parsed) {
        return new ResumeSkeleton(
                parsed.resumeId(),
                entity.getFileHash(),
                parsed.candidateLevel(),
                parsed.targetDomain(),
                parsed.projects()
        );
    }
}
