package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Random;

@Component
public class ResumeSkeletonSampler {

    private static final int MIN_DECISIONS = 2;

    public ResumeSkeleton sampleDecisions(ResumeSkeleton skeleton, long seed) {
        Random random = new Random(seed);
        List<Project> sampledProjects = skeleton.projects().stream()
                .map(project -> sampleProjectDecisions(project, random))
                .toList();
        return new ResumeSkeleton(
                skeleton.resumeId(),
                skeleton.fileHash(),
                skeleton.candidateLevel(),
                skeleton.targetDomain(),
                sampledProjects
        );
    }

    private Project sampleProjectDecisions(Project project, Random random) {
        List<String> decisions = project.decisions();
        if (decisions.size() <= MIN_DECISIONS) {
            return project;
        }
        int targetSize = Math.max(MIN_DECISIONS, (int) Math.ceil(decisions.size() / 2.0));
        List<String> shuffled = new ArrayList<>(decisions);
        Collections.shuffle(shuffled, random);
        List<String> sampled = List.copyOf(shuffled.subList(0, targetSize));
        return new Project(
                project.projectId(),
                project.projectName(),
                project.techStack(),
                project.role(),
                project.architecture(),
                sampled,
                project.depthSignals()
        );
    }
}
