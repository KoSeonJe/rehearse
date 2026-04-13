package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("Interview - 면접 엔티티")
class InterviewTest {

    @Nested
    @DisplayName("getEffectiveTechStack 메서드")
    class GetEffectiveTechStack {

        @Test
        @DisplayName("techStack이 null이고 BACKEND이면 JAVA_SPRING을 반환한다")
        void nullTechStack_backendPosition_returnsJavaSpring() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.JAVA_SPRING);
        }

        @Test
        @DisplayName("techStack이 PYTHON_DJANGO이면 PYTHON_DJANGO를 반환한다")
        void pythonDjango_returnsPythonDjango() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.BACKEND)
                    .level(InterviewLevel.MID)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .techStack(TechStack.PYTHON_DJANGO)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.PYTHON_DJANGO);
        }

        @Test
        @DisplayName("techStack이 null이고 FRONTEND이면 REACT_TS를 반환한다")
        void nullTechStack_frontendPosition_returnsReactTs() {
            // given
            Interview interview = Interview.builder()
                    .position(Position.FRONTEND)
                    .level(InterviewLevel.JUNIOR)
                    .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                    .durationMinutes(30)
                    .build();

            // when & then
            assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.REACT_TS);
        }
    }
}
