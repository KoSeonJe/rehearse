package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.entity.QuestionDistribution;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.prompt.QuestionCountCalculator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Component
public class StandardTrackQuestionGenerator {

    private static final int PARALLEL_TIMEOUT_SEC = 60;

    private final CacheableQuestionProvider cacheableProvider;
    private final FreshQuestionProvider freshProvider;
    private final QuestionSetAssembler assembler;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public StandardTrackQuestionGenerator(CacheableQuestionProvider cacheableProvider,
                                          FreshQuestionProvider freshProvider,
                                          QuestionSetAssembler assembler) {
        this.cacheableProvider = cacheableProvider;
        this.freshProvider = freshProvider;
        this.assembler = assembler;
    }

    @PreDestroy
    void shutdown() {
        virtualExecutor.close();
    }

    public List<QuestionSet> generate(Long interviewId, Long userId, Position position,
                                      InterviewLevel level, List<InterviewType> interviewTypes,
                                      List<String> csSubTopics, String resumeText,
                                      Integer durationMinutes, TechStack techStack) {

        int totalCount = QuestionCountCalculator.calculate(durationMinutes, interviewTypes.size());
        QuestionDistribution distribution = QuestionDistribution.create(interviewTypes, totalCount);

        TechStack effectiveTechStack = techStack != null
                ? techStack : TechStack.getDefaultForPosition(position);

        CompletableFuture<List<QuestionSet>> cacheableFuture = CompletableFuture.supplyAsync(() ->
                provideCacheable(interviewId, userId, position, level, effectiveTechStack,
                        distribution.getCacheableTypes(), csSubTopics),
                virtualExecutor
        ).orTimeout(PARALLEL_TIMEOUT_SEC, TimeUnit.SECONDS);

        CompletableFuture<List<QuestionSet>> freshFuture = CompletableFuture.supplyAsync(() ->
                provideFresh(interviewId, position, level, effectiveTechStack,
                        distribution.getFreshTypes(), resumeText, csSubTopics, durationMinutes),
                virtualExecutor
        ).orTimeout(PARALLEL_TIMEOUT_SEC, TimeUnit.SECONDS);

        List<QuestionSet> all = new ArrayList<>();
        try {
            all.addAll(cacheableFuture.join());
            all.addAll(freshFuture.join());
        } catch (CompletionException e) {
            cacheableFuture.cancel(true);
            freshFuture.cancel(true);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("질문 생성 병렬 처리 실패: " + cause.getMessage(), cause);
        }

        for (int i = 0; i < all.size(); i++) {
            all.get(i).updateOrderIndex(i);
        }
        return all;
    }

    private List<QuestionSet> provideCacheable(
            Long interviewId, Long userId, Position position, InterviewLevel level,
            TechStack techStack, Map<InterviewType, Integer> typeDistribution,
            List<String> csSubTopics) {

        List<QuestionSet> result = new ArrayList<>();
        for (var entry : typeDistribution.entrySet()) {
            InterviewType type = entry.getKey();
            int count = entry.getValue();

            List<QuestionPool> poolQuestions = cacheableProvider.provide(
                    userId, position, level, techStack, type, count, csSubTopics);

            for (QuestionPool qp : poolQuestions) {
                result.add(assembler.fromPool(type, qp));
            }
        }

        log.info("[CACHEABLE] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }

    private List<QuestionSet> provideFresh(
            Long interviewId, Position position, InterviewLevel level,
            TechStack techStack, Map<InterviewType, Integer> typeDistribution,
            String resumeText, List<String> csSubTopics, Integer durationMinutes) {

        if (typeDistribution.isEmpty()) {
            return List.of();
        }

        int totalFreshCount = typeDistribution.values().stream().mapToInt(Integer::intValue).sum();
        Set<InterviewType> freshTypeSet = typeDistribution.keySet();

        List<GeneratedQuestion> generated = freshProvider.provide(
                position, level, techStack, freshTypeSet,
                totalFreshCount, resumeText, csSubTopics, durationMinutes);

        List<QuestionSet> result = new ArrayList<>();
        for (GeneratedQuestion gq : generated) {
            result.add(assembler.fromGenerated(gq));
        }

        log.info("[FRESH] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }
}
