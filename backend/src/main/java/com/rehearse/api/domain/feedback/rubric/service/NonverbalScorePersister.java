package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NonverbalScorePersister {

    private final QuestionScorePersister questionScorePersister;

    public void persistAll(QuestionSet questionSet,
                           QuestionSetFeedback feedback,
                           List<SaveFeedbackRequest.TimestampFeedbackItem> items) {
        if (items == null || items.isEmpty()) {
            return;
        }

        List<TimestampFeedback> timestamps = feedback.getTimestampFeedbacks();
        int size = Math.min(items.size(), timestamps.size());
        for (int i = 0; i < size; i++) {
            persistOne(questionSet, timestamps.get(i), items.get(i));
        }
    }

    private void persistOne(QuestionSet questionSet,
                            TimestampFeedback timestampFeedback,
                            SaveFeedbackRequest.TimestampFeedbackItem item) {
        Question question = timestampFeedback.getQuestion();
        if (question == null || question.getId() == null) {
            log.warn("nonverbal_score 저장 스킵: question 또는 questionId가 없습니다 (timestampFeedbackId={})",
                    timestampFeedback.getId());
            return;
        }

        Interview interview = questionSet.getInterview();
        SaveFeedbackRequest.NonverbalScore payload = item.getNonverbalScore();

        if (payload == null) {
            log.info("[정상 skip] Nonverbal payloadNull. interviewId={}, questionId={}",
                    interview.getId(), question.getId());
            return;
        }

        if (payload.getVocal() == null && payload.getVision() == null) {
            log.warn("[결함 skip] Nonverbal areasEmpty. interviewId={}, questionId={}",
                    interview.getId(), question.getId());
            return;
        }

        Map<String, DimensionScore> dims = new LinkedHashMap<>();
        mergeArea(dims, payload.getVocal());
        mergeArea(dims, payload.getVision());

        if (dims.isEmpty()) {
            log.warn("[결함 skip] Nonverbal allInvalid. interviewId={}, questionId={}",
                    interview.getId(), question.getId());
            return;
        }

        questionScorePersister.saveNonverbal(question.getId(), interview.getId(), dims);
    }

    private void mergeArea(Map<String, DimensionScore> dims, SaveFeedbackRequest.AreaScore area) {
        if (area == null || area.getDimensions() == null) {
            return;
        }
        for (SaveFeedbackRequest.DimensionScoreItem dim : area.getDimensions()) {
            if (!isValid(dim)) {
                continue;
            }
            dims.put(dim.getDimensionRef(),
                    DimensionScore.of(dim.getScore(), dim.getObservation(), dim.getEvidenceQuote()));
        }
    }

    private boolean isValid(SaveFeedbackRequest.DimensionScoreItem dim) {
        return dim != null
                && dim.getDimensionRef() != null && !dim.getDimensionRef().isBlank()
                && dim.getScore() != null
                && dim.getObservation() != null && !dim.getObservation().isBlank()
                && dim.getEvidenceQuote() != null && !dim.getEvidenceQuote().isBlank();
    }
}
