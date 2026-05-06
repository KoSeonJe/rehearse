package com.rehearse.api.domain.resume.repository;

import com.rehearse.api.domain.resume.entity.InterviewPlan;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

public interface InterviewPlanRepository extends JpaRepository<InterviewPlan, Long> {

    Optional<InterviewPlan> findByInterviewId(Long interviewId);

    @Modifying
    @Query("DELETE FROM InterviewPlan p WHERE p.interviewId = :interviewId")
    int deleteByInterviewId(Long interviewId);
}
