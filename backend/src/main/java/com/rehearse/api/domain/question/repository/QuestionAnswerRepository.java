package com.rehearse.api.domain.question.repository;

import com.rehearse.api.domain.question.entity.QuestionAnswer;
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

    // 같은 질문셋에 대한 POST /answers 재호출 시 기존 행을 제거하고 새로 저장(멱등성 보장).
    // 프론트엔드의 면접 종료 복구 플로우가 경합 상황에서 중복 POST 를 일으킬 수 있어 defense-in-depth.
    @Modifying
    @Query("DELETE FROM QuestionAnswer a WHERE a.question.id IN (SELECT q.id FROM Question q WHERE q.questionSet.id = :questionSetId)")
    void deleteByQuestionSetId(@Param("questionSetId") Long questionSetId);
}
