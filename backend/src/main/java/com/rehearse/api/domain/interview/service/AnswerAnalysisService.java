package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.models.service.AnswerAnalyzer;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.infra.ai.dto.GeneratedAnswerAnalysis;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@RequiredArgsConstructor
public class AnswerAnalysisService {

    private final AnswerAnalyzer answerAnalyzer;

    public AnswerAnalysis analyze(
            Long interviewId,
            String mainQuestion,
            ReferenceType questionReferenceType,
            String userAnswer,
            boolean isResumeTrack
    ) {
        if (interviewId == null) {
            throw new IllegalArgumentException("interviewId 는 null 일 수 없습니다.");
        }

        GeneratedAnswerAnalysis generated = answerAnalyzer.analyze(
                interviewId, mainQuestion, questionReferenceType, userAnswer, isResumeTrack);
        return generated.toDomain();
    }
}
