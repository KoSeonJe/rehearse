package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.QuestionSet;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionSetRepository extends JpaRepository<QuestionSet, Long> {

    List<QuestionSet> findByInterviewIdOrderByOrderIndex(Long interviewId);

    @Query("SELECT qs FROM QuestionSet qs JOIN FETCH qs.questions WHERE qs.interview.id = :interviewId ORDER BY qs.orderIndex")
    List<QuestionSet> findByInterviewIdWithQuestions(@Param("interviewId") Long interviewId);

    long countByInterviewId(Long interviewId);
}
