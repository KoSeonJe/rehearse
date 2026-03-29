package com.rehearse.api.domain.questionpool.repository;

import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long> {

    long countByCacheKeyAndIsActiveTrue(String cacheKey);

    long countByCacheKeyAndIsActiveTrueAndCategoryIn(String cacheKey, List<String> categories);

    List<QuestionPool> findByCacheKeyAndIsActiveTrue(String cacheKey);

    List<QuestionPool> findByCacheKeyAndIsActiveTrueAndCategoryIn(String cacheKey, List<String> categories);
}
