package com.rehearse.api.infra.ai.dto;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("GeneratedResumeSkeleton - LLM 응답 매핑 시점 검증")
class GeneratedResumeSkeletonTest {

    @Nested
    @DisplayName("정상 매핑")
    class HappyPath {

        @Test
        @DisplayName("projects 가 있으면 인스턴스를 생성한다")
        void should_construct_when_projects_present() {
            GeneratedResumeSkeleton g = new GeneratedResumeSkeleton(
                    "rid", "MID", "BACKEND", List.of(), null);
            assertThat(g.projects()).isEmpty();
            assertThat(g.interrogationPriorityMap()).isEmpty();
        }

        @Test
        @DisplayName("nested 리스트 null 은 빈 리스트로 정규화된다")
        void should_normalize_nested_null_lists_to_empty() {
            GeneratedResumeSkeleton.GeneratedProject p = new GeneratedResumeSkeleton.GeneratedProject(
                    "p1", "name", null, "role", "arch", null, null, null);
            assertThat(p.techStack()).isEmpty();
            assertThat(p.decisions()).isEmpty();
            assertThat(p.claims()).isEmpty();
            assertThat(p.implicitCsTopics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("매핑 거절")
    class Rejection {

        @Test
        @DisplayName("projects 가 null 이면 거절한다")
        void should_reject_when_projects_null() {
            assertThatThrownBy(() -> new GeneratedResumeSkeleton(
                    "rid", "MID", "BACKEND", null, null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
