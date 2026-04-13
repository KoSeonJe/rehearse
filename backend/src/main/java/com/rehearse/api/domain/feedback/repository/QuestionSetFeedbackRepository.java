package com.rehearse.api.domain.feedback.repository;

import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionSetFeedbackRepository extends JpaRepository<QuestionSetFeedback, Long> {

    Optional<QuestionSetFeedback> findByQuestionSetId(Long questionSetId);

    @Query("SELECT f FROM QuestionSetFeedback f JOIN FETCH f.timestampFeedbacks WHERE f.questionSet.id = :questionSetId")
    Optional<QuestionSetFeedback> findByQuestionSetIdWithTimestampFeedbacks(@Param("questionSetId") Long questionSetId);

    @Query("SELECT f FROM QuestionSetFeedback f WHERE f.questionSet.id IN :questionSetIds")
    List<QuestionSetFeedback> findByQuestionSetIdIn(@Param("questionSetIds") List<Long> questionSetIds);

    @Modifying
    @Query("DELETE FROM QuestionSetFeedback f WHERE f.questionSet.interview.id = :interviewId")
    void deleteAllByInterviewId(@Param("interviewId") Long interviewId);
}
