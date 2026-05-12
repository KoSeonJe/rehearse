package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.question.entity.QuestionPool;
import com.rehearse.api.domain.question.repository.QuestionPoolRepository;
import com.rehearse.api.global.support.TestFixtures;
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
    @DisplayName("selectIfSufficient")
    class SelectIfSufficient {

        @Test
        @DisplayName("활성 풀 수가 requiredCount × 3 이상이면 선택 결과를 반환한다")
        void sufficient_returnsSelection() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1"),
                    TestFixtures.createQuestionPool("key", "Q2"),
                    TestFixtures.createQuestionPool("key", "Q3"),
                    TestFixtures.createQuestionPool("key", "Q4"),
                    TestFixtures.createQuestionPool("key", "Q5"),
                    TestFixtures.createQuestionPool("key", "Q6"),
                    TestFixtures.createQuestionPool("key", "Q7"),
                    TestFixtures.createQuestionPool("key", "Q8"),
                    TestFixtures.createQuestionPool("key", "Q9")
            );
            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key")).willReturn(candidates);

            // when
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(PoolSelectionCriteria.of("key", 3));

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(3);
        }

        @Test
        @DisplayName("활성 풀 수가 requiredCount × 3 미만이면 빈 Optional을 반환한다")
        void insufficient_returnsEmpty() {
            // given
            List<QuestionPool> candidates = List.of(
                    TestFixtures.createQuestionPool("key", "Q1"),
                    TestFixtures.createQuestionPool("key", "Q2"),
                    TestFixtures.createQuestionPool("key", "Q3"),
                    TestFixtures.createQuestionPool("key", "Q4"),
                    TestFixtures.createQuestionPool("key", "Q5"),
                    TestFixtures.createQuestionPool("key", "Q6"),
                    TestFixtures.createQuestionPool("key", "Q7"),
                    TestFixtures.createQuestionPool("key", "Q8")
            );
            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key")).willReturn(candidates);

            // when
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(PoolSelectionCriteria.of("key", 3));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("categoryFilter가 주어지면 카테고리 필터 fetch 결과로 충분성을 판단한다")
        void withCategoryFilter_usesFilteredFetch() {
            // given
            List<String> filter = List.of("OS", "NETWORK");
            List<QuestionPool> filtered = List.of(
                    TestFixtures.createQuestionPool("key", "Q1", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q2", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q3", null, "NETWORK"),
                    TestFixtures.createQuestionPool("key", "Q4", null, "NETWORK"),
                    TestFixtures.createQuestionPool("key", "Q5", null, "OS"),
                    TestFixtures.createQuestionPool("key", "Q6", null, "NETWORK")
            );
            given(questionPoolRepository.findByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter))
                    .willReturn(filtered);

            // when: requiredCount=2 → threshold=6, candidates=6 → 충분
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(
                    new PoolSelectionCriteria("key", 2, filter, null));

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(2);
            then(questionPoolRepository).should()
                    .findByCacheKeyAndIsActiveTrueAndCategoryIn("key", filter);
        }

        @Test
        @DisplayName("usedPoolIds를 제외한 후보 수가 requiredCount × 2 이상이면 선택 결과를 반환한다")
        void withUsedPoolIds_sufficient_returnsSelection() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            QuestionPool q3 = TestFixtures.createQuestionPool("key", "Q3");
            setId(q1, 1L);
            setId(q2, 2L);
            setId(q3, 3L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2, q3));

            // when: requiredCount=1, usedPoolIds={1} → 남은 2개 >= ceil(1*2.0)=2 → 충분
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(
                    new PoolSelectionCriteria("key", 1, null, Set.of(1L)));

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).hasSize(1);
            assertThat(result.get()).doesNotContain(q1);
        }

        @Test
        @DisplayName("usedPoolIds를 제외한 후보 수가 requiredCount × 2 미만이면 빈 Optional을 반환한다")
        void withUsedPoolIds_insufficient_returnsEmpty() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            setId(q1, 1L);
            setId(q2, 2L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2));

            // when: requiredCount=2, usedPoolIds={1} → 남은 1개 < ceil(2*2.0)=4 → 부족
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(
                    new PoolSelectionCriteria("key", 2, null, Set.of(1L)));

            // then
            assertThat(result).isEmpty();
        }

        @Test
        @DisplayName("usedPoolIds에 포함된 항목은 선택 결과에서 제외된다")
        void withUsedPoolIds_excludesUsedFromResult() {
            // given
            QuestionPool q1 = TestFixtures.createQuestionPool("key", "Q1");
            QuestionPool q2 = TestFixtures.createQuestionPool("key", "Q2");
            QuestionPool q3 = TestFixtures.createQuestionPool("key", "Q3");
            QuestionPool q4 = TestFixtures.createQuestionPool("key", "Q4");
            setId(q1, 1L);
            setId(q2, 2L);
            setId(q3, 3L);
            setId(q4, 4L);

            given(questionPoolRepository.findByCacheKeyAndIsActiveTrue("key"))
                    .willReturn(List.of(q1, q2, q3, q4));

            // when: requiredCount=2, usedPoolIds={1} → 남은 3개 >= ceil(2*2.0)=4 ? 3<4 → 부족
            // requiredCount=1 로 가정해서 충분 케이스 검증
            Optional<List<QuestionPool>> result = questionPoolService.selectIfSufficient(
                    new PoolSelectionCriteria("key", 1, null, Set.of(1L)));

            // then
            assertThat(result).isPresent();
            assertThat(result.get()).doesNotContain(q1);
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
