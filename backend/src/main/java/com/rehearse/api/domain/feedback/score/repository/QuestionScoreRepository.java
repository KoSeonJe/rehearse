package com.rehearse.api.domain.feedback.score.repository;

import com.rehearse.api.domain.feedback.score.entity.QuestionScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface QuestionScoreRepository extends JpaRepository<QuestionScore, Long> {

    Optional<QuestionScore> findByQuestionIdAndRubricId(Long questionId, String rubricId);

    List<QuestionScore> findByInterviewIdOrderByQuestionIdAsc(Long interviewId);
}
