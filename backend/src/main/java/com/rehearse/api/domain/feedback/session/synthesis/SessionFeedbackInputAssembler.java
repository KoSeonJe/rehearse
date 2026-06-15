package com.rehearse.api.domain.feedback.session.synthesis;

import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import com.rehearse.api.domain.question.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.Comparator;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class SessionFeedbackInputAssembler {

    private final InterviewFinder interviewFinder;
    private final TimestampFeedbackRepository timestampFeedbackRepository;
    private final QuestionSetRepository questionSetRepository;

    public SessionFeedbackInput assemble(Long interviewId) {
        Interview interview = interviewFinder.findById(interviewId);
        List<TurnScoreView> turnScores = buildTurnScores(interviewId);
        return buildInput(interview, turnScores, null, null);
    }

    public SessionFeedbackInput assembleWithDelivery(Long interviewId, String deliveryAnalysis,
                                                     String visionAnalysis, String nonverbalAggregate) {
        Interview interview = interviewFinder.findById(interviewId);
        List<TurnScoreView> turnScores = buildTurnScores(interviewId);
        return buildInput(interview, turnScores, deliveryAnalysis, visionAnalysis);
    }

    private SessionFeedbackInput buildInput(Interview interview, List<TurnScoreView> turnScores,
                                            String deliveryAnalysis, String visionAnalysis) {
        return new SessionFeedbackInput(
                buildSessionMetadata(interview, turnScores.size()),
                turnScores,
                deliveryAnalysis,
                visionAnalysis,
                buildCoverage(turnScores),
                interview.getLevel()
        );
    }

    private List<TurnScoreView> buildTurnScores(Long interviewId) {
        Map<Long, String> turnLabels = buildTurnLabels(interviewId);
        return timestampFeedbackRepository.findByInterviewIdOrderByStartMs(interviewId).stream()
                .map(feedback -> toTurnScoreView(feedback, turnLabels))
                .toList();
    }

    private TurnScoreView toTurnScoreView(TimestampFeedback feedback, Map<Long, String> turnLabels) {
        String turnLabel = feedback.getQuestion() != null
                ? turnLabels.get(feedback.getQuestion().getId())
                : null;
        return new TurnScoreView(
                turnLabel,
                feedback.getVerbalComment(),
                feedback.getAccuracyIssues(),
                feedback.getCoachingStructure(),
                feedback.getCoachingImprovement(),
                feedback.getNonverbalComment(),
                feedback.getVocalComment(),
                feedback.getAttitudeComment(),
                feedback.getOverallComment()
        );
    }

    private String buildCoverage(List<TurnScoreView> turnScores) {
        long analyzed = turnScores.stream()
                .filter(this::hasContent)
                .count();
        if (analyzed == turnScores.size()) {
            return "all turns scored";
        }
        return analyzed + "/" + turnScores.size() + " turns scored";
    }

    private boolean hasContent(TurnScoreView turn) {
        return turn.verbalComment() != null
                || turn.overallComment() != null
                || turn.coachingStructure() != null
                || turn.coachingImprovement() != null;
    }

    private Map<Long, String> buildTurnLabels(Long interviewId) {
        List<Question> questions = questionSetRepository.findByInterviewIdWithQuestions(interviewId).stream()
                .sorted(Comparator.comparingInt(QuestionSet::getOrderIndex))
                .flatMap(qs -> qs.getQuestions().stream()
                        .sorted(Comparator.comparingInt(Question::getOrderIndex)))
                .toList();

        Map<Long, String> labels = new HashMap<>();
        int mainOrder = 0;
        int followUpOrder = 1;
        for (Question question : questions) {
            if (question.getQuestionType().isMain()) {
                mainOrder++;
                followUpOrder = 1;
                labels.put(question.getId(), mainOrder + "-1");
            } else if (question.getQuestionType().isFollowUp() && mainOrder > 0) {
                followUpOrder++;
                labels.put(question.getId(), mainOrder + "-" + followUpOrder);
            }
        }
        return labels;
    }

    private SessionFeedbackInput.SessionMetadata buildSessionMetadata(Interview interview, int totalTurns) {
        return new SessionFeedbackInput.SessionMetadata(
                interview.getId(),
                interview.getPosition() != null ? interview.getPosition().name() : "UNKNOWN",
                interview.getLevel() != null ? interview.getLevel().name() : "MID",
                interview.getInterviewTypes().stream()
                        .map(Enum::name)
                        .toList(),
                totalTurns,
                interview.getDurationMinutes() != null ? interview.getDurationMinutes() : 0
        );
    }
}
