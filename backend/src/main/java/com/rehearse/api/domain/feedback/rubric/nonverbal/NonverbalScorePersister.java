package com.rehearse.api.domain.feedback.rubric.nonverbal;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.nonverbal.entity.NonverbalScoreEntity;
import com.rehearse.api.domain.feedback.rubric.nonverbal.repository.NonverbalScoreRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Map;

@Slf4j
@Component
@RequiredArgsConstructor
public class NonverbalScorePersister {

    private final NonverbalScoreRepository nonverbalScoreRepository;
    private final NonverbalRubricScorer nonverbalRubricScorer;
    private final ObjectMapper objectMapper;

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
        if (timestampFeedback.getId() == null) {
            log.debug("nonverbal_score 저장 스킵: timestampFeedback id가 아직 없습니다");
            return;
        }

        Interview interview = questionSet.getInterview();
        String category = questionSet.getCategory().name();
        String track = resolveTrack(interview);
        String difficulty = item.getDifficulty() != null ? item.getDifficulty() : "easy";

        NonverbalTurnScore score = nonverbalRubricScorer.score(
                item.getNonverbalScore(),
                category,
                track,
                item.getResumeMode(),
                difficulty
        );
        if (!score.hasAnyScore()) {
            return;
        }

        nonverbalScoreRepository.save(NonverbalScoreEntity.from(
                interview.getId(),
                timestampFeedback.getId(),
                score,
                toJson(score.rawSignals())
        ));
    }

    private String resolveTrack(Interview interview) {
        if (interview.getInterviewTypes().contains(InterviewType.RESUME_BASED)) {
            return "RESUME_BASED";
        }
        return null;
    }

    private String toJson(Map<String, Object> rawSignals) {
        try {
            return objectMapper.writeValueAsString(rawSignals);
        } catch (JsonProcessingException e) {
            throw new IllegalStateException("nonverbal rawSignals 직렬화 실패", e);
        }
    }
}
