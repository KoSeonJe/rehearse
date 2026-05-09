package com.rehearse.api.domain.question.repository;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.QuestionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {

    List<QuestionSet> findByInterviewIdOrderByOrderIndex(Long interviewId);

    Optional<QuestionSet> findByInterviewIdAndCategory(Long interviewId, InterviewType category);

    @Query("SELECT qs FROM QuestionSet qs JOIN FETCH qs.questions WHERE qs.interview.id = :interviewId ORDER BY qs.orderIndex")
    List<QuestionSet> findByInterviewIdWithQuestions(@Param("interviewId") Long interviewId);

    long countByInterviewId(Long interviewId);
}
