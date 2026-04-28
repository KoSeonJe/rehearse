package com.rehearse.api.domain.feedback.rubric.repository;

import com.rehearse.api.domain.feedback.rubric.entity.RubricScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.Optional;

public interface RubricScoreRepository extends JpaRepository<RubricScoreEntity, Long> {

    List<RubricScoreEntity> findByInterviewIdOrderByTurnIdAsc(Long interviewId);

    List<RubricScoreEntity> findByInterviewIdAndRubricId(Long interviewId, String rubricId);

    Optional<RubricScoreEntity> findFirstByInterviewIdAndTurnId(Long interviewId, Long turnId);
}
