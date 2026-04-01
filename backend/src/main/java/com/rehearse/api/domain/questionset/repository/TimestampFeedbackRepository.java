package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface TimestampFeedbackRepository extends JpaRepository<TimestampFeedback, Long> {

    List<TimestampFeedback> findByQuestionSetFeedbackIdOrderByStartMs(Long questionSetFeedbackId);

    @Modifying
    @Query("DELETE FROM TimestampFeedback t WHERE t.questionSetFeedback.id IN (SELECT f.id FROM QuestionSetFeedback f WHERE f.questionSet.interview.id = :interviewId)")
    void deleteAllByInterviewId(@Param("interviewId") Long interviewId);
}
