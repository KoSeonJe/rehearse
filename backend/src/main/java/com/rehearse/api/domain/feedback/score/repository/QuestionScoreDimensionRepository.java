package com.rehearse.api.domain.feedback.score.repository;

import com.rehearse.api.domain.feedback.score.entity.QuestionScoreDimension;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionScoreDimensionRepository extends JpaRepository<QuestionScoreDimension, Long> {

    List<QuestionScoreDimension> findByQuestionScoreId(Long questionScoreId);
}
