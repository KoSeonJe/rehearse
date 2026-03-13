package com.devlens.api.domain.feedback.repository;

import com.devlens.api.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByInterviewIdOrderByTimestampSeconds(Long interviewId);

    @Query("SELECT f FROM Feedback f JOIN FETCH f.interview WHERE f.interview.id = :interviewId ORDER BY f.timestampSeconds")
    List<Feedback> findByInterviewIdWithInterview(@Param("interviewId") Long interviewId);
}
