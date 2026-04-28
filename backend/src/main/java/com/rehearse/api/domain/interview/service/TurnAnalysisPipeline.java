package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.AnswerAnalysis;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;
import com.rehearse.api.domain.interview.entity.IntentResult;
import com.rehearse.api.domain.interview.entity.IntentType;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

import java.util.List;

/**
 * Resume 트랙의 텍스트 기반 턴 분석 파이프라인.
 * IntentClassifier + AnswerAnalyzer 조합을 캡슐화하여 Orchestrator 책임을 분리한다.
 * Audio 경로(Standard 트랙)는 AudioTurnAnalyzer가 담당하므로 이 클래스는 text-only.
 */
@Component
@RequiredArgsConstructor
public class TurnAnalysisPipeline {

    private final IntentClassifier intentClassifier;
    private final AnswerAnalyzer answerAnalyzer;

    public TurnAnalysisResult analyze(
            Long interviewId,
            long turnIndex,
            String questionContent,
            String answerText,
            List<FollowUpExchange> previousExchanges
    ) {
        IntentResult intent = intentClassifier.classify(questionContent, answerText, previousExchanges);

        AnswerAnalysis analysis = (intent.type() == IntentType.ANSWER)
                ? answerAnalyzer.analyze(interviewId, turnIndex, questionContent, null, answerText, List.of())
                : AnswerAnalysis.empty(turnIndex);

        return new TurnAnalysisResult(answerText, intent, analysis);
    }
}
