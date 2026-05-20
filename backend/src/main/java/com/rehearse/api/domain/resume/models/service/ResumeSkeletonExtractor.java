package com.rehearse.api.domain.resume.models.service;

import com.rehearse.api.infra.ai.dto.GeneratedResumeSkeleton;

public interface ResumeSkeletonExtractor {

    GeneratedResumeSkeleton extract(byte[] pdfBytes, String fileHash);
}
