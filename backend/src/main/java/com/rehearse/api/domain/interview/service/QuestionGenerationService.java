package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.questionpool.config.CacheStrategyConfig;
import com.rehearse.api.domain.questionpool.entity.CacheStrategy;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.service.CacheableQuestionProvider;
import com.rehearse.api.domain.questionpool.service.FreshQuestionProvider;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.prompt.QuestionCountCalculator;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.event.TransactionPhase;
import org.springframework.transaction.event.TransactionalEventListener;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Slf4j
@Service
public class QuestionGenerationService {

    private final QuestionGenerationTransactionHelper transactionHelper;
    private final CacheableQuestionProvider cacheableProvider;
    private final FreshQuestionProvider freshProvider;
    private final Executor questionSubTaskExecutor;

    public QuestionGenerationService(
            QuestionGenerationTransactionHelper transactionHelper,
            CacheableQuestionProvider cacheableProvider,
            FreshQuestionProvider freshProvider,
            @Qualifier("questionSubTaskExecutor") Executor questionSubTaskExecutor) {
        this.transactionHelper = transactionHelper;
        this.cacheableProvider = cacheableProvider;
        this.freshProvider = freshProvider;
        this.questionSubTaskExecutor = questionSubTaskExecutor;
    }

    @Async("questionSubTaskExecutor")
    @TransactionalEventListener(phase = TransactionPhase.AFTER_COMMIT)
    public void handleQuestionGenerationEvent(QuestionGenerationRequestedEvent event) {
        try {
            generateQuestions(event.getInterviewId(), event.getPosition(), event.getPositionDetail(),
                    event.getLevel(), event.getInterviewTypes(), event.getCsSubTopics(),
                    event.getResumeText(), event.getDurationMinutes(), event.getTechStack());
        } catch (Exception e) {
            log.error("질문 생성 비동기 작업 실패: interviewId={}", event.getInterviewId(), e);
            String reason = e.getCause() != null ? e.getCause().getMessage() : e.getMessage();
            transactionHelper.failGeneration(event.getInterviewId(),
                    reason != null ? reason : "알 수 없는 오류");
        }
    }

    public void generateQuestions(Long interviewId, Position position, String positionDetail,
                                  InterviewLevel level, List<InterviewType> interviewTypes,
                                  List<String> csSubTopics, String resumeText,
                                  Integer durationMinutes, TechStack techStack) {

        // Phase A: 상태 전환 (별도 트랜잭션)
        transactionHelper.startGeneration(interviewId);

        // 유형별 질문 수 배분
        int totalCount = QuestionCountCalculator.calculate(durationMinutes, interviewTypes.size());
        Map<InterviewType, Integer> distribution = distributeQuestionCount(interviewTypes, totalCount);

        // CACHEABLE / FRESH 분류
        Map<CacheStrategy, Map<InterviewType, Integer>> grouped = distribution.entrySet().stream()
                .collect(Collectors.groupingBy(
                        e -> CacheStrategyConfig.getStrategy(e.getKey()),
                        Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue)));

        Map<InterviewType, Integer> cacheableTypes = grouped.getOrDefault(CacheStrategy.CACHEABLE, Map.of());
        Map<InterviewType, Integer> freshTypes = grouped.getOrDefault(CacheStrategy.FRESH, Map.of());

        TechStack effectiveTechStack = techStack != null
                ? techStack : TechStack.getDefaultForPosition(position);

        // Phase B: 병렬 질문 생성
        CompletableFuture<List<QuestionSet>> cacheableFuture = CompletableFuture.supplyAsync(() ->
                provideCacheableQuestions(interviewId, position, level, effectiveTechStack,
                        cacheableTypes, csSubTopics),
                questionSubTaskExecutor
        ).orTimeout(60, TimeUnit.SECONDS);

        CompletableFuture<List<QuestionSet>> freshFuture = CompletableFuture.supplyAsync(() ->
                provideFreshQuestions(interviewId, position, level, effectiveTechStack,
                        freshTypes, resumeText, csSubTopics, durationMinutes),
                questionSubTaskExecutor
        ).orTimeout(60, TimeUnit.SECONDS);

        List<QuestionSet> allQuestionSets = new ArrayList<>();
        try {
            allQuestionSets.addAll(cacheableFuture.join());
            allQuestionSets.addAll(freshFuture.join());
        } catch (java.util.concurrent.CompletionException e) {
            cacheableFuture.cancel(true);
            freshFuture.cancel(true);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("질문 생성 병렬 처리 실패: " + cause.getMessage(), cause);
        }

        // question_order 재배정
        for (int i = 0; i < allQuestionSets.size(); i++) {
            allQuestionSets.get(i).updateOrderIndex(i);
        }

        // Phase C: 결과 저장 (별도 트랜잭션)
        transactionHelper.saveResults(interviewId, allQuestionSets);
    }

    private List<QuestionSet> provideCacheableQuestions(
            Long interviewId, Position position, InterviewLevel level,
            TechStack techStack, Map<InterviewType, Integer> typeDistribution,
            List<String> csSubTopics) {

        List<QuestionSet> result = new ArrayList<>();
        for (var entry : typeDistribution.entrySet()) {
            InterviewType type = entry.getKey();
            int count = entry.getValue();

            List<QuestionPool> poolQuestions = cacheableProvider.provide(
                    position, level, techStack, type, count, csSubTopics);

            for (QuestionPool qp : poolQuestions) {
                QuestionSet qs = QuestionSet.builder()
                        .category(parseQuestionCategory(qp.getCategory()))
                        .orderIndex(0)
                        .build();

                Question question = Question.builder()
                        .questionType(QuestionType.MAIN)
                        .questionText(qp.getContent())
                        .modelAnswer(qp.getModelAnswer())
                        .referenceType(parseReferenceType(qp.getReferenceType()))
                        .orderIndex(0)
                        .questionPool(qp)
                        .build();

                qs.addQuestion(question);
                result.add(qs);
            }
        }

        log.info("[CACHEABLE] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }

    private List<QuestionSet> provideFreshQuestions(
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
            QuestionSet qs = QuestionSet.builder()
                    .category(parseQuestionCategory(gq.getQuestionCategory()))
                    .orderIndex(0)
                    .build();

            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText(gq.getContent())
                    .modelAnswer(gq.getModelAnswer())
                    .referenceType(parseReferenceType(gq.getReferenceType()))
                    .orderIndex(0)
                    .build();

            qs.addQuestion(question);
            result.add(qs);
        }

        log.info("[FRESH] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }

    private Map<InterviewType, Integer> distributeQuestionCount(
            List<InterviewType> types, int totalCount) {
        int base = totalCount / types.size();
        int remainder = totalCount % types.size();

        Map<InterviewType, Integer> distribution = new LinkedHashMap<>();
        for (int i = 0; i < types.size(); i++) {
            distribution.put(types.get(i), base + (i < remainder ? 1 : 0));
        }
        return distribution;
    }

    private QuestionCategory parseQuestionCategory(String categoryStr) {
        if (categoryStr != null) {
            try {
                return QuestionCategory.valueOf(categoryStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return QuestionCategory.CS;
    }

    private ReferenceType parseReferenceType(String refTypeStr) {
        if (refTypeStr != null) {
            try {
                return ReferenceType.valueOf(refTypeStr.toUpperCase());
            } catch (IllegalArgumentException ignored) {
            }
        }
        return ReferenceType.GUIDE;
    }
}
