package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface TimestampFeedbackRepository extends JpaRepository<TimestampFeedback, Long> {

    List<TimestampFeedback> findByQuestionSetFeedbackIdOrderByStartMs(Long questionSetFeedbackId);
}
