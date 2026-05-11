package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewRuntimeState;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.BDDMockito.given;
import static org.mockito.Mockito.mock;

@DisplayName("ResumeRoutePolicy - Resume 트랙 라우팅 판단")
class ResumeRoutePolicyTest {

    private final ResumeRoutePolicy policy = new ResumeRoutePolicy();

    @Nested
    @DisplayName("isResumeTrack")
    class IsResumeTrack {

        @Test
        @DisplayName("runtimeState 에 skeleton 캐시가 있으면 true")
        void returnsTrue_whenSkeletonCachePresent() {
            ResumeSkeleton skeleton = new ResumeSkeleton("r1", "h1", null, "backend", List.of(), Map.of());
            InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", skeleton);
            Interview interview = mock(Interview.class);

            assertThat(policy.isResumeTrack(state, interview)).isTrue();
        }

        @Test
        @DisplayName("skeleton 캐시 없어도 InterviewType 이 RESUME_BASED 면 true")
        void returnsTrue_whenInterviewTypeIsResumeBased() {
            InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));

            assertThat(policy.isResumeTrack(state, interview)).isTrue();
        }

        @Test
        @DisplayName("skeleton 캐시 없고 InterviewType 이 CS_FUNDAMENTAL 이면 false")
        void returnsFalse_whenInterviewTypeIsCs() {
            InterviewRuntimeState state = new InterviewRuntimeState("JUNIOR", null);
            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.CS_FUNDAMENTAL));

            assertThat(policy.isResumeTrack(state, interview)).isFalse();
        }

        @Test
        @DisplayName("state null 이어도 RESUME_BASED 면 true")
        void returnsTrue_whenStateIsNull_andInterviewIsResumeBased() {
            Interview interview = mock(Interview.class);
            given(interview.getInterviewTypes()).willReturn(Set.of(InterviewType.RESUME_BASED));

            assertThat(policy.isResumeTrack(null, interview)).isTrue();
        }
    }
}
