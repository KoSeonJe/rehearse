package com.rehearse.api.domain.feedback.rubric.repository;

import com.rehearse.api.domain.feedback.rubric.entity.NonverbalScore;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NonverbalScoreRepository extends JpaRepository<NonverbalScore, Long> {

    List<NonverbalScore> findByInterviewIdOrderByTurnIdAsc(Long interviewId);
}
