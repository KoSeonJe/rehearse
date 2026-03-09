package com.devlens.api.domain.interview.repository;

import com.devlens.api.domain.interview.entity.Interview;
import org.springframework.data.jpa.repository.JpaRepository;

public interface InterviewRepository extends JpaRepository<Interview, Long> {
}
