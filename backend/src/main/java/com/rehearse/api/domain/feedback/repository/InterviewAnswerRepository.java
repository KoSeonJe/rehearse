package com.rehearse.api.domain.feedback.repository;

import com.rehearse.api.domain.feedback.entity.InterviewAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface InterviewAnswerRepository extends JpaRepository<InterviewAnswer, Long> {

    List<InterviewAnswer> findByInterviewIdOrderByQuestionIndex(Long interviewId);

    @Query("SELECT a FROM InterviewAnswer a JOIN FETCH a.interview WHERE a.interview.id = :interviewId ORDER BY a.questionIndex")
    List<InterviewAnswer> findByInterviewIdWithInterview(@Param("interviewId") Long interviewId);
}
