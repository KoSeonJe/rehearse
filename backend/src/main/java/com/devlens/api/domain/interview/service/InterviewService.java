package com.devlens.api.domain.interview.service;

import com.devlens.api.domain.interview.dto.*;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewQuestion;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.ClaudeApiClient;
import com.devlens.api.infra.ai.dto.GeneratedFollowUp;
import com.devlens.api.infra.ai.dto.GeneratedQuestion;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InterviewService {

    private final InterviewRepository interviewRepository;
    private final ClaudeApiClient claudeApiClient;

    @Transactional
    public InterviewResponse createInterview(CreateInterviewRequest request) {
        // 1. Interview 엔티티 생성
        Interview interview = Interview.builder()
                .position(request.getPosition())
                .level(request.getLevel())
                .interviewType(request.getInterviewType())
                .build();

        // 2. Claude API로 질문 생성
        List<GeneratedQuestion> generatedQuestions = claudeApiClient.generateQuestions(
                request.getPosition(),
                request.getLevel(),
                request.getInterviewType()
        );

        // 3. 질문 엔티티 변환 및 연결
        for (GeneratedQuestion gq : generatedQuestions) {
            InterviewQuestion question = InterviewQuestion.builder()
                    .questionOrder(gq.getOrder())
                    .category(gq.getCategory())
                    .content(gq.getContent())
                    .evaluationCriteria(gq.getEvaluationCriteria())
                    .build();
            interview.addQuestion(question);
        }

        // 4. 저장
        Interview saved = interviewRepository.save(interview);

        log.info("면접 세션 생성 완료: id={}, position={}, level={}, type={}",
                saved.getId(), saved.getPosition(), saved.getLevel(), saved.getInterviewType());

        return InterviewResponse.from(saved);
    }

    public InterviewResponse getInterview(Long id) {
        Interview interview = findInterviewById(id);
        return InterviewResponse.from(interview);
    }

    @Transactional
    public UpdateStatusResponse updateStatus(Long id, UpdateStatusRequest request) {
        Interview interview = findInterviewById(id);

        try {
            interview.updateStatus(request.getStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INTERVIEW_002",
                    e.getMessage()
            );
        }

        log.info("면접 세션 상태 변경: id={}, newStatus={}", id, request.getStatus());

        return UpdateStatusResponse.from(interview);
    }

    public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request) {
        Interview interview = findInterviewById(id);

        if (interview.getStatus() != InterviewStatus.IN_PROGRESS) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "INTERVIEW_003",
                    "진행 중인 면접에서만 후속 질문을 생성할 수 있습니다."
            );
        }

        GeneratedFollowUp followUp = claudeApiClient.generateFollowUpQuestion(
                request.getQuestionContent(),
                request.getAnswerText(),
                request.getNonVerbalSummary()
        );

        log.info("후속 질문 생성 완료: interviewId={}, type={}", id, followUp.getType());

        return FollowUpResponse.builder()
                .question(followUp.getQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .build();
    }

    private Interview findInterviewById(Long id) {
        return interviewRepository.findByIdWithQuestions(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INTERVIEW_001",
                        "면접 세션을 찾을 수 없습니다."
                ));
    }
}
