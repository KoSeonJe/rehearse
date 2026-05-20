package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeIngestionService {

    private final ResumeExtractionService extractionService;
    private final ResumeSkeletonPersister skeletonStore;

    public ResumeSkeleton ingestPdf(Long interviewId, byte[] pdfBytes, String fileHash) {
        validatePdfBytes(pdfBytes);
        if (fileHash == null || fileHash.isBlank()) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_EMPTY);
        }

        ResumeSkeleton fromDb = skeletonStore.findByInterviewId(interviewId)
                .filter(s -> fileHash.equals(s.fileHash()))
                .orElse(null);
        if (fromDb != null) {
            log.info("이력서 DB 히트: interviewId={}, fileHash={}", interviewId, fileHash.substring(0, 8));
            return fromDb;
        }

        return extractAndPersist(interviewId, pdfBytes, fileHash);
    }

    private void validatePdfBytes(byte[] pdfBytes) {
        if (pdfBytes == null || pdfBytes.length == 0) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_EMPTY);
        }
    }

    private ResumeSkeleton extractAndPersist(Long interviewId, byte[] pdfBytes, String fileHash) {
        ResumeSkeleton skeleton = extractionService.extract(pdfBytes, fileHash);
        skeletonStore.save(interviewId, skeleton);
        log.info("이력서 추출·저장 완료: interviewId={}, fileHash={}", interviewId, fileHash.substring(0, 8));
        return skeleton;
    }
}
