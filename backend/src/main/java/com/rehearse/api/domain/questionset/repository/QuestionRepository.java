package com.rehearse.api.domain.questionset.repository;

import com.rehearse.api.domain.questionset.entity.Question;
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
}
