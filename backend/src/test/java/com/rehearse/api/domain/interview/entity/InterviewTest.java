package com.rehearse.api.domain.interview.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class InterviewTest {

    @Test
    @DisplayName("techStack이 null이고 position이 BACKEND이면 getEffectiveTechStack()은 JAVA_SPRING을 반환한다")
    void getEffectiveTechStack_nullTechStack_backendPosition_returnsJavaSpring() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.JAVA_SPRING);
    }

    @Test
    @DisplayName("techStack이 PYTHON_DJANGO이면 getEffectiveTechStack()은 PYTHON_DJANGO를 반환한다")
    void getEffectiveTechStack_pythonDjango_returnsPythonDjango() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.MID)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .techStack(TechStack.PYTHON_DJANGO)
                .build();

        assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.PYTHON_DJANGO);
    }

    @Test
    @DisplayName("techStack이 null이고 position이 FRONTEND이면 getEffectiveTechStack()은 REACT_TS를 반환한다")
    void getEffectiveTechStack_nullTechStack_frontendPosition_returnsReactTs() {
        Interview interview = Interview.builder()
                .position(Position.FRONTEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();

        assertThat(interview.getEffectiveTechStack()).isEqualTo(TechStack.REACT_TS);
    }
}
