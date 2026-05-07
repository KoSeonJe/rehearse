package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.domain.interview.entity.AskedPerspectives;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.SttService;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import lombok.extern.slf4j.Slf4j;
import org.springframework.lang.Nullable;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

@Slf4j
@Service
public class TextFallbackTurnAnalyzer {

    private final SttService sttService;
    private final AnswerAnalyzer answerAnalyzer;

    public TextFallbackTurnAnalyzer(
            @Nullable SttService sttService,
            AnswerAnalyzer answerAnalyzer
    ) {
        this.sttService = sttService;
        this.answerAnalyzer = answerAnalyzer;
    }

    public TurnAnalysisResult analyze(
            Long interviewId,
            Long turnId,
            MultipartFile audioFile,
            String mainQuestion,
            ReferenceType questionReferenceType,
            AskedPerspectives askedPerspectives
    ) {
        if (sttService == null) {
            log.error("[TextFallbackTurnAnalyzer] STT 서비스 미설정 — fallback 불가. interviewId={}", interviewId);
            throw new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE);
        }
        String answerText = sttService.transcribe(audioFile);
        AnswerAnalysis analysis = answerAnalyzer.analyze(
                interviewId, turnId, mainQuestion, questionReferenceType,
                answerText, askedPerspectives.values());
        return new TurnAnalysisResult(answerText, analysis);
    }
}
