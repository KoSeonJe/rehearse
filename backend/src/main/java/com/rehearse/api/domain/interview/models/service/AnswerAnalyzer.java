package com.rehearse.api.domain.interview.models.service;

import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;

/**
 * 사용자 답변 분석 (claims / dimension gaps / 다음 행동) port.
 *
 * <p>구현체는 OpenAI / Claude 어댑터, Mock fallback, Resilient (OpenAI primary → Claude fallback).
 */
public interface AnswerAnalyzer {

    GeneratedAnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    );
}
