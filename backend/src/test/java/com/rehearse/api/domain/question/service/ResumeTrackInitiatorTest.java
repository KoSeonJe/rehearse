package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.service.InterviewRuntimeStateCache;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.PreparedResume;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.ResumePlanPreparationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeTrackInitiator")
class ResumeTrackInitiatorTest {

    @InjectMocks
    private ResumeTrackInitiator initiator;

    @Mock
    private QuestionGenerationTransactionHandler transactionHandler;
    @Mock
    private ResumePlanPreparationService resumePlanPreparationService;
    @Mock
    private ResumeInterviewOrchestrator resumeInterviewOrchestrator;
    @Mock
    private InterviewRuntimeStateCache runtimeStateStore;

    private ResumeSkeleton skeleton() {
        return new ResumeSkeleton("r1", "h1", null, "backend", List.of(), Map.of());
    }

    private InterviewPlan plan() {
        ChainReference chain = new ChainReference("proj1::redis", "Redis", 1, List.of(1, 2, 3));
        PlaygroundPhase playground = new PlaygroundPhase("소개해주세요", List.of());
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        ProjectPlan project = new ProjectPlan("proj1", "Redis", 1, playground, interrogation);
        return new InterviewPlan("plan-1", List.of(project));
    }

    @Test
    @DisplayName("prepare → getOrInit → startSession → saveResults(emptyList) 순서로 호출된다")
    void initiate_callsCollaboratorsInCorrectOrder() {
        ResumeSkeleton skeleton = skeleton();
        InterviewPlan plan = plan();
        given(resumePlanPreparationService.prepare(1L, "hash-1", "이력서 본문", 30))
                .willReturn(new PreparedResume(skeleton, plan));
        given(resumeInterviewOrchestrator.startSession(eq(1L), eq(30), any(), any()))
                .willReturn(FollowUpResponse.builder().question("opener").presentToUser(true).build());

        initiator.initiate(1L, InterviewLevel.JUNIOR, "hash-1", "이력서 본문", 30);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                resumePlanPreparationService, runtimeStateStore, resumeInterviewOrchestrator, transactionHandler);
        inOrder.verify(resumePlanPreparationService).prepare(1L, "hash-1", "이력서 본문", 30);
        inOrder.verify(runtimeStateStore).getOrInit(eq(1L), any());
        inOrder.verify(resumeInterviewOrchestrator).startSession(eq(1L), eq(30), eq(skeleton), eq(plan));
        inOrder.verify(transactionHandler).saveResults(eq(1L), eq(List.of()));
    }

    @Test
    @DisplayName("startSession 이전에 runtimeStateStore 가 skeleton+plan 으로 시드된다")
    void initiate_seedsRuntimeStateBeforeStartSession() {
        ResumeSkeleton skeleton = skeleton();
        InterviewPlan plan = plan();
        given(resumePlanPreparationService.prepare(7L, "hash-7", "이력서", 25))
                .willReturn(new PreparedResume(skeleton, plan));

        initiator.initiate(7L, InterviewLevel.SENIOR, "hash-7", "이력서", 25);

        org.mockito.ArgumentCaptor<java.util.function.Supplier<InterviewRuntimeState>> supplierCaptor =
                org.mockito.ArgumentCaptor.forClass(java.util.function.Supplier.class);
        then(runtimeStateStore).should().getOrInit(eq(7L), supplierCaptor.capture());
        InterviewRuntimeState seeded = supplierCaptor.getValue().get();
        org.assertj.core.api.Assertions.assertThat(seeded.getCurrentLevel()).isEqualTo("SENIOR");
        org.assertj.core.api.Assertions.assertThat(seeded.getResumeSkeletonCache()).isSameAs(skeleton);
        org.assertj.core.api.Assertions.assertThat(seeded.getInterviewPlanCache()).isSameAs(plan);
    }

    @Test
    @DisplayName("startSession 예외는 그대로 전파된다")
    void initiate_startSessionThrows_propagates() {
        given(resumePlanPreparationService.prepare(1L, "hash-1", "이력서", 30))
                .willReturn(new PreparedResume(skeleton(), plan()));
        given(resumeInterviewOrchestrator.startSession(any(), anyInt(), any(), any()))
                .willThrow(new RuntimeException("AI 호출 실패"));

        assertThatThrownBy(() -> initiator.initiate(1L, InterviewLevel.JUNIOR, "hash-1", "이력서", 30))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI 호출 실패");
    }
}
