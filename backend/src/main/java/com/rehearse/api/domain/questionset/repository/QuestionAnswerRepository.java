package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.QuestionAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionAnswerRepository extends JpaRepository<QuestionAnswer, Long> {

    @Query("SELECT a FROM QuestionAnswer a JOIN FETCH a.question q WHERE q.questionSet.id = :questionSetId ORDER BY a.startMs")
    List<QuestionAnswer> findByQuestionSetIdWithQuestion(@Param("questionSetId") Long questionSetId);

    @Modifying
    @Query("DELETE FROM QuestionAnswer a WHERE a.question.id IN (SELECT q.id FROM Question q WHERE q.questionSet.interview.id = :interviewId)")
    void deleteAllByInterviewId(@Param("interviewId") Long interviewId);
}
