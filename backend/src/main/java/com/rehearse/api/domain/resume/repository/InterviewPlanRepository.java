package com.rehearse.api.domain.resume.repository;

import com.rehearse.api.domain.resume.entity.InterviewPlanEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewPlanRepository extends JpaRepository<InterviewPlanEntity, Long> {

    Optional<InterviewPlanEntity> findByInterviewId(Long interviewId);
}
