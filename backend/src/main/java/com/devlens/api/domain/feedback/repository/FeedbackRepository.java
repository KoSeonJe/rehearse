package com.devlens.api.domain.feedback.repository;

import com.devlens.api.domain.feedback.entity.Feedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface FeedbackRepository extends JpaRepository<Feedback, Long> {

    List<Feedback> findByInterviewIdOrderByTimestampSeconds(Long interviewId);
}
