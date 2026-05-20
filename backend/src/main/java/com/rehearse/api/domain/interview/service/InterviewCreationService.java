package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.CreateInterviewRequest;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.util.FileHasher;
import java.io.IOException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewCreationService {

    private static final long MAX_RESUME_FILE_SIZE_BYTES = 10_485_760L;
    private static final String PDF_CONTENT_TYPE = "application/pdf";
    private static final byte[] PDF_MAGIC_BYTES = {'%', 'P', 'D', 'F'};

    private final InterviewRepository interviewRepository;
    private final FileHasher fileHasher;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InterviewResponse createInterview(Long userId, CreateInterviewRequest request, MultipartFile resumeFile) {
        validateResumeExclusivity(request.getInterviewTypes(), resumeFile);
        if (!request.getTechStack().isAllowedFor(request.getPosition())) {
            throw new BusinessException(InterviewErrorCode.INVALID_TECH_STACK);
        }

        byte[] resumePdfBytes = null;
        String resumeFileHash = null;
        if (resumeFile != null && !resumeFile.isEmpty()) {
            validatePdfFile(resumeFile);
            resumePdfBytes = readFileBytes(resumeFile);
            validatePdfMagicBytes(resumePdfBytes);
            resumeFileHash = fileHasher.hash(resumePdfBytes);
        }

        Interview interview = Interview.builder()
                .userId(userId)
                .position(request.getPosition())
                .positionDetail(request.getPositionDetail())
                .level(request.getLevel())
                .interviewTypes(request.getInterviewTypes())
                .csSubTopics(request.getCsSubTopics())
                .durationMinutes(request.getDurationMinutes())
                .techStack(request.getTechStack())
                .build();

        Interview saved = interviewRepository.save(interview);

        eventPublisher.publishEvent(new QuestionGenerationRequestedEvent(
                saved.getId(),
                userId,
                request.getPosition(),
                request.getPositionDetail(),
                request.getLevel(),
                request.getInterviewTypes(),
                request.getCsSubTopics(),
                resumePdfBytes,
                resumeFileHash,
                request.getDurationMinutes(),
                request.getTechStack()
        ));

        log.info("면접 세션 생성 완료 (질문 생성 이벤트 발행): id={}, position={}, level={}, types={}",
                saved.getId(), saved.getPosition(), saved.getLevel(), saved.getInterviewTypes());

        return InterviewResponse.from(saved, Collections.emptyList());
    }

    private void validateResumeExclusivity(List<InterviewType> interviewTypes, MultipartFile resumeFile) {
        if (interviewTypes == null || interviewTypes.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.INVALID_INTERVIEW_TYPES);
        }

        boolean hasResumeBased = interviewTypes.contains(InterviewType.RESUME_BASED);
        boolean hasResumeFile = resumeFile != null && !resumeFile.isEmpty();

        if (hasResumeBased && interviewTypes.size() > 1) {
            throw new BusinessException(ResumeErrorCode.RESUME_EXCLUSIVITY_VIOLATION);
        }

        if (hasResumeBased && !hasResumeFile) {
            throw new BusinessException(ResumeErrorCode.RESUME_REQUIRED_FOR_RESUME_BASED);
        }

        if (!hasResumeBased && hasResumeFile) {
            throw new BusinessException(ResumeErrorCode.RESUME_EXCLUSIVITY_VIOLATION);
        }
    }

    private byte[] readFileBytes(MultipartFile resumeFile) {
        try {
            return resumeFile.getBytes();
        } catch (IOException e) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_EMPTY);
        }
    }

    private void validatePdfFile(MultipartFile resumeFile) {
        if (resumeFile.getSize() > MAX_RESUME_FILE_SIZE_BYTES) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_SIZE);
        }
        String contentType = resumeFile.getContentType();
        if (contentType == null || !contentType.equals(PDF_CONTENT_TYPE)) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_TYPE);
        }
    }

    private void validatePdfMagicBytes(byte[] bytes) {
        if (bytes.length < PDF_MAGIC_BYTES.length) {
            throw new BusinessException(ResumeErrorCode.INVALID_FILE_MAGIC_BYTES);
        }
        for (int i = 0; i < PDF_MAGIC_BYTES.length; i++) {
            if (bytes[i] != PDF_MAGIC_BYTES[i]) {
                throw new BusinessException(ResumeErrorCode.INVALID_FILE_MAGIC_BYTES);
            }
        }
    }

}
