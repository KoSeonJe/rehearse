package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
@RequiredArgsConstructor
public class TurnAnalysisPipeline {

    private final AnswerAnalyzer answerAnalyzer;

    public TurnAnalysisResult analyze(
            Long interviewId,
            long turnIndex,
            String questionContent,
            String answerText,
            List<FollowUpExchange> previousExchanges
    ) {
        AnswerAnalysis analysis = answerAnalyzer.analyze(
                interviewId, turnIndex, questionContent, null, answerText, List.of());
        return new TurnAnalysisResult(answerText, analysis);
    }
}
