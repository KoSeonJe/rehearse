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
    private ResumeSkeletonPersister resumeSkeletonPersister;

    @Mock
    private InterviewPlanPersister interviewPlanPersister;

    @Mock
    private InterviewPlanRuntimeCache interviewPlanRuntimeCache;

    @Nested
    @DisplayName("findSkeletonByInterviewId")
    class FindSkeleton {

        @Test
        @DisplayName("Persister 가 entity 반환 시 그대로 위임 반환한다")
        void returns_present_when_persister_has_skeleton() {
            ResumeSkeleton skeleton = createFixtureSkeleton();
            given(resumeSkeletonPersister.findByInterviewId(1L)).willReturn(Optional.of(skeleton));

            Optional<ResumeSkeleton> result = resumeFinder.findSkeletonByInterviewId(1L);

            assertThat(result).contains(skeleton);
        }

        @Test
        @DisplayName("Persister 가 부재 반환 시 Optional.empty 를 그대로 위임 반환한다")
        void returns_empty_when_persister_has_none() {
            given(resumeSkeletonPersister.findByInterviewId(99L)).willReturn(Optional.empty());

            Optional<ResumeSkeleton> result = resumeFinder.findSkeletonByInterviewId(99L);

            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("findInterviewPlan")
    class FindPlan {

        @Test
        @DisplayName("runtime cache hit 시 cache 값을 반환하고 Persister 는 호출하지 않는다")
        void returns_cached_plan_and_skips_persister() {
            InterviewPlan plan = createFixturePlan();
            given(interviewPlanRuntimeCache.read(1L)).willReturn(plan);

            Optional<InterviewPlan> result = resumeFinder.findInterviewPlan(1L);

            assertThat(result).contains(plan);
            then(interviewPlanPersister).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("runtime cache miss 시 Persister DB 조회로 fallback 한다")
        void falls_back_to_persister_when_cache_miss() {
            InterviewPlan plan = createFixturePlan();
            given(interviewPlanRuntimeCache.read(1L)).willReturn(null);
            given(interviewPlanPersister.findByInterviewId(1L)).willReturn(Optional.of(plan));

            Optional<InterviewPlan> result = resumeFinder.findInterviewPlan(1L);

            assertThat(result).contains(plan);
        }

        @Test
        @DisplayName("runtime cache miss + Persister 도 부재 시 Optional.empty 를 반환한다")
        void returns_empty_when_both_absent() {
            given(interviewPlanRuntimeCache.read(99L)).willReturn(null);
            given(interviewPlanPersister.findByInterviewId(99L)).willReturn(Optional.empty());

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
