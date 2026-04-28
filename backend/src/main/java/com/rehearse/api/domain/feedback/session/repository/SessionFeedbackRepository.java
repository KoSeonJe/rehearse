package com.rehearse.api.domain.feedback.session.repository;

import com.rehearse.api.domain.feedback.session.entity.SessionFeedback;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

public interface SessionFeedbackRepository extends JpaRepository<SessionFeedback, Long> {

    Optional<SessionFeedback> findByInterviewId(Long interviewId);

    List<SessionFeedback> findByStatusAndCreatedAtBefore(SessionFeedbackStatus status, LocalDateTime cutoff);
}
