package com.rehearse.api.domain.question.repository;

import com.rehearse.api.domain.question.entity.Question;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Set;

public interface QuestionRepository extends JpaRepository<Question, Long> {

    List<Question> findByQuestionSetIdOrderByOrderIndex(Long questionSetId);

    @Query("SELECT DISTINCT q.questionPool.id FROM Question q " +
            "JOIN q.questionSet qs JOIN qs.interview i " +
            "WHERE i.userId = :userId AND q.questionPool IS NOT NULL " +
            "AND q.questionPool.cacheKey = :cacheKey")
    Set<Long> findUsedQuestionPoolIdsByUserIdAndCacheKey(
            @Param("userId") Long userId, @Param("cacheKey") String cacheKey);

    @Query("""
            SELECT q FROM Question q
            JOIN q.questionSet qs
            WHERE qs.interview.id = :interviewId
              AND EXISTS (
                SELECT 1 FROM TimestampFeedback tf
                WHERE tf.question.id = q.id
                  AND tf.transcript IS NOT NULL
                  AND tf.transcript <> ''
              )
              AND NOT EXISTS (SELECT 1 FROM QuestionScore s WHERE s.questionId = q.id)
            """)
    List<Question> findAnsweredButUnscoredByInterviewId(@Param("interviewId") Long interviewId);
}
