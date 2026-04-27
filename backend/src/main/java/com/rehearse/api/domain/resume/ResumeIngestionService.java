package com.rehearse.api.domain.resume;

import com.rehearse.api.domain.resume.domain.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
public class ResumeIngestionService {

    private static final int MIN_RESUME_TEXT_LENGTH = 50;

    private final PdfTextExtractor pdfTextExtractor;
    private final ResumeExtractionService extractionService;
    private final ResumeFileHasher fileHasher;
    private final ResumeSkeletonStore skeletonStore;
    private final ResumeSkeletonCache skeletonCache;

    public ResumeSkeleton ingest(Long interviewId, MultipartFile resumeFile) {
        byte[] fileBytes = readFileBytes(resumeFile);
        String fileHash = fileHasher.hash(fileBytes);

        ResumeSkeleton cached = skeletonCache.read(interviewId, fileHash);
        if (cached != null) {
            log.info("이력서 캐시 히트: interviewId={}, fileHash={}", interviewId, fileHash.substring(0, 8));
            return cached;
        }

        ResumeSkeleton fromDb = skeletonStore.findByInterviewId(interviewId)
                .filter(s -> fileHash.equals(s.fileHash()))
                .orElse(null);
        if (fromDb != null) {
            log.info("이력서 DB 히트: interviewId={}, fileHash={}", interviewId, fileHash.substring(0, 8));
            skeletonCache.write(interviewId, fromDb);
            return fromDb;
        }

        String normalizedText = pdfTextExtractor.extract(resumeFile);
        validateExtractedText(normalizedText);

        return extractAndPersist(interviewId, normalizedText, fileHash);
    }

    private byte[] readFileBytes(MultipartFile resumeFile) {
        try {
            return resumeFile.getBytes();
        } catch (IOException e) {
            log.error("이력서 파일 읽기 실패", e);
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_EMPTY);
        }
    }

    private void validateExtractedText(String text) {
        if (text == null || text.isBlank() || text.length() < MIN_RESUME_TEXT_LENGTH) {
            throw new BusinessException(ResumeErrorCode.EMPTY_RESUME_TEXT);
        }
    }

    private ResumeSkeleton extractAndPersist(Long interviewId, String normalizedText, String fileHash) {
        ResumeSkeleton skeleton = extractionService.extract(normalizedText, fileHash);
        skeletonStore.save(interviewId, skeleton);
        skeletonCache.write(interviewId, skeleton);
        log.info("이력서 추출·저장 완료: interviewId={}, fileHash={}", interviewId, fileHash.substring(0, 8));
        return skeleton;
    }
}
