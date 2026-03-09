package com.devlens.api.domain.feedback.repository;

import com.devlens.api.domain.feedback.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    List<InterviewAnswer> findByInterviewIdOrderByQuestionIndex(Long interviewId);
}
