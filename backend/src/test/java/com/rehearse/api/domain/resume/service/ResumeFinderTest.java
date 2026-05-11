package com.rehearse.api.domain.resume.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.entity.ResumeSkeletonEntity;
import com.rehearse.api.domain.resume.repository.InterviewPlanRepository;
import com.rehearse.api.domain.resume.repository.ResumeSkeletonRepository;
import java.util.List;
import java.util.Map;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeFinder - resume 도메인 cross-domain read 진입점")
class ResumeFinderTest {

    @InjectMocks
    private ResumeFinder resumeFinder;

    @Mock
    private ResumeSkeletonRepository resumeSkeletonRepository;

    @Mock
    private ResumeSkeletonCodec resumeSkeletonCodec;

    @Mock
    private InterviewPlanRepository interviewPlanRepository;

    @Mock
    private InterviewPlanRuntimeCache interviewPlanRuntimeCache;

    @Nested
    @DisplayName("findSkeletonByInterviewId")
    class FindSkeleton {

        @Test
        @DisplayName("Repository 가 entity 반환 시 Codec 으로 역직렬화 후 반환한다")
        void returns_decoded_skeleton_when_repository_has_entity() {
            ResumeSkeletonEntity entity = ResumeSkeletonEntity.builder()
                    .interviewId(1L).fileHash("h").candidateLevel("JUNIOR")
                    .targetDomain("BACKEND").skeletonJson("{}").build();
            ResumeSkeleton skeleton = createFixtureSkeleton();
            given(resumeSkeletonRepository.findByInterviewId(1L)).willReturn(Optional.of(entity));
            given(resumeSkeletonCodec.deserialize(entity)).willReturn(skeleton);

            Optional<ResumeSkeleton> result = resumeFinder.findSkeletonByInterviewId(1L);

            assertThat(result).contains(skeleton);
        }

        @Test
        @DisplayName("Repository 가 부재 반환 시 Optional.empty 를 반환하고 Codec 은 호출되지 않는다")
        void returns_empty_when_repository_has_none() {
            given(resumeSkeletonRepository.findByInterviewId(99L)).willReturn(Optional.empty());

            Optional<ResumeSkeleton> result = resumeFinder.findSkeletonByInterviewId(99L);

            assertThat(result).isEmpty();
            then(resumeSkeletonCodec).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("findInterviewPlan")
    class FindPlan {

        @Test
        @DisplayName("runtime cache hit 시 cache 값을 반환하고 Repository 는 호출하지 않는다")
        void returns_cached_plan_and_skips_repository() {
            InterviewPlan plan = createFixturePlan();
            given(interviewPlanRuntimeCache.read(1L)).willReturn(plan);

            Optional<InterviewPlan> result = resumeFinder.findInterviewPlan(1L);

            assertThat(result).contains(plan);
            then(interviewPlanRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("runtime cache miss 시 Repository DB 조회로 fallback 한다")
        void falls_back_to_repository_when_cache_miss() {
            InterviewPlan plan = createFixturePlan();
            given(interviewPlanRuntimeCache.read(1L)).willReturn(null);
            given(interviewPlanRepository.findByInterviewId(1L)).willReturn(Optional.of(plan));

            Optional<InterviewPlan> result = resumeFinder.findInterviewPlan(1L);

            assertThat(result).contains(plan);
        }

        @Test
        @DisplayName("runtime cache miss + Repository 도 부재 시 Optional.empty 를 반환한다")
        void returns_empty_when_both_absent() {
            given(interviewPlanRuntimeCache.read(99L)).willReturn(null);
            given(interviewPlanRepository.findByInterviewId(99L)).willReturn(Optional.empty());

            Optional<InterviewPlan> result = resumeFinder.findInterviewPlan(99L);

            assertThat(result).isEmpty();
        }
    }

    private ResumeSkeleton createFixtureSkeleton() {
        return new ResumeSkeleton(
                "resume_1",
                "hash_abc",
                CandidateLevel.JUNIOR,
                "BACKEND",
                List.of(),
                Map.of()
        );
    }

    private InterviewPlan createFixturePlan() {
        ChainReference chain = new ChainReference("p1::Redis", "Redis", 1, List.of(1, 2, 3, 4));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("프로젝트를 소개해주세요.", List.of("p1_c1"));
        ProjectPlan projectPlan = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);
        return new InterviewPlan("plan_test", List.of(projectPlan));
    }
}
