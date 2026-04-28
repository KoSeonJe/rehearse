package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.entity.InterviewTrack;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("InterviewTurnPolicyResolver — InterviewType 조합으로 정책 라우팅")
class InterviewTurnPolicyResolverTest {

    private final StandardFollowUpPolicy standardPolicy = new StandardFollowUpPolicy(2);
    private final ResumeTrackPolicy resumePolicy = new ResumeTrackPolicy();
    private final InterviewTurnPolicyResolver resolver =
            new InterviewTurnPolicyResolver(standardPolicy, resumePolicy);

    @Test
    @DisplayName("CS_FUNDAMENTAL 만 선택된 면접은 StandardFollowUpPolicy 로 라우팅")
    void resolve_csOnly_routesToStandard() {
        Interview interview = interviewOf(InterviewType.CS_FUNDAMENTAL);

        InterviewTurnPolicy resolved = resolver.resolve(interview);

        assertThat(resolved).isSameAs(standardPolicy);
        assertThat(resolved.getTrack()).isEqualTo(InterviewTrack.CS);
    }

    @Test
    @DisplayName("RESUME_BASED 면접은 ResumeTrackPolicy 로 라우팅")
    void resolve_resumeBased_routesToResume() {
        Interview interview = interviewOf(InterviewType.RESUME_BASED);

        InterviewTurnPolicy resolved = resolver.resolve(interview);

        assertThat(resolved).isSameAs(resumePolicy);
        assertThat(resolved.getTrack()).isEqualTo(InterviewTrack.RESUME);
    }

    @Test
    @DisplayName("LANGUAGE_FRAMEWORK 단독 면접은 StandardFollowUpPolicy 로 라우팅")
    void resolve_languageFramework_routesToStandard() {
        Interview interview = interviewOf(InterviewType.LANGUAGE_FRAMEWORK);

        InterviewTurnPolicy resolved = resolver.resolve(interview);

        assertThat(resolved).isSameAs(standardPolicy);
    }

    private Interview interviewOf(InterviewType type) {
        return Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(type))
                .durationMinutes(30)
                .build();
    }
}
