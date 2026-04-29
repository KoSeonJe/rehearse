package com.rehearse.api.domain.feedback.rubric.nonverbal.repository;

import com.rehearse.api.domain.feedback.rubric.nonverbal.entity.NonverbalScoreEntity;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface NonverbalScoreRepository extends JpaRepository<NonverbalScoreEntity, Long> {

    List<NonverbalScoreEntity> findByInterviewIdOrderByTurnIdAsc(Long interviewId);
}
