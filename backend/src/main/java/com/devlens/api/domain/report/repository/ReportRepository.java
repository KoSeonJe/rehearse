package com.devlens.api.domain.report.repository;

import com.devlens.api.domain.report.entity.InterviewReport;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface ReportRepository extends JpaRepository<InterviewReport, Long> {

    Optional<InterviewReport> findByInterviewId(Long interviewId);
}
