package com.rehearse.api.domain.question.models.service;

import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;

/**
 * 이력서 트랙 질문 (opener + main) 일괄 생성 port.
 *
 * <p>구현체는 OpenAI / Claude 어댑터, Mock fallback, Resilient (OpenAI primary → Claude fallback).
 */
public interface ResumeQuestionGenerator {

    GeneratedResumeQuestions generate(ResumeSkeleton skeleton, int openerCount, int mainCount);
}
