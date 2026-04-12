package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.vo.QuestionDistribution;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.service.CacheableQuestionProvider;
import com.rehearse.api.domain.questionpool.service.FreshQuestionProvider;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.entity.FeedbackPerspective;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.prompt.QuestionCountCalculator;
import jakarta.annotation.PreDestroy;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CompletionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;

@Slf4j
@Service
public class QuestionGenerationService {

    private final QuestionGenerationTransactionHandler transactionHandler;
    private final CacheableQuestionProvider cacheableProvider;
    private final FreshQuestionProvider freshProvider;
    private final ExecutorService virtualExecutor = Executors.newVirtualThreadPerTaskExecutor();

    public QuestionGenerationService(
            QuestionGenerationTransactionHandler transactionHandler,
            CacheableQuestionProvider cacheableProvider,
            FreshQuestionProvider freshProvider) {
        this.transactionHandler = transactionHandler;
        this.cacheableProvider = cacheableProvider;
        this.freshProvider = freshProvider;
    }

    @PreDestroy
    void shutdown() {
        virtualExecutor.close();
    }

    public void generateQuestions(Long interviewId, Long userId, Position position,
                                  InterviewLevel level, List<InterviewType> interviewTypes,
                                  List<String> csSubTopics, String resumeText,
                                  Integer durationMinutes, TechStack techStack) {

        // Phase A: 상태 전환 (별도 트랜잭션 — 외부 Bean 호출로 프록시 적용)
        transactionHandler.startGeneration(interviewId);

        // 유형별 질문 수 배분 및 CACHEABLE / FRESH 분류
        int totalCount = QuestionCountCalculator.calculate(durationMinutes, interviewTypes.size());
        QuestionDistribution distribution = QuestionDistribution.create(interviewTypes, totalCount);

        Map<InterviewType, Integer> cacheableTypes = distribution.getCacheableTypes();
        Map<InterviewType, Integer> freshTypes = distribution.getFreshTypes();

        TechStack effectiveTechStack = techStack != null
                ? techStack : TechStack.getDefaultForPosition(position);

        // Phase B: 병렬 질문 생성
        CompletableFuture<List<QuestionSet>> cacheableFuture = CompletableFuture.supplyAsync(() ->
                provideCacheableQuestions(interviewId, userId, position, level, effectiveTechStack,
                        cacheableTypes, csSubTopics),
                virtualExecutor
        ).orTimeout(60, TimeUnit.SECONDS);

        CompletableFuture<List<QuestionSet>> freshFuture = CompletableFuture.supplyAsync(() ->
                provideFreshQuestions(interviewId, position, level, effectiveTechStack,
                        freshTypes, resumeText, csSubTopics, durationMinutes),
                virtualExecutor
        ).orTimeout(60, TimeUnit.SECONDS);

        List<QuestionSet> allQuestionSets = new ArrayList<>();
        try {
            allQuestionSets.addAll(cacheableFuture.join());
            allQuestionSets.addAll(freshFuture.join());
        } catch (CompletionException e) {
            cacheableFuture.cancel(true);
            freshFuture.cancel(true);
            Throwable cause = e.getCause() != null ? e.getCause() : e;
            throw new RuntimeException("질문 생성 병렬 처리 실패: " + cause.getMessage(), cause);
        }

        // question_order 재배정
        for (int i = 0; i < allQuestionSets.size(); i++) {
            allQuestionSets.get(i).updateOrderIndex(i);
        }

        // Phase C: 결과 저장 (별도 트랜잭션 — 외부 Bean 호출로 프록시 적용)
        transactionHandler.saveResults(interviewId, allQuestionSets);
    }

    private List<QuestionSet> provideCacheableQuestions(
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
                QuestionSet qs = QuestionSet.builder()
                        .category(type.name())
                        .orderIndex(0)
                        .build();

                Question question = Question.builder()
                        .questionType(QuestionType.MAIN)
                        .questionText(qp.getContent())
                        .modelAnswer(qp.getModelAnswer())
                        .referenceType(parseReferenceType(qp.getReferenceType()))
                        .feedbackPerspective(determinePerspective(type))
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
                    .category(resolveInterviewType(gq.getQuestionCategory()))
                    .orderIndex(0)
                    .build();

            FeedbackPerspective perspective = parseFeedbackPerspective(gq.getQuestionCategory());
            Question question = Question.builder()
                    .questionType(QuestionType.MAIN)
                    .questionText(gq.getContent())
                    .modelAnswer(gq.getModelAnswer())
                    .referenceType(parseReferenceType(gq.getReferenceType()))
                    .feedbackPerspective(perspective)
                    .orderIndex(0)
                    .build();

            qs.addQuestion(question);
            result.add(qs);
        }

        log.info("[FRESH] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }

    private String resolveInterviewType(String questionCategory) {
        if (questionCategory == null) return InterviewType.CS_FUNDAMENTAL.name();
        try {
            return InterviewType.valueOf(questionCategory.toUpperCase()).name();
        } catch (IllegalArgumentException ignored) {}
        if ("RESUME".equalsIgnoreCase(questionCategory)) return InterviewType.RESUME_BASED.name();
        return InterviewType.CS_FUNDAMENTAL.name();
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

    private FeedbackPerspective determinePerspective(InterviewType type) {
        return switch (type) {
            case BEHAVIORAL -> FeedbackPerspective.BEHAVIORAL;
            case RESUME_BASED -> FeedbackPerspective.EXPERIENCE;
            default -> FeedbackPerspective.TECHNICAL;
        };
    }

    private FeedbackPerspective parseFeedbackPerspective(String questionCategory) {
        if ("RESUME".equalsIgnoreCase(questionCategory)) {
            return FeedbackPerspective.EXPERIENCE;
        }
        return FeedbackPerspective.TECHNICAL;
    }
}
