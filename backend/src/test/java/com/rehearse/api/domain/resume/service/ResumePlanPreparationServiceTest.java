package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.ReplanResponse;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Map;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumePlanPreparationService.replan - 이력서 면접 plan 재생성")
class ResumePlanPreparationServiceTest {

    private static final Long INTERVIEW_ID = 10L;
    private static final Long OWNER_USER_ID = 1L;

    @InjectMocks
    private ResumePlanPreparationService service;

    @Mock
    private ResumeIngestionService resumeIngestionService;

    @Mock
    private ResumeSkeletonPersister skeletonStore;

    @Mock
    private ResumeInterviewPlanner resumeInterviewPlanner;

    @Mock
    private InterviewPlanPersister planStore;

    @Mock
    private InterviewFinder interviewFinder;

    @Nested
    @DisplayName("정상 흐름")
    class HappyPath {

        @Test
        @DisplayName("기존 plan 존재 시 새 plan 으로 교체하고 replaced=true 반환")
        void replan_replacesExistingPlan_andReturnsReplacedTrue() {
            // given
            Interview interview = createResumeBasedInterview();
            ResumeSkeleton skeleton = createSkeleton();
            InterviewPlan existingPlan = createInterviewPlan("plan-old");
            ReflectionTestUtils.setField(existingPlan, "id", 100L);
            InterviewPlan freshPlan = createInterviewPlan("plan-new");
            InterviewPlan savedPlan = createInterviewPlan("plan-new");
            ReflectionTestUtils.setField(savedPlan, "id", 200L);

            given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);
            given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.of(skeleton));
            given(planStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.of(existingPlan));
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt())).willReturn(freshPlan);
            given(planStore.replace(INTERVIEW_ID, freshPlan)).willReturn(savedPlan);

            // when
            ReplanResponse response = service.replan(INTERVIEW_ID, OWNER_USER_ID);

            // then
            assertThat(response.interviewId()).isEqualTo(INTERVIEW_ID);
            assertThat(response.planId()).isEqualTo(200L);
            assertThat(response.replaced()).isTrue();
            then(planStore).should().replace(INTERVIEW_ID, freshPlan);
        }

        @Test
        @DisplayName("기존 plan 부재 시 새 plan 생성하고 replaced=false 반환")
        void replan_createsNewPlan_whenNoExistingPlan() {
            // given
            Interview interview = createResumeBasedInterview();
            ResumeSkeleton skeleton = createSkeleton();
            InterviewPlan freshPlan = createInterviewPlan("plan-fresh");
            InterviewPlan savedPlan = createInterviewPlan("plan-fresh");
            ReflectionTestUtils.setField(savedPlan, "id", 300L);

            given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);
            given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.of(skeleton));
            given(planStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.empty());
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt())).willReturn(freshPlan);
            given(planStore.replace(INTERVIEW_ID, freshPlan)).willReturn(savedPlan);

            // when
            ReplanResponse response = service.replan(INTERVIEW_ID, OWNER_USER_ID);

            // then
            assertThat(response.interviewId()).isEqualTo(INTERVIEW_ID);
            assertThat(response.planId()).isEqualTo(300L);
            assertThat(response.replaced()).isFalse();
        }
    }

    @Nested
    @DisplayName("거부 케이스")
    class Rejection {

        @Test
        @DisplayName("RESUME_BASED 외 인터뷰 호출 시 INTERVIEW_NOT_RESUME_BASED (400)")
        void replan_rejects_whenInterviewNotResumeBased() {
            // given
            Interview interview = createCsInterview();
            given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(InterviewErrorCode.INTERVIEW_NOT_RESUME_BASED.getCode());
                    });

            then(skeletonStore).should(never()).findByInterviewId(anyLong());
            then(planStore).should(never()).replace(anyLong(), any());
        }

        @Test
        @DisplayName("skeleton 부재 시 RESUME_PLAN_NOT_READY (409)")
        void replan_rejects_whenSkeletonMissing() {
            // given
            Interview interview = createResumeBasedInterview();
            given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);
            given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.empty());

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(ResumeErrorCode.RESUME_PLAN_NOT_READY.getCode());
                    });

            then(planStore).should(never()).replace(anyLong(), any());
            then(resumeInterviewPlanner).should(never()).plan(any(), anyInt());
        }

        @Test
        @DisplayName("타 유저 호출 시 INTERVIEW_NOT_FOUND (404, 정보 누출 방지)")
        void replan_rejects_whenNotOwner() {
            // given
            Interview interview = createResumeBasedInterview();
            given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, 999L))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(InterviewErrorCode.NOT_FOUND.getCode());
                    });

            then(skeletonStore).should(never()).findByInterviewId(anyLong());
        }
    }

    private Interview createResumeBasedInterview() {
        Interview interview = Interview.builder()
                .userId(OWNER_USER_ID)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.RESUME_BASED))
                .durationMinutes(30)
                .techStack(TechStack.JAVA_SPRING)
                .build();
        ReflectionTestUtils.setField(interview, "id", INTERVIEW_ID);
        return interview;
    }

    private Interview createCsInterview() {
        Interview interview = Interview.builder()
                .userId(OWNER_USER_ID)
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .techStack(TechStack.JAVA_SPRING)
                .build();
        ReflectionTestUtils.setField(interview, "id", INTERVIEW_ID);
        return interview;
    }

    private ResumeSkeleton createSkeleton() {
        return new ResumeSkeleton(
                "resume-1",
                "hash-1",
                CandidateLevel.JUNIOR,
                "backend",
                List.of(),
                Map.of()
        );
    }

    private InterviewPlan createInterviewPlan(String sessionPlanId) {
        PlaygroundPhase playground = new PlaygroundPhase(
                "프로젝트에 대해 자유롭게 소개해주세요.",
                List.of()
        );
        ChainReference chain = new ChainReference(
                "proj-1::topic-1",
                "topic-1",
                1,
                List.of(1)
        );
        InterrogationPhase interrogation = new InterrogationPhase(
                List.of(chain),
                List.of()
        );
        ProjectPlan projectPlan = new ProjectPlan(
                "proj-1",
                "프로젝트 1",
                1,
                playground,
                interrogation
        );
        return new InterviewPlan(sessionPlanId, List.of(projectPlan));
    }
}
