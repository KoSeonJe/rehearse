package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.DepthSignals;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Locale;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeExtractionService {

    private final ResumeSkeletonExtractor resumeSkeletonExtractor;

    public ResumeSkeleton extract(byte[] pdfBytes, String fileHash) {
        GeneratedResumeSkeleton parsed = resumeSkeletonExtractor.extract(pdfBytes, fileHash);
        return mapToSkeleton(parsed, fileHash);
    }

    private ResumeSkeleton mapToSkeleton(GeneratedResumeSkeleton parsed, String fileHash) {
        CandidateLevel level = parseCandidateLevel(parsed.candidateLevel());
        List<Project> projects = parsed.projects().stream()
                .map(p -> new Project(
                        p.projectId(),
                        p.projectName() == null ? "" : p.projectName(),
                        p.techStack(),
                        p.role(),
                        p.architecture(),
                        p.decisions(),
                        toDepthSignals(p.depthSignals())))
                .toList();
        return new ResumeSkeleton(parsed.resumeId(), fileHash, level, parsed.targetDomain(), projects);
    }

    private DepthSignals toDepthSignals(GeneratedResumeSkeleton.GeneratedDepthSignals raw) {
        if (raw == null) {
            return DepthSignals.empty();
        }
        return new DepthSignals(
                raw.tradeoffs(),
                raw.alternatives(),
                raw.quantitative(),
                raw.decisionRationale());
    }

    private CandidateLevel parseCandidateLevel(String raw) {
        if (raw == null || raw.isBlank()) {
            return CandidateLevel.JUNIOR;
        }
        try {
            return CandidateLevel.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException e) {
            log.warn("알 수 없는 candidate_level={} → JUNIOR fallback", raw);
            return CandidateLevel.JUNIOR;
        }
    }
}
