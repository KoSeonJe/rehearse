package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.QuestionSetAnswer;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface QuestionSetAnswerRepository extends JpaRepository<QuestionSetAnswer, Long> {

    @Query("SELECT a FROM QuestionSetAnswer a JOIN FETCH a.question q WHERE q.questionSet.id = :questionSetId ORDER BY a.startMs")
    List<QuestionSetAnswer> findByQuestionSetIdWithQuestion(@Param("questionSetId") Long questionSetId);
}
