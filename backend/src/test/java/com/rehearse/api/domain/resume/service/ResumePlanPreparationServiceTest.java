package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.dto.ReplanResponse;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumePlannerErrorCode;
import com.rehearse.api.domain.resume.service.ResumeReplanLoader.ReplanContext;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
    private ResumeReplanLoader replanLoader;

    @Nested
    @DisplayName("정상 흐름")
    class HappyPath {

        @Test
        @DisplayName("기존 plan 존재 시 새 plan 으로 교체하고 replaced=true 반환")
        void replan_replacesExistingPlan_andReturnsReplacedTrue() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            InterviewPlan existingPlan = createInterviewPlan("plan-old");
            ReflectionTestUtils.setField(existingPlan, "id", 100L);
            InterviewPlan freshPlan = createInterviewPlan("plan-new");
            InterviewPlan savedPlan = createInterviewPlan("plan-new");
            ReflectionTestUtils.setField(savedPlan, "id", 200L);

            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, 30));
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
            ResumeSkeleton skeleton = createSkeleton();
            InterviewPlan freshPlan = createInterviewPlan("plan-fresh");
            InterviewPlan savedPlan = createInterviewPlan("plan-fresh");
            ReflectionTestUtils.setField(savedPlan, "id", 300L);

            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, 30));
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

        @Test
        @DisplayName("durationMinutes null 인 경우 default 30 적용")
        void replan_appliesDefaultDuration_whenInterviewDurationNull() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            InterviewPlan freshPlan = createInterviewPlan("plan-default");
            InterviewPlan savedPlan = createInterviewPlan("plan-default");
            ReflectionTestUtils.setField(savedPlan, "id", 400L);

            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, null));
            given(planStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.empty());
            given(resumeInterviewPlanner.plan(eq(skeleton), eq(30))).willReturn(freshPlan);
            given(planStore.replace(INTERVIEW_ID, freshPlan)).willReturn(savedPlan);

            // when
            ReplanResponse response = service.replan(INTERVIEW_ID, OWNER_USER_ID);

            // then
            assertThat(response.planId()).isEqualTo(400L);
            then(resumeInterviewPlanner).should().plan(skeleton, 30);
        }
    }

    @Nested
    @DisplayName("거부 / 실패 케이스")
    class Rejection {

        @Test
        @DisplayName("loader 가 BusinessException 던지면 그대로 전파 (도메인 룰 통과)")
        void replan_propagatesLoaderBusinessException() {
            // given
            BusinessException loaderError = new BusinessException(
                    com.rehearse.api.domain.interview.exception.InterviewErrorCode.INTERVIEW_NOT_RESUME_BASED);
            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID)).willThrow(loaderError);

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isSameAs(loaderError);

            then(resumeInterviewPlanner).should(never()).plan(any(), anyInt());
            then(planStore).should(never()).replace(anyLong(), any());
        }

        @Test
        @DisplayName("planner 가 LLM 인프라 BusinessException(AI_*) 던지면 PLAN_GENERATION_FAILED (503) 매핑")
        void replan_mapsAiInfraFailure_toPlanGenerationFailed() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, 30));
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt()))
                    .willThrow(new BusinessException(AiErrorCode.SERVICE_UNAVAILABLE));

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(ResumePlannerErrorCode.PLAN_GENERATION_FAILED.getCode());
                        assertThat(be.getStatus().value()).isEqualTo(503);
                    });

            then(planStore).should(never()).replace(anyLong(), any());
        }

        @Test
        @DisplayName("planner 가 RuntimeException 던지면 PLAN_GENERATION_FAILED (503) 매핑")
        void replan_mapsRuntimeException_toPlanGenerationFailed() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, 30));
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt()))
                    .willThrow(new RuntimeException("LLM timeout"));

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(ResumePlannerErrorCode.PLAN_GENERATION_FAILED.getCode());
                    });

            then(planStore).should(never()).replace(anyLong(), any());
        }

        @Test
        @DisplayName("planner 가 도메인 BusinessException(non-AI) 던지면 그대로 전파")
        void replan_propagatesDomainBusinessException() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            BusinessException domainError = new BusinessException(ResumePlannerErrorCode.INVALID_PLAN);
            given(replanLoader.load(INTERVIEW_ID, OWNER_USER_ID))
                    .willReturn(new ReplanContext(skeleton, 30));
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt())).willThrow(domainError);

            // when / then
            assertThatThrownBy(() -> service.replan(INTERVIEW_ID, OWNER_USER_ID))
                    .isSameAs(domainError);
        }
    }

    @Nested
    @DisplayName("prepare - 정상 / 실패 매핑")
    class PrepareFlow {

        @Test
        @DisplayName("planner 가 LLM 인프라 BusinessException 던지면 PLAN_GENERATION_FAILED 매핑")
        void prepare_mapsAiInfraFailure_toPlanGenerationFailed() {
            // given
            ResumeSkeleton skeleton = createSkeleton();
            given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.of(skeleton));
            given(planStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.empty());
            given(resumeInterviewPlanner.plan(eq(skeleton), anyInt()))
                    .willThrow(new BusinessException(AiErrorCode.TIMEOUT));

            // when / then
            assertThatThrownBy(() -> service.prepare(INTERVIEW_ID, "hash-1", "ignored", 30))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(e -> {
                        BusinessException be = (BusinessException) e;
                        assertThat(be.getCode())
                                .isEqualTo(ResumePlannerErrorCode.PLAN_GENERATION_FAILED.getCode());
                    });

            then(planStore).should(never()).save(anyLong(), any());
        }
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
