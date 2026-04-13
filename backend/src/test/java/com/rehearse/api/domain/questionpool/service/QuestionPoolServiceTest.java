package com.rehearse.api.domain.questionpool.service;

import com.rehearse.api.domain.questionpool.entity.QuestionPool;
import com.rehearse.api.domain.questionpool.repository.QuestionPoolRepository;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class QuestionPoolServiceTest {

    @InjectMocks
    private QuestionPoolService questionPoolService;

    @Mock
    private QuestionPoolRepository questionPoolRepository;

    @Nested
    @DisplayName("isPoolSufficient")
    class IsPoolSufficient {

        @Test
        @DisplayName("활성 풀 수가 requiredCount × 3 이상이면 true를 반환한다")
        void sufficient_returnsTrue() {
            // given
            given(questionPoolRepository.countByCacheKeyAndIsActiveTrue("key")).willReturn(9L);

            // when
            boolean result = questionPoolService.isPoolSufficient("key", 3);

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("활성 풀 수가 requiredCount × 3 미만이면 false를 반환한다")
        void insufficient_returnsFalse() {
            // given
            given(questionPoolRepository.countByCacheKeyAndIsActiveTrue("key")).willReturn(8L);

            // when
            boolean result = questionPoolService.isPoolSufficient("key", 3);

            // then
            assertThat(result).isFalse();
        }

        @Test
        @DisplayName("categoryFilter가 주어지면 카테고리 필터 쿼리에 위임한다")
        void withCategoryFilter_delegatesToFilteredCount() {
            // given
            List<String> filter = List.of("OS", "NETWORK");
            given(questionPoolRepository.countByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter))
                    .willReturn(6L);

            // when
            boolean result = questionPoolService.isPoolSufficient("key", 2, filter);

            // then
            assertThat(result).isTrue();
            then(questionPoolRepository).should()
                    .countByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter);
        }

        @Test
        @DisplayName("usedPoolIds를 제외한 후보 수가 requiredCount × 2 이상이면 true를 반환한다")
        void withUsedPoolIds_excludesUsedAndChecks() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            QuestionPool q3 = TestFixtures.createQuestionPool("key", "Q3");
            setId(q1, 1L);
            setId(q2, 2L);
            setId(q3, 3L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2, q3));

            // when: requiredCount=1, usedPoolIds={1} → 남은 2개 >= ceil(1*2.0)=2 → true
            boolean result = questionPoolService.isPoolSufficient("key", 1, null, Set.of(1L));

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("usedPoolIds를 제외한 후보 수가 requiredCount × 2 미만이면 false를 반환한다")
        void withUsedPoolIds_insufficient_returnsFalse() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            setId(q1, 1L);
            setId(q2, 2L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2));

            // when: requiredCount=2, usedPoolIds={1} → 남은 1개 < ceil(2*2.0)=4 → false
            boolean result = questionPoolService.isPoolSufficient("key", 2, null, Set.of(1L));

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("shouldSaveToPool")
    class ShouldSaveToPool {

        @Test
        @DisplayName("활성 풀 수가 200 미만이면 저장해야 한다고 판단한다")
        void belowCap_returnsTrue() {
            // given
            given(questionPoolRepository.countByCacheKeyAndIsActiveTrue("key")).willReturn(199L);

            // when
            boolean result = questionPoolService.shouldSaveToPool("key");

            // then
            assertThat(result).isTrue();
        }

        @Test
        @DisplayName("활성 풀 수가 soft cap(200)에 도달하면 저장하지 않는다")
        void atCap_returnsFalse() {
            // given
            given(questionPoolRepository.countByCacheKeyAndIsActiveTrue("key")).willReturn(200L);

            // when
            boolean result = questionPoolService.shouldSaveToPool("key");

            // then
            assertThat(result).isFalse();
        }
    }

    @Nested
    @DisplayName("selectFromPool")
    class SelectFromPool {

        @Test
        @DisplayName("기본 선택: cacheKey에 해당하는 활성 풀에서 requiredCount만큼 선택한다")
        void basicSelect_returnsRequiredCount() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1"),
                    TestFixtures.createQuestionPool("key", "Q2"),
                    TestFixtures.createQuestionPool("key", "Q3"),
                    TestFixtures.createQuestionPool("key", "Q4")
            );
            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key")).willReturn(candidates);

            // when
            List<QuestionPool> result = questionPoolService.selectFromPool("key", 2);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("카테고리 필터 선택: 필터에 맞는 후보만 사용하여 선택한다")
        void withCategoryFilter_usesFilteredCandidates() {
            // given
            List<String> filter = List.of("OS");
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q-OS-1", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q-OS-2", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q-OS-3", null, "OS")
            );
            given(questionPoolRepository.findByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter))
                    .willReturn(candidates);

            // when
            List<QuestionPool> result = questionPoolService.selectFromPool("key", 2, filter);

            // then
            assertThat(result).hasSize(2);
            then(questionPoolRepository).should()
                    .findByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter);
        }

        @Test
        @DisplayName("사용된 ID 제외 선택: usedPoolIds에 포함된 항목은 결과에서 제외된다")
        void withUsedPoolIds_excludesUsed() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            QuestionPool q3 = TestFixtures.createQuestionPool("key", "Q3");
            setId(q1, 1L);
            setId(q2, 2L);
            setId(q3, 3L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2, q3));

            // when
            List<QuestionPool> result = questionPoolService.selectFromPool("key", 2, null, Set.of(1L));

            // then
            assertThat(result).hasSize(2);
            assertThat(result).doesNotContain(q1);
        }
    }

    @Nested
    @DisplayName("selectWithCategoryDistribution")
    class SelectWithCategoryDistribution {

        @Test
        @DisplayName("후보 수가 요청 수 이하이면 후보 전체를 반환한다")
        void candidatesLessThanRequired_returnsAll() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q2", null, "NETWORK")
            );

            // when
            List<QuestionPool> result = questionPoolService.selectWithCategoryDistribution(candidates, 5);

            // then
            assertThat(result).hasSize(2);
            assertThat(result).containsExactlyInAnyOrderElementsOf(candidates);
        }

        @Test
        @DisplayName("단일 카테고리 후보에서 requiredCount만큼 균등하게 선택한다")
        void singleCategory_selectsRequiredCount() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q2", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q3", null, "OS")
            );

            // when
            List<QuestionPool> result = questionPoolService.selectWithCategoryDistribution(candidates, 2);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("category가 null인 항목은 'UNKNOWN' 카테고리로 분류하여 선택에 포함한다")
        void nullCategory_groupedAsUnknown() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1"),  // category=null
                    TestFixtures.createQuestionPool("key", "Q2"),
                    TestFixtures.createQuestionPool("key", "Q3")
            );

            // when
            List<QuestionPool> result = questionPoolService.selectWithCategoryDistribution(candidates, 2);

            // then
            assertThat(result).hasSize(2);
        }

        @Test
        @DisplayName("여러 카테고리에서 균등 분배하여 requiredCount만큼 선택한다")
        void multipleCategories_distributesEvenly() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q-OS-1", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q-OS-2", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q-OS-3", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q-NET-1", null, "NETWORK"),
                    TestFixtures.createQuestionPool("key", "Q-NET-2", null, "NETWORK"),
                    TestFixtures.createQuestionPool("key", "Q-NET-3", null, "NETWORK")
            );

            // when
            List<QuestionPool> result = questionPoolService.selectWithCategoryDistribution(candidates, 4);

            // then
            assertThat(result).hasSize(4);
        }

        @Test
        @DisplayName("빈 후보 리스트가 주어지면 빈 결과를 반환한다")
        void emptyCandidates_returnsEmptyList() {
            // given
            List<QuestionPool> candidates = List.of();

            // when
            List<QuestionPool> result = questionPoolService.selectWithCategoryDistribution(candidates, 3);

            // then
            assertThat(result).isEmpty();
        }
    }

    // ──────────────────────────────────────────────
    // Helper: JPA @GeneratedValue 없이 ID 설정
    // ──────────────────────────────────────────────
    private void setId(QuestionPool pool, Long id) {
        org.springframework.test.util.ReflectionTestUtils.setField(pool, "id", id);
    }
}
