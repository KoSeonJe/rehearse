package com.rehearse.api.domain.feedback.rubric.service;

import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.rubric.entity.DimensionScore;
import com.rehearse.api.domain.feedback.score.service.QuestionScorePersister;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewType;
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

    private final NonverbalRubricScorer nonverbalRubricScorer;
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
            if (item.getNonverbalScore() == null) {
                log.info("[정상 skip] Nonverbal payloadNull. interviewId={}, questionId={}",
                        interview.getId(), question.getId());
            } else {
                SaveFeedbackRequest.NonverbalScore payload = item.getNonverbalScore();
                log.warn("[결함 skip] Nonverbal scoreEmpty. interviewId={}, questionId={}, "
                                + "fluency={}, confidenceTone={}, eyeContactPosture={}, composure={}",
                        interview.getId(), question.getId(),
                        payload.getFluency(), payload.getConfidenceTone(),
                        payload.getEyeContactPosture(), payload.getComposure());
            }
            return;
        }

        Map<String, DimensionScore> dims = new LinkedHashMap<>();
        dims.put("fluency", DimensionScore.of(score.fluency(), null, null));
        dims.put("confidence_tone", DimensionScore.of(score.confidenceTone(), null, null));
        dims.put("eye_contact_posture", DimensionScore.of(score.eyeContactPosture(), null, null));
        dims.put("composure", DimensionScore.of(score.composure(), null, null));

        questionScorePersister.saveNonverbal(question.getId(), interview.getId(), dims);
    }

    private String resolveTrack(Interview interview) {
        if (interview.getInterviewTypes().contains(InterviewType.RESUME_BASED)) {
            return "RESUME_BASED";
        }
        return null;
    }
}
