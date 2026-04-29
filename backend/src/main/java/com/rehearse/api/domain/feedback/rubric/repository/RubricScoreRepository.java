package com.rehearse.api.domain.feedback.rubric.repository;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RubricScoreRepository extends JpaRepository<RubricScore, Long> {

    List<RubricScore> findByInterviewIdOrderByTurnIdAsc(Long interviewId);

    List<RubricScore> findByInterviewIdAndRubricId(Long interviewId, String rubricId);

    Optional<RubricScore> findFirstByInterviewIdAndTurnId(Long interviewId, Long turnId);
}
