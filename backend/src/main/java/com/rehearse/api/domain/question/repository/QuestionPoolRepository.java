package com.rehearse.api.domain.question.repository;

import com.rehearse.api.domain.question.entity.QuestionPool;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;

import java.util.List;

public interface QuestionPoolRepository extends JpaRepository<QuestionPool, Long>,
        JpaSpecificationExecutor<QuestionPool> {

    long countByCacheKeyAndIsActiveTrue(String cacheKey);

    List<QuestionPool> findByCacheKeyAndIsActiveTrue(String cacheKey);

    List<QuestionPool> findByCacheKeyAndIsActiveTrueAndCategoryIn(String cacheKey, List<String> categories);
}
