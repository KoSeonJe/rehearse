package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.global.support.TestFixtures;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.times;

@ExtendWith(MockitoExtension.class)
class CacheableQuestionProviderTest {

    @InjectMocks
    private CacheableQuestionProvider cacheableQuestionProvider;

    @Mock
    private QuestionPoolService questionPoolService;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private AiClient aiClient;

    @Mock
    private QuestionGenerationLock questionGenerationLock;

    private static final Position POSITION = Position.BACKEND;
    private static final InterviewLevel LEVEL = InterviewLevel.JUNIOR;
    private static final TechStack TECH_STACK = TechStack.JAVA_SPRING;

    @Nested
    @DisplayName("캐시 히트 시나리오")
    class CacheHit {

        @Test
        @DisplayName("Pool이 충분하면 AI를 호출하지 않고 Pool에서 질문을 반환한다")
        void poolSufficient_returnsFromPool_withoutAiCall() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "질문1"),
                    TestFixtures.createQuestionPool(cacheKey, "질문2"),
                    TestFixtures.createQuestionPool(cacheKey, "질문3")
            );

            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(poolQuestions);

            // when
            List<QuestionPool> result = cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            assertThat(result).hasSize(3);
            then(aiClient).should(never()).generateQuestions(any());
        }

        @Test
        @DisplayName("userId가 null이면 usedPoolIds 조회를 스킵하고 빈 Set으로 처리한다")
        void userIdNull_skipsUsedPoolIdsQuery() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), eq(Set.of())))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), anyList(), eq(Set.of())))
                    .willReturn(List.of(TestFixtures.createQuestionPool(cacheKey, "질문1")));

            // when
            cacheableQuestionProvider.provide(null, POSITION, LEVEL, TECH_STACK,
                    InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            then(questionRepository).should(never())
                    .findUsedQuestionPoolIdsByUserIdAndCacheKey(any(), any());
        }

        @Test
        @DisplayName("userId가 존재하면 기사용 질문 풀 ID를 조회하여 제외 처리한다")
        void userIdPresent_queriesUsedPoolIds() {
            // given
            Long userId = 1L;
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            Set<Long> usedIds = Set.of(10L, 20L);

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(userId, cacheKey))
                    .willReturn(usedIds);
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), eq(usedIds)))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), anyList(), eq(usedIds)))
                    .willReturn(List.of(TestFixtures.createQuestionPool(cacheKey, "질문1")));

            // when
            cacheableQuestionProvider.provide(userId, POSITION, LEVEL, TECH_STACK,
                    InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            then(questionRepository).should()
                    .findUsedQuestionPoolIdsByUserIdAndCacheKey(userId, cacheKey);
        }
    }

    @Nested
    @DisplayName("캐시 미스 및 AI 호출 시나리오")
    class CacheMiss {

        @Test
        @DisplayName("Pool이 부족하면 AI를 호출하고 결과를 Pool에 저장 후 반환한다")
        void poolInsufficient_callsAi_savesAndReturns() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            ReentrantLock lock = new ReentrantLock();
            List<GeneratedQuestion> generated = List.of(makeGeneratedQuestion("AI질문1"));
            List<QuestionPool> convertedPool = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "AI질문1")
            );

            // 첫 번째 호출(lock 전): false, 두 번째 호출(lock 후 재확인): false
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(false, false);
            given(questionGenerationLock.acquire(cacheKey)).willReturn(lock);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willReturn(generated);
            given(questionPoolService.convertAndCacheIfEligible(eq(cacheKey), eq(generated)))
                    .willReturn(convertedPool);
            given(questionPoolService.selectWithCategoryDistribution(eq(convertedPool), eq(3)))
                    .willReturn(convertedPool);

            // when
            List<QuestionPool> result = cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            assertThat(result).isNotEmpty();
            then(aiClient).should().generateQuestions(any(QuestionGenerationRequest.class));
            then(questionPoolService).should().convertAndCacheIfEligible(eq(cacheKey), eq(generated));
        }

        @Test
        @DisplayName("Lock 해제는 AI 호출 성공 여부와 무관하게 finally에서 반드시 호출된다")
        void lockRelease_calledInFinally_onSuccess() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            ReentrantLock lock = new ReentrantLock();
            List<GeneratedQuestion> generated = List.of(makeGeneratedQuestion("AI질문1"));
            List<QuestionPool> convertedPool = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "AI질문1")
            );

            // 첫 번째 호출(lock 전): false, 두 번째 호출(lock 후 재확인): false
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(1), anyList(), anySet()))
                    .willReturn(false, false);
            given(questionGenerationLock.acquire(cacheKey)).willReturn(lock);
            given(aiClient.generateQuestions(any())).willReturn(generated);
            given(questionPoolService.convertAndCacheIfEligible(any(), any())).willReturn(convertedPool);
            given(questionPoolService.selectWithCategoryDistribution(any(), anyInt()))
                    .willReturn(convertedPool);

            // when
            cacheableQuestionProvider.provide(null, POSITION, LEVEL, TECH_STACK,
                    InterviewType.CS_FUNDAMENTAL, 1, null);

            // then
            then(questionGenerationLock).should().release(lock);
        }

        @Test
        @DisplayName("AI 호출 실패 시 예외가 전파되고 Lock이 반드시 해제된다")
        void aiCallFails_exceptionPropagated_lockReleased() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            ReentrantLock lock = new ReentrantLock();

            // 첫 번째 호출(lock 전): false, 두 번째 호출(lock 후 재확인): false
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(false, false);
            given(questionGenerationLock.acquire(cacheKey)).willReturn(lock);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willThrow(new RuntimeException("AI 서비스 오류"));

            // when & then
            assertThatThrownBy(() -> cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, null))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessageContaining("AI 서비스 오류");

            then(questionGenerationLock).should().release(lock);
        }
    }

    @Nested
    @DisplayName("Stampede 보호 시나리오")
    class StampedeProtection {

        @Test
        @DisplayName("Lock 획득 후 재확인 시 Pool이 충분하면 AI를 호출하지 않는다")
        void afterLockAcquire_recheck_poolSufficient_noAiCall() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            ReentrantLock lock = new ReentrantLock();
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "질문1")
            );

            // 첫 번째 isPoolSufficient 호출(lock 획득 전): false
            // 두 번째 isPoolSufficient 호출(lock 획득 후 재확인): true
            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(false)
                    .willReturn(true);
            given(questionGenerationLock.acquire(cacheKey)).willReturn(lock);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), anyList(), anySet()))
                    .willReturn(poolQuestions);

            // when
            List<QuestionPool> result = cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            assertThat(result).isNotEmpty();
            then(aiClient).should(never()).generateQuestions(any());
            then(questionGenerationLock).should().release(lock);
        }
    }

    @Nested
    @DisplayName("InterviewType별 카테고리 필터 처리")
    class CategoryFilterByType {

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입에 csSubTopics가 주어지면 카테고리명으로 변환된 필터를 사용한다")
        void csType_withSubTopics_usesCategoryFilter() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            List<String> csSubTopics = List.of("OS", "NETWORK");
            // CsSubTopic.OS → "운영체제", CsSubTopic.NETWORK → "네트워크"
            List<String> expectedFilter = List.of("운영체제", "네트워크");
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "OS질문", null, "운영체제")
            );

            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), eq(expectedFilter), anySet()))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), eq(expectedFilter), anySet()))
                    .willReturn(poolQuestions);

            // when
            List<QuestionPool> result = cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, csSubTopics);

            // then
            assertThat(result).hasSize(1);
            then(questionPoolService).should()
                    .isPoolSufficient(eq(cacheKey), eq(3), eq(expectedFilter), anySet());
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입에 csSubTopics가 null이면 전체 CS 카테고리를 필터로 사용한다")
        void csType_nullSubTopics_usesAllCsCategories() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            // CsSubTopic.allCategoryNames() = ["자료구조", "운영체제", "네트워크", "데이터베이스"]
            List<String> allCategories = List.of("자료구조", "운영체제", "네트워크", "데이터베이스");
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "CS질문")
            );

            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), eq(allCategories), anySet()))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), eq(allCategories), anySet()))
                    .willReturn(poolQuestions);

            // when
            cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.CS_FUNDAMENTAL, 3, null);

            // then
            then(questionPoolService).should()
                    .isPoolSufficient(eq(cacheKey), eq(3), eq(allCategories), anySet());
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL이 아닌 타입은 카테고리 필터로 빈 리스트를 사용한다")
        void nonCsType_useEmptyCategoryFilter() {
            // given
            String cacheKey = "JUNIOR:BEHAVIORAL";
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "인성질문")
            );

            given(questionPoolService.isPoolSufficient(eq(cacheKey), eq(3), eq(List.of()), anySet()))
                    .willReturn(true);
            given(questionPoolService.selectFromPool(eq(cacheKey), eq(3), eq(List.of()), anySet()))
                    .willReturn(poolQuestions);

            // when
            cacheableQuestionProvider.provide(
                    null, POSITION, LEVEL, TECH_STACK, InterviewType.BEHAVIORAL, 3, null);

            // then
            then(questionPoolService).should()
                    .isPoolSufficient(eq(cacheKey), eq(3), eq(List.of()), anySet());
        }
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private GeneratedQuestion makeGeneratedQuestion(String content) {
        GeneratedQuestion gq = new GeneratedQuestion();
        ReflectionTestUtils.setField(gq, "content", content);
        ReflectionTestUtils.setField(gq, "category", "운영체제");
        return gq;
    }
}
