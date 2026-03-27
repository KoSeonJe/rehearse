package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.CreateInterviewRequest;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.Collections;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewCreationService {

    private final InterviewRepository interviewRepository;
    private final PdfTextExtractor pdfTextExtractor;
    private final ApplicationEventPublisher eventPublisher;

    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request, MultipartFile resumeFile) {
        String resumeText = null;
        if (resumeFile != null && !resumeFile.isEmpty()) {
            resumeText = pdfTextExtractor.extract(resumeFile);
        }

        if (request.getTechStack() != null && !request.getTechStack().isAllowedFor(request.getPosition())) {
            throw new BusinessException(InterviewErrorCode.INVALID_TECH_STACK);
        }

        Interview interview = Interview.builder()
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
                request.getPosition(),
                request.getPositionDetail(),
                request.getLevel(),
                request.getInterviewTypes(),
                request.getCsSubTopics(),
                resumeText,
                request.getDurationMinutes(),
                request.getTechStack()
        ));

        log.info("면접 세션 생성 완료 (질문 생성 이벤트 발행): id={}, position={}, level={}, types={}",
                saved.getId(), saved.getPosition(), saved.getLevel(), saved.getInterviewTypes());

        return InterviewResponse.from(saved, Collections.emptyList());
    }
}
