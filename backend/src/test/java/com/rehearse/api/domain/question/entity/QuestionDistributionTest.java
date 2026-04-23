package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.question.entity.QuestionDistribution;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionDistributionTest {

    @Nested
    @DisplayName("create 메서드 — 분배 수학 로직")
    class Create {

        @Test
        @DisplayName("2개 타입에 6문제를 균등 분배하면 각 3개씩 할당된다")
        void create_evenDistribution_twoTypes() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL);

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 6);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsEntry(InterviewType.CS_FUNDAMENTAL, 3)
                    .containsEntry(InterviewType.BEHAVIORAL, 3);
        }

        @Test
        @DisplayName("2개 타입에 7문제를 분배하면 나머지 1개가 첫 번째 타입에 할당된다 (4, 3)")
        void create_unevenDistribution_remainderGoesToFirst() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL);

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 7);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsEntry(InterviewType.CS_FUNDAMENTAL, 4)
                    .containsEntry(InterviewType.BEHAVIORAL, 3);
        }

        @Test
        @DisplayName("단일 타입에 전체 문제가 할당된다")
        void create_singleType_allQuestionsAssigned() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL);

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 5);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsEntry(InterviewType.CS_FUNDAMENTAL, 5);
            assertThat(cacheable).hasSize(1);
        }

        @Test
        @DisplayName("3개 타입에 10문제를 분배하면 나머지 1개가 첫 번째 타입에 할당된다 (4, 3, 3)")
        void create_threeTypes_remainderDistributedCorrectly() {
            // given
            List<InterviewType> types = List.of(
                    InterviewType.CS_FUNDAMENTAL,
                    InterviewType.BEHAVIORAL,
                    InterviewType.LANGUAGE_FRAMEWORK
            );

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 10);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsEntry(InterviewType.CS_FUNDAMENTAL, 4)
                    .containsEntry(InterviewType.BEHAVIORAL, 3)
                    .containsEntry(InterviewType.LANGUAGE_FRAMEWORK, 3);
        }

        @Test
        @DisplayName("3개 타입에 11문제를 분배하면 나머지 2개가 첫 두 타입에 할당된다 (4, 4, 3)")
        void create_threeTypes_twoRemainders() {
            // given
            List<InterviewType> types = List.of(
                    InterviewType.CS_FUNDAMENTAL,
                    InterviewType.BEHAVIORAL,
                    InterviewType.LANGUAGE_FRAMEWORK
            );

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 11);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsEntry(InterviewType.CS_FUNDAMENTAL, 4)
                    .containsEntry(InterviewType.BEHAVIORAL, 4)
                    .containsEntry(InterviewType.LANGUAGE_FRAMEWORK, 3);
        }

        @Test
        @DisplayName("전체 문제 수의 합이 요청한 totalCount와 일치한다")
        void create_sumEqualsTotal() {
            // given
            List<InterviewType> types = List.of(
                    InterviewType.CS_FUNDAMENTAL,
                    InterviewType.BEHAVIORAL,
                    InterviewType.LANGUAGE_FRAMEWORK
            );
            int totalCount = 13;

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, totalCount);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            int sum = cacheable.values().stream().mapToInt(Integer::intValue).sum();
            assertThat(sum).isEqualTo(totalCount);
        }
    }

    @Nested
    @DisplayName("getCacheableTypes / getFreshTypes — 필터링")
    class CacheableAndFresh {

        @Test
        @DisplayName("getCacheableTypes는 isCacheable()이 true인 타입만 반환한다")
        void getCacheableTypes_returnsCacheableOnly() {
            // given
            List<InterviewType> types = List.of(
                    InterviewType.CS_FUNDAMENTAL,   // CACHEABLE
                    InterviewType.RESUME_BASED       // FRESH
            );

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 6);
            Map<InterviewType, Integer> cacheable = distribution.getCacheableTypes();

            // then
            assertThat(cacheable).containsKey(InterviewType.CS_FUNDAMENTAL);
            assertThat(cacheable).doesNotContainKey(InterviewType.RESUME_BASED);
        }

        @Test
        @DisplayName("getFreshTypes는 isCacheable()이 false인 타입만 반환한다")
        void getFreshTypes_returnsFreshOnly() {
            // given
            List<InterviewType> types = List.of(
                    InterviewType.CS_FUNDAMENTAL,   // CACHEABLE
                    InterviewType.RESUME_BASED       // FRESH
            );

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 6);
            Map<InterviewType, Integer> fresh = distribution.getFreshTypes();

            // then
            assertThat(fresh).containsKey(InterviewType.RESUME_BASED);
            assertThat(fresh).doesNotContainKey(InterviewType.CS_FUNDAMENTAL);
        }

        @Test
        @DisplayName("모든 타입이 CACHEABLE이면 getFreshTypes는 빈 맵을 반환한다")
        void getFreshTypes_allCacheable_returnsEmpty() {
            // given
            List<InterviewType> types = List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL);

            // when
            QuestionDistribution distribution = QuestionDistribution.create(types, 6);
            Map<InterviewType, Integer> fresh = distribution.getFreshTypes();

            // then
            assertThat(fresh).isEmpty();
        }
    }
}
