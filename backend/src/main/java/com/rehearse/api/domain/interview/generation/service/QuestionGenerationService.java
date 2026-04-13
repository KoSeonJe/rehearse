package com.rehearse.api.domain.interview.generation.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.vo.QuestionDistribution;
import com.rehearse.api.domain.interview.generation.pool.entity.QuestionPool;
import com.rehearse.api.domain.interview.generation.pool.service.CacheableQuestionProvider;
import com.rehearse.api.domain.interview.generation.pool.service.FreshQuestionProvider;
import com.rehearse.api.domain.feedback.entity.FeedbackPerspective;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionType;
import com.rehearse.api.domain.question.entity.ReferenceType;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
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
                QuestionSet qs = buildQuestionSet(
                        QuestionSetCategory.valueOf(type.name()),
                        qp.getContent(), qp.getTtsContent(), qp.getModelAnswer(),
                        qp.getReferenceType(), determinePerspective(type), qp);
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
            QuestionSetCategory category = resolveCategory(gq.getQuestionCategory());
            QuestionSet qs = buildQuestionSet(
                    category, gq.getContent(), gq.getTtsContent(), gq.getModelAnswer(),
                    gq.getReferenceType(), determinePerspective(category), null);
            result.add(qs);
        }

        log.info("[FRESH] 질문 제공 완료: interviewId={}, count={}", interviewId, result.size());
        return result;
    }

    private QuestionSet buildQuestionSet(QuestionSetCategory category, String questionText,
                                          String ttsText, String modelAnswer,
                                          String referenceType, FeedbackPerspective perspective,
                                          QuestionPool poolRef) {
        QuestionSet qs = QuestionSet.builder()
                .category(category)
                .orderIndex(0)
                .build();

        Question question = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText(questionText)
                .ttsText(ttsText)
                .modelAnswer(modelAnswer)
                .referenceType(parseReferenceType(referenceType))
                .feedbackPerspective(perspective)
                .orderIndex(0)
                .questionPool(poolRef)
                .build();

        qs.addQuestion(question);
        return qs;
    }

    private QuestionSetCategory resolveCategory(String questionCategory) {
        if (questionCategory == null) {
            throw new IllegalArgumentException("questionCategory must not be null");
        }
        if ("RESUME".equalsIgnoreCase(questionCategory)) {
            return QuestionSetCategory.RESUME_BASED;
        }
        try {
            return QuestionSetCategory.valueOf(questionCategory.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown questionCategory: " + questionCategory, e);
        }
    }

    private ReferenceType parseReferenceType(String refTypeStr) {
        if (refTypeStr == null) {
            throw new IllegalArgumentException("referenceType은 null일 수 없습니다");
        }
        try {
            return ReferenceType.valueOf(refTypeStr.toUpperCase());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException("Unknown referenceType: " + refTypeStr, e);
        }
    }

    private FeedbackPerspective determinePerspective(InterviewType type) {
        return switch (type) {
            case BEHAVIORAL -> FeedbackPerspective.BEHAVIORAL;
            case RESUME_BASED -> FeedbackPerspective.EXPERIENCE;
            default -> FeedbackPerspective.TECHNICAL;
        };
    }

    private FeedbackPerspective determinePerspective(QuestionSetCategory category) {
        return switch (category) {
            case BEHAVIORAL -> FeedbackPerspective.BEHAVIORAL;
            case RESUME_BASED -> FeedbackPerspective.EXPERIENCE;
            default -> FeedbackPerspective.TECHNICAL;
        };
    }
}
