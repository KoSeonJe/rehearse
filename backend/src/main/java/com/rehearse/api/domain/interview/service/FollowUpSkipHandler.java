package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.BlockReason;
import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.TurnAnalysisResult;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
public class FollowUpSkipHandler {

    private final AiCallMetrics aiCallMetrics;
    private final FollowUpTransactionHandler followUpTransactionHandler;

    public FollowUpResponse handleAnalyzerSkip(
            Long interviewId, FollowUpContext context, FollowUpRequest request, TurnAnalysisResult turn
    ) {
        log.info("Analyzer SKIP 권고 → Step B 미호출. interviewId={}, questionSetId={}",
                interviewId, request.getQuestionSetId());
        aiCallMetrics.incrementFollowUpSkip("analyzer_skip");
        int turnIndex = turnIndexOf(request);
        log.warn("[진행차단진단] interviewId={} track={} stage=standard-followup reason={} turnIndex={}",
                interviewId, InterviewTrack.CS.logLabel(),
                BlockReason.ANALYZER_SKIP.logValue(), turnIndex);
        followUpTransactionHandler.publishTurnCompletedEvent(
                interviewId, context, turn, context.currentMainQuestionId(), turnIndex);
        return FollowUpResponse.aiSkip(turn.answerText(), "analyzer_recommend_skip");
    }

    public FollowUpResponse handleStepBSkip(
            Long interviewId, FollowUpContext context, FollowUpRequest request,
            TurnAnalysisResult turn, String skipReason
    ) {
        log.info("Step B 가 skip 반환: interviewId={}, questionSetId={}, reason={}",
                interviewId, request.getQuestionSetId(), skipReason);
        aiCallMetrics.incrementFollowUpSkip("step_b_skip");
        int turnIndex = turnIndexOf(request);
        log.warn("[진행차단진단] interviewId={} track={} stage=standard-followup reason={} turnIndex={}",
                interviewId, InterviewTrack.CS.logLabel(),
                BlockReason.STEP_B_SKIP.logValue(), turnIndex);
        followUpTransactionHandler.publishTurnCompletedEvent(
                interviewId, context, turn, context.currentMainQuestionId(), turnIndex);
        return FollowUpResponse.aiSkip(turn.answerText(), skipReason);
    }

    private int turnIndexOf(FollowUpRequest request) {
        return request.getPreviousExchanges() == null ? 0 : request.getPreviousExchanges().size();
    }
}
