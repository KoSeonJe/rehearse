package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.resume.models.service.ResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.adapter.OpenAiResumeSkeletonExtractor;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;
import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton.GeneratedProject;
import jakarta.annotation.PostConstruct;
import java.util.List;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@ConditionalOnMissingBean(OpenAiResumeSkeletonExtractor.class)
public class MockResumeSkeletonExtractor implements ResumeSkeletonExtractor {

    @PostConstruct
    void init() {
        log.warn("=== MockResumeSkeletonExtractor 활성화: API 키 없이 Mock skeleton 으로 동작합니다 ===");
    }

    @Override
    public GeneratedResumeSkeleton extract(byte[] pdfBytes, String fileHash) {
        String hashPrefix = (fileHash != null && fileHash.length() >= 8) ? fileHash.substring(0, 8) : "(none)";
        log.info("[MockResumeSkeletonExtractor] 호출 fileHash={}", hashPrefix);
        return new GeneratedResumeSkeleton(
                "mock-r1",
                "junior",
                "backend",
                List.<GeneratedProject>of()
        );
    }
}
