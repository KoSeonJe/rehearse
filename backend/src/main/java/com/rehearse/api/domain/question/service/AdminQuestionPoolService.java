package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.dto.AdminQuestionPoolResponse;
import com.rehearse.api.domain.question.dto.AdminQuestionPoolSearchCondition;
import com.rehearse.api.domain.question.dto.CreateQuestionPoolRequest;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.repository.QuestionPoolRepository;
import jakarta.persistence.criteria.Predicate;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class AdminQuestionPoolService {

    private final QuestionPoolRepository questionPoolRepository;

    public Page<AdminQuestionPoolResponse> search(AdminQuestionPoolSearchCondition condition, Pageable pageable) {
        Pageable sortedPageable = PageRequest.of(
                pageable.getPageNumber(),
                pageable.getPageSize(),
                Sort.by(Sort.Direction.DESC, "createdAt").and(Sort.by(Sort.Direction.DESC, "id")));

        return questionPoolRepository.findAll(specification(condition), sortedPageable)
                .map(AdminQuestionPoolResponse::from);
    }

    @Transactional
    public AdminQuestionPoolResponse create(CreateQuestionPoolRequest request) {
        QuestionPool questionPool = QuestionPool.create(
                request.cacheKey(),
                request.content(),
                request.ttsContent(),
                request.category(),
                request.bestAnswer());

        return AdminQuestionPoolResponse.from(questionPoolRepository.save(questionPool));
    }

    private Specification<QuestionPool> specification(AdminQuestionPoolSearchCondition condition) {
        return (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();

            likeIfPresent(predicates, cb, root.get("cacheKey"), condition.cacheKey());
            likeIfPresent(predicates, cb, root.get("category"), condition.category());

            if (condition.isActive() != null) {
                predicates.add(cb.equal(root.get("isActive"), condition.isActive()));
            }

            String keyword = normalize(condition.keyword());
            if (keyword != null) {
                String pattern = "%" + keyword + "%";
                predicates.add(cb.or(
                        cb.like(cb.lower(root.get("content")), pattern),
                        cb.like(cb.lower(root.get("bestAnswer")), pattern)));
            }

            return cb.and(predicates.toArray(Predicate[]::new));
        };
    }

    private void likeIfPresent(List<Predicate> predicates,
                               jakarta.persistence.criteria.CriteriaBuilder cb,
                               jakarta.persistence.criteria.Expression<String> expression,
                               String value) {
        String normalized = normalize(value);
        if (normalized != null) {
            predicates.add(cb.like(cb.lower(expression), "%" + normalized + "%"));
        }
    }

    private String normalize(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim().toLowerCase(Locale.ROOT);
    }
}
