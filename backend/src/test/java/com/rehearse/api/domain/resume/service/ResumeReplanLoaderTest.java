package com.rehearse.api.domain.resume.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.domain.resume.service.ResumeReplanLoader.ReplanContext;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
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
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResumeReplanLoader - replan 컨텍스트 로딩 (tx 1: owner 검증 + skeleton 조회)")
class ResumeReplanLoaderTest {

    private static final Long INTERVIEW_ID = 10L;
    private static final Long OWNER_USER_ID = 1L;

    @InjectMocks
    private ResumeReplanLoader loader;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private ResumeSkeletonPersister skeletonStore;

    @Test
    @DisplayName("정상 - owner + RESUME_BASED + skeleton 모두 충족 시 ReplanContext 반환")
    void load_returnsContext_whenAllValid() {
        // given
        Interview interview = createResumeBasedInterview();
        ResumeSkeleton skeleton = createSkeleton();
        given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);
        given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.of(skeleton));

        // when
        ReplanContext context = loader.load(INTERVIEW_ID, OWNER_USER_ID);

        // then
        assertThat(context.skeleton()).isSameAs(skeleton);
        assertThat(context.durationMinutes()).isEqualTo(30);
    }

    @Test
    @DisplayName("타 유저 호출 시 INTERVIEW_NOT_FOUND (404, 정보 누출 방지)")
    void load_rejects_whenNotOwner() {
        // given
        Interview interview = createResumeBasedInterview();
        given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);

        // when / then
        assertThatThrownBy(() -> loader.load(INTERVIEW_ID, 999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(InterviewErrorCode.NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("RESUME_BASED 외 인터뷰 호출 시 INTERVIEW_NOT_RESUME_BASED (400)")
    void load_rejects_whenInterviewNotResumeBased() {
        // given
        Interview interview = createCsInterview();
        given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);

        // when / then
        assertThatThrownBy(() -> loader.load(INTERVIEW_ID, OWNER_USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode())
                            .isEqualTo(InterviewErrorCode.INTERVIEW_NOT_RESUME_BASED.getCode());
                });
    }

    @Test
    @DisplayName("skeleton 부재 시 RESUME_PLAN_NOT_READY (409)")
    void load_rejects_whenSkeletonMissing() {
        // given
        Interview interview = createResumeBasedInterview();
        given(interviewFinder.findById(INTERVIEW_ID)).willReturn(interview);
        given(skeletonStore.findByInterviewId(INTERVIEW_ID)).willReturn(Optional.empty());

        // when / then
        assertThatThrownBy(() -> loader.load(INTERVIEW_ID, OWNER_USER_ID))
                .isInstanceOf(BusinessException.class)
                .satisfies(e -> {
                    BusinessException be = (BusinessException) e;
                    assertThat(be.getCode()).isEqualTo(ResumeErrorCode.RESUME_PLAN_NOT_READY.getCode());
                });
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
}
