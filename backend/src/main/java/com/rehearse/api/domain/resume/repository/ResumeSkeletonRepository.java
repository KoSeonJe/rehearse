package com.rehearse.api.domain.resume.repository;

import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ResumeSkeletonRepository extends JpaRepository<ResumeSkeletonEntity, Long> {

    Optional<ResumeSkeletonEntity> findByInterviewId(Long interviewId);
}
