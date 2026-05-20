package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeSkeletonSampler 는 동일 이력서로 반복 인터뷰 시 LLM 입력 다양화를 위해 프로젝트별 decisions 를 절반 샘플링한다")
class ResumeSkeletonSamplerTest {

    private final ResumeSkeletonSampler sampler = new ResumeSkeletonSampler();

    @Nested
    @DisplayName("샘플 크기 규칙 — 프로젝트별 decisions 의 절반 (올림), 최소 2 유지")
    class SampleSizeRule {

        @Test
        @DisplayName("decisions 7개면 4개 (ceil(7/2)) 가 샘플링된다")
        void samples_4_when_decisions_count_is_7() {
            ResumeSkeleton skeleton = skeletonWithDecisions(seven());

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);

            assertThat(sampled.projects().get(0).decisions()).hasSize(4);
        }

        @Test
        @DisplayName("decisions 4개면 2개 (ceil(4/2)) 가 샘플링된다")
        void samples_2_when_decisions_count_is_4() {
            ResumeSkeleton skeleton = skeletonWithDecisions(List.of("d1", "d2", "d3", "d4"));

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);

            assertThat(sampled.projects().get(0).decisions()).hasSize(2);
        }

        @Test
        @DisplayName("decisions 2개 이하면 샘플링 없이 그대로 유지된다")
        void keeps_decisions_when_count_is_at_or_below_min() {
            ResumeSkeleton skeleton = skeletonWithDecisions(List.of("d1", "d2"));

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);

            assertThat(sampled.projects().get(0).decisions()).containsExactly("d1", "d2");
        }
    }

    @Nested
    @DisplayName("재현성 / 다양성 — seed 는 interviewId")
    class SeedBehavior {

        @Test
        @DisplayName("같은 seed 면 같은 샘플 결과가 나온다 (재현성)")
        void produces_same_result_for_same_seed() {
            ResumeSkeleton skeleton = skeletonWithDecisions(seven());

            ResumeSkeleton first = sampler.sampleDecisions(skeleton, 42L);
            ResumeSkeleton second = sampler.sampleDecisions(skeleton, 42L);

            assertThat(first.projects().get(0).decisions())
                    .isEqualTo(second.projects().get(0).decisions());
        }

        @Test
        @DisplayName("다른 seed 면 일반적으로 다른 샘플 조합이 나온다 (다양성)")
        void produces_different_result_for_different_seed() {
            ResumeSkeleton skeleton = skeletonWithDecisions(seven());

            ResumeSkeleton first = sampler.sampleDecisions(skeleton, 1L);
            ResumeSkeleton second = sampler.sampleDecisions(skeleton, 2L);

            assertThat(first.projects().get(0).decisions())
                    .isNotEqualTo(second.projects().get(0).decisions());
        }
    }

    @Nested
    @DisplayName("샘플링 외 필드 보존")
    class FieldPreservation {

        @Test
        @DisplayName("projectId / projectName / techStack / role / architecture 는 샘플링 영향 없이 유지된다")
        void preserves_non_decision_fields() {
            Project original = new Project(
                    "p1", "주문 캐싱", List.of("Redis", "MySQL"),
                    "백엔드", "Cache-Aside", seven(), null
            );
            ResumeSkeleton skeleton = new ResumeSkeleton(
                    "r1", "hash1", CandidateLevel.JUNIOR, "backend", List.of(original)
            );

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);
            Project result = sampled.projects().get(0);

            assertThat(result.projectId()).isEqualTo("p1");
            assertThat(result.projectName()).isEqualTo("주문 캐싱");
            assertThat(result.techStack()).containsExactly("Redis", "MySQL");
            assertThat(result.role()).isEqualTo("백엔드");
            assertThat(result.architecture()).isEqualTo("Cache-Aside");
        }

        @Test
        @DisplayName("샘플링된 decisions 는 원본 decisions 의 부분집합이다")
        void sampled_decisions_are_subset_of_original() {
            List<String> originals = seven();
            ResumeSkeleton skeleton = skeletonWithDecisions(originals);

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);

            assertThat(originals).containsAll(sampled.projects().get(0).decisions());
        }

        @Test
        @DisplayName("빈 projects 면 빈 결과가 반환된다")
        void returns_empty_when_projects_empty() {
            ResumeSkeleton skeleton = new ResumeSkeleton(
                    "r1", "hash1", CandidateLevel.JUNIOR, "backend", List.of()
            );

            ResumeSkeleton sampled = sampler.sampleDecisions(skeleton, 1L);

            assertThat(sampled.projects()).isEmpty();
        }
    }

    private ResumeSkeleton skeletonWithDecisions(List<String> decisions) {
        Project project = new Project(
                "p1", "test-project", List.of("Java"), "backend", "monolith", decisions, null
        );
        return new ResumeSkeleton(
                "r1", "hash1", CandidateLevel.JUNIOR, "backend", List.of(project)
        );
    }

    private List<String> seven() {
        return List.of("d1", "d2", "d3", "d4", "d5", "d6", "d7");
    }
}
