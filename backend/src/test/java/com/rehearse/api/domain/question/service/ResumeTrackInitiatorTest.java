package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.resume.entity.ChainReference;
import com.rehearse.api.domain.resume.entity.InterrogationPhase;
import com.rehearse.api.domain.resume.entity.InterviewPlan;
import com.rehearse.api.domain.resume.entity.PlaygroundPhase;
import com.rehearse.api.domain.resume.entity.ProjectPlan;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.service.InterviewPlanPersister;
import com.rehearse.api.domain.resume.service.ResumeInterviewOrchestrator;
import com.rehearse.api.domain.resume.service.ResumePlanPreparationService;
import com.rehearse.api.domain.resume.service.ResumeSkeletonPersister;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;
import java.util.Map;
import java.util.Optional;

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
    private ResumeSkeletonPersister resumeSkeletonPersister;
    @Mock
    private InterviewPlanPersister interviewPlanPersister;
    @Mock
    private ResumeInterviewOrchestrator resumeInterviewOrchestrator;

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
    @DisplayName("prepare → startSession → saveResults(emptyList) 순서로 호출된다")
    void initiate_callsCollaboratorsInCorrectOrder() {
        given(resumeSkeletonPersister.findByInterviewId(1L)).willReturn(Optional.of(skeleton()));
        given(interviewPlanPersister.findByInterviewId(1L)).willReturn(Optional.of(plan()));
        given(resumeInterviewOrchestrator.startSession(eq(1L), eq(30), any(), any()))
                .willReturn(FollowUpResponse.builder().question("opener").presentToUser(true).build());

        initiator.initiate(1L, "hash-1", "이력서 본문", 30);

        org.mockito.InOrder inOrder = org.mockito.Mockito.inOrder(
                resumePlanPreparationService, resumeInterviewOrchestrator, transactionHandler);
        inOrder.verify(resumePlanPreparationService).prepare(1L, "hash-1", "이력서 본문", 30);
        inOrder.verify(resumeInterviewOrchestrator).startSession(eq(1L), eq(30), any(), any());
        inOrder.verify(transactionHandler).saveResults(eq(1L), eq(List.of()));
    }

    @Test
    @DisplayName("durationMinutes 가 null 이면 기본값 30 으로 startSession 이 호출된다")
    void initiate_nullDuration_defaultsToThirty() {
        given(resumeSkeletonPersister.findByInterviewId(1L)).willReturn(Optional.of(skeleton()));
        given(interviewPlanPersister.findByInterviewId(1L)).willReturn(Optional.of(plan()));
        given(resumeInterviewOrchestrator.startSession(eq(1L), eq(30), any(), any()))
                .willReturn(FollowUpResponse.builder().build());

        initiator.initiate(1L, "hash-1", "이력서", null);

        then(resumeInterviewOrchestrator).should().startSession(eq(1L), eq(30), any(), any());
    }

    @Test
    @DisplayName("startSession 예외는 그대로 전파된다")
    void initiate_startSessionThrows_propagates() {
        given(resumeSkeletonPersister.findByInterviewId(1L)).willReturn(Optional.of(skeleton()));
        given(interviewPlanPersister.findByInterviewId(1L)).willReturn(Optional.of(plan()));
        given(resumeInterviewOrchestrator.startSession(any(), anyInt(), any(), any()))
                .willThrow(new RuntimeException("AI 호출 실패"));

        assertThatThrownBy(() -> initiator.initiate(1L, "hash-1", "이력서", 30))
                .isInstanceOf(RuntimeException.class)
                .hasMessageContaining("AI 호출 실패");
    }
}
