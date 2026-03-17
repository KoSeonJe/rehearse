package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.QuestionSetFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface QuestionSetFeedbackRepository extends JpaRepository<QuestionSetFeedback, Long> {

    Optional<QuestionSetFeedback> findByQuestionSetId(Long questionSetId);

    @Query("SELECT f FROM QuestionSetFeedback f JOIN FETCH f.timestampFeedbacks WHERE f.questionSet.id = :questionSetId")
    Optional<QuestionSetFeedback> findByQuestionSetIdWithTimestampFeedbacks(@Param("questionSetId") Long questionSetId);
}
