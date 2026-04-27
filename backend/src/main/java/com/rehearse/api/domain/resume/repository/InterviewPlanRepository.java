package com.rehearse.api.domain.resume.repository;

import com.rehearse.api.domain.resume.domain.InterviewPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPlanRepository extends JpaRepository<InterviewPlan, Long> {

    Optional<InterviewPlan> findByInterviewId(Long interviewId);
}
