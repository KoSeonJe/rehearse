package com.rehearse.api.domain.interview.models.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;

/**
 * 답변 분석 결과 기반 후속 질문 생성 port.
 *
 * <p>구현체는 OpenAI / Claude 어댑터, Mock fallback, Resilient (OpenAI primary → Claude fallback).
 */
public interface FollowUpQuestionGenerator {

    GeneratedFollowUp generate(
            String mainQuestion,
            String userAnswer,
            AnswerAnalysis analysis
    );
}
