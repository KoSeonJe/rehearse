package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.question.dto.GetQuestionPoolCommand;
import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.repository.QuestionRepository;
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

import java.util.List;
import java.util.Optional;
import java.util.Set;
import java.util.concurrent.locks.ReentrantLock;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class StandardQuestionProviderTest {

    @InjectMocks
    private StandardQuestionProvider standardQuestionProvider;

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

    private static GetQuestionPoolCommand command(Long userId, List<String> csSubTopics) {
        return new GetQuestionPoolCommand(userId, POSITION, LEVEL, TECH_STACK, csSubTopics);
    }

    @Nested
    @DisplayName("캐시 히트 시나리오")
    class CacheHit {

        @Test
        @DisplayName("Pool이 충분하면 AI를 호출하지 않고 Pool에서 질문을 반환한다")
        void poolSufficient_returnsFromPool_withoutAiCall() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            Long userId = 1L;
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "질문1"),
                    TestFixtures.createQuestionPool(cacheKey, "질문2"),
                    TestFixtures.createQuestionPool(cacheKey, "질문3")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.of(poolQuestions));

            // when
            List<QuestionPool> result = standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, null));

            // then
            assertThat(result).hasSize(3);
            then(aiClient).should(never()).generateQuestions(any());
        }

        @Test
        @DisplayName("userId가 존재하면 기사용 질문 풀 ID를 조회하여 제외 처리한다")
        void userIdPresent_queriesUsedPoolIds() {
            // given
            Long userId = 1L;
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            Set<Long> usedIds = Set.of(10L, 20L);

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(usedIds);
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.of(List.of(TestFixtures.createQuestionPool(cacheKey, "질문1"))));

            // when
            standardQuestionProvider.provide(InterviewType.CS_FUNDAMENTAL, 3, command(userId, null));

            // then
            then(questionRepository).should()
                    .findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any());
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
            Long userId = 1L;
            ReentrantLock lock = new ReentrantLock();
            List<GeneratedQuestion> generated = List.of(makeGeneratedQuestion("AI질문1"));
            List<QuestionPool> convertedPool = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "AI질문1")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            // 첫 번째 호출(lock 전): empty, 두 번째 호출(lock 후 재확인): empty
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.empty(), Optional.empty());
            given(questionGenerationLock.acquire(any())).willReturn(lock);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willReturn(generated);
            given(questionPoolService.saveQuestionPools(any(), eq(generated)))
                    .willReturn(convertedPool);
            given(questionPoolService.selectWithCategoryDistribution(eq(convertedPool), eq(3)))
                    .willReturn(convertedPool);

            // when
            List<QuestionPool> result = standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, null));

            // then
            assertThat(result).isNotEmpty();
            then(aiClient).should().generateQuestions(any(QuestionGenerationRequest.class));
            then(questionPoolService).should().saveQuestionPools(any(), eq(generated));
        }

        @Test
        @DisplayName("Lock 해제는 AI 호출 성공 여부와 무관하게 finally에서 반드시 호출된다")
        void lockRelease_calledInFinally_onSuccess() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            Long userId = 1L;
            ReentrantLock lock = new ReentrantLock();
            List<GeneratedQuestion> generated = List.of(makeGeneratedQuestion("AI질문1"));
            List<QuestionPool> convertedPool = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "AI질문1")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.empty(), Optional.empty());
            given(questionGenerationLock.acquire(any())).willReturn(lock);
            given(aiClient.generateQuestions(any())).willReturn(generated);
            given(questionPoolService.saveQuestionPools(any(), any())).willReturn(convertedPool);
            given(questionPoolService.selectWithCategoryDistribution(any(), anyInt()))
                    .willReturn(convertedPool);

            // when
            standardQuestionProvider.provide(InterviewType.CS_FUNDAMENTAL, 1, command(userId, null));

            // then
            then(questionGenerationLock).should().release(lock);
        }

        @Test
        @DisplayName("AI 호출 실패 시 예외가 전파되고 Lock이 반드시 해제된다")
        void aiCallFails_exceptionPropagated_lockReleased() {
            // given
            Long userId = 1L;
            ReentrantLock lock = new ReentrantLock();

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.empty(), Optional.empty());
            given(questionGenerationLock.acquire(any())).willReturn(lock);
            given(aiClient.generateQuestions(any(QuestionGenerationRequest.class)))
                    .willThrow(new RuntimeException("AI 서비스 오류"));

            // when & then
            assertThatThrownBy(() -> standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, null)))
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
            Long userId = 1L;
            ReentrantLock lock = new ReentrantLock();
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "질문1")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            // 첫 번째 selectIfSufficient(lock 획득 전): empty
            // 두 번째 selectIfSufficient(lock 획득 후 재확인): present
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.empty())
                    .willReturn(Optional.of(poolQuestions));
            given(questionGenerationLock.acquire(any())).willReturn(lock);

            // when
            List<QuestionPool> result = standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, null));

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
            Long userId = 1L;
            List<String> csSubTopics = List.of("OS", "NETWORK");
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "OS질문", null, "운영체제")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.of(poolQuestions));

            // when
            List<QuestionPool> result = standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, csSubTopics));

            // then
            assertThat(result).hasSize(1);
            then(questionPoolService).should()
                    .selectIfSufficient(any(PoolSelectionCriteria.class));
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL 타입에 csSubTopics가 null이면 전체 CS 카테고리를 필터로 사용한다")
        void csType_nullSubTopics_usesAllCsCategories() {
            // given
            String cacheKey = "JUNIOR:CS_FUNDAMENTAL";
            Long userId = 1L;
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "CS질문")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.of(poolQuestions));

            // when
            standardQuestionProvider.provide(
                    InterviewType.CS_FUNDAMENTAL, 3, command(userId, null));

            // then
            then(questionPoolService).should()
                    .selectIfSufficient(any(PoolSelectionCriteria.class));
        }

        @Test
        @DisplayName("CS_FUNDAMENTAL이 아닌 타입은 카테고리 필터로 빈 리스트를 사용한다")
        void nonCsType_useEmptyCategoryFilter() {
            // given
            String cacheKey = "JUNIOR:BEHAVIORAL";
            Long userId = 1L;
            List<QuestionPool> poolQuestions = List.of(
                    TestFixtures.createQuestionPool(cacheKey, "인성질문")
            );

            given(questionRepository.findUsedQuestionPoolIdsByUserIdAndCacheKey(eq(userId), any()))
                    .willReturn(Set.of());
            given(questionPoolService.selectIfSufficient(any(PoolSelectionCriteria.class)))
                    .willReturn(Optional.of(poolQuestions));

            // when
            standardQuestionProvider.provide(
                    InterviewType.BEHAVIORAL, 3, command(userId, null));

            // then
            then(questionPoolService).should()
                    .selectIfSufficient(any(PoolSelectionCriteria.class));
        }
    }

    // ──────────────────────────────────────────────
    // Helper
    // ──────────────────────────────────────────────

    private GeneratedQuestion makeGeneratedQuestion(String content) {
        return new GeneratedQuestion(
                content, "tts", "운영체제", 1, "criteria",
                "CS_FUNDAMENTAL", "model", "strategy");
    }
}
