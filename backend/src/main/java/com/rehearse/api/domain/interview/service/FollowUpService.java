package com.rehearse.api.domain.interview.service;

import static org.springframework.transaction.annotation.Propagation.*;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FollowUpService {

    private final AiClient aiClient;
    private final FollowUpTransactionHandler followUpTransactionHandler;

    @Transactional(propagation = NOT_SUPPORTED)
    public FollowUpResponse generateFollowUp(Long id, FollowUpRequest request, MultipartFile audioFile) {
        if (audioFile == null || audioFile.isEmpty()) {
            throw new BusinessException(InterviewErrorCode.ANSWER_TEXT_REQUIRED);
        }

        // Phase 1: DB 조회 + 검증 (짧은 readOnly 트랜잭션)
        FollowUpContext context = followUpTransactionHandler.loadFollowUpContext(id, request.getQuestionSetId());

        // Phase 2: GPT-audio 호출 — STT + 후속질문 한 번에 (트랜잭션 없음)
        FollowUpGenerationRequest followUpReq = new FollowUpGenerationRequest(
                context.position(),
                context.effectiveTechStack(),
                context.level(),
                request.getQuestionContent(),
                null,
                request.getNonVerbalSummary(),
                request.getPreviousExchanges()
        );
        GeneratedFollowUp followUp = aiClient.generateFollowUpWithAudio(audioFile, followUpReq);

        // Phase 3: 결과 저장 (짧은 write 트랜잭션)
        Question savedQuestion = followUpTransactionHandler.saveFollowUpResult(
                context.questionSetId(), followUp, context.nextOrderIndex());

        log.info("REALTIME 후속 질문 생성 완료: interviewId={}, questionSetId={}, questionId={}, type={}",
                id, request.getQuestionSetId(), savedQuestion.getId(), followUp.getType());

        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.getQuestion())
                .reason(followUp.getReason())
                .type(followUp.getType())
                .answerText(followUp.getAnswerText())
                .modelAnswer(savedQuestion.getModelAnswer())
                .build();
    }
}
