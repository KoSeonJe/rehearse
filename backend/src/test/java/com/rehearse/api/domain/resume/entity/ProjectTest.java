package com.rehearse.api.domain.resume.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Project record - skeleton 단일 진실 invariant")
class ProjectTest {

    @Nested
    @DisplayName("정상 생성")
    class HappyPath {

        @Test
        @DisplayName("projectId 와 projectName 이 모두 유효하면 Project 가 생성된다")
        void create_when_required_fields_present() {
            Project project = new Project(
                    "p1",
                    "주문 API 캐싱 프로젝트",
                    List.of(),
                    List.of()
            );

            assertThat(project.projectId()).isEqualTo("p1");
            assertThat(project.projectName()).isEqualTo("주문 API 캐싱 프로젝트");
            assertThat(project.claims()).isEmpty();
            assertThat(project.implicitCsTopics()).isEmpty();
        }

        @Test
        @DisplayName("claims / implicitCsTopics 가 null 이어도 빈 리스트로 정규화된다")
        void normalize_null_collections_to_empty() {
            Project project = new Project("p1", "이름", null, null);

            assertThat(project.claims()).isEmpty();
            assertThat(project.implicitCsTopics()).isEmpty();
        }
    }

    @Nested
    @DisplayName("projectId invariant")
    class ProjectIdInvariant {

        @Test
        @DisplayName("projectId 가 null 이면 예외가 발생한다")
        void throw_when_projectId_null() {
            assertThatThrownBy(() -> new Project(null, "이름", List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("projectId");
        }

        @Test
        @DisplayName("projectId 가 공백이면 예외가 발생한다")
        void throw_when_projectId_blank() {
            assertThatThrownBy(() -> new Project("   ", "이름", List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("projectId");
        }
    }

    @Nested
    @DisplayName("projectName invariant")
    class ProjectNameInvariant {

        @Test
        @DisplayName("projectName 이 null 이면 예외가 발생한다")
        void throw_when_projectName_null() {
            assertThatThrownBy(() -> new Project("p1", null, List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("projectName");
        }

        @Test
        @DisplayName("projectName 이 공백이면 예외가 발생한다")
        void throw_when_projectName_blank() {
            assertThatThrownBy(() -> new Project("p1", "   ", List.of(), List.of()))
                    .isInstanceOf(IllegalArgumentException.class)
                    .hasMessageContaining("projectName");
        }
    }
}
