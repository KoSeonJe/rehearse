package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("DepthSignals — 깊이 신호 VO (null → empty list 변환 + 구버전 JSON 호환)")
class DepthSignalsTest {

    private final ObjectMapper objectMapper = new ObjectMapper();

    @Nested
    @DisplayName("canonical constructor")
    class CanonicalConstructor {

        @Test
        @DisplayName("모든 필드 null → 빈 List 로 변환된다")
        void allFields_null_yields_empty_lists() {
            DepthSignals signals = new DepthSignals(null, null, null, null);

            assertThat(signals.tradeoffs()).isEmpty();
            assertThat(signals.alternatives()).isEmpty();
            assertThat(signals.quantitative()).isEmpty();
            assertThat(signals.decisionRationale()).isEmpty();
        }

        @Test
        @DisplayName("일부 필드 null → 해당 필드만 빈 List 로 변환된다")
        void partialNullFields_yield_empty_lists_for_null_only() {
            DepthSignals signals = new DepthSignals(
                    List.of("trade-1"), null, List.of("q-1"), null);

            assertThat(signals.tradeoffs()).containsExactly("trade-1");
            assertThat(signals.alternatives()).isEmpty();
            assertThat(signals.quantitative()).containsExactly("q-1");
            assertThat(signals.decisionRationale()).isEmpty();
        }

        @Test
        @DisplayName("empty() 팩토리는 4 필드 모두 빈 List 인 인스턴스를 반환한다")
        void empty_factory_returns_all_empty() {
            DepthSignals signals = DepthSignals.empty();

            assertThat(signals.tradeoffs()).isEmpty();
            assertThat(signals.alternatives()).isEmpty();
            assertThat(signals.quantitative()).isEmpty();
            assertThat(signals.decisionRationale()).isEmpty();
        }
    }

    @Nested
    @DisplayName("Jackson 역직렬화")
    class JacksonDeserialization {

        @Test
        @DisplayName("4 필드 모두 명시된 JSON → 동일 값으로 매핑된다")
        void fullJson_maps_to_all_fields() throws Exception {
            String json = """
                    {
                      "tradeoffs": ["t1", "t2"],
                      "alternatives": ["a1"],
                      "quantitative": ["q1"],
                      "decisionRationale": ["d1"]
                    }
                    """;

            DepthSignals signals = objectMapper.readValue(json, DepthSignals.class);

            assertThat(signals.tradeoffs()).containsExactly("t1", "t2");
            assertThat(signals.alternatives()).containsExactly("a1");
            assertThat(signals.quantitative()).containsExactly("q1");
            assertThat(signals.decisionRationale()).containsExactly("d1");
        }

        @Test
        @DisplayName("빈 객체 {} → 모든 필드가 빈 List 로 역직렬화된다")
        void emptyObject_yields_all_empty_lists() throws Exception {
            DepthSignals signals = objectMapper.readValue("{}", DepthSignals.class);

            assertThat(signals.tradeoffs()).isEmpty();
            assertThat(signals.alternatives()).isEmpty();
            assertThat(signals.quantitative()).isEmpty();
            assertThat(signals.decisionRationale()).isEmpty();
        }

        @Test
        @DisplayName("unknown 키가 섞인 JSON 도 역직렬화 성공 (ignoreUnknown=true)")
        void unknown_keys_are_ignored() throws Exception {
            String json = """
                    {
                      "tradeoffs": ["t1"],
                      "unknown_field": "ignored"
                    }
                    """;

            DepthSignals signals = objectMapper.readValue(json, DepthSignals.class);

            assertThat(signals.tradeoffs()).containsExactly("t1");
        }
    }

    @Nested
    @DisplayName("Project 와의 호환")
    class ProjectCompatibility {

        @Test
        @DisplayName("구버전 Project JSON (depth_signals 키 부재) → Project.depthSignals 는 empty 로 채워진다")
        void legacyProjectJson_falls_back_to_empty_depthSignals() throws Exception {
            String legacyJson = """
                    {
                      "projectId": "p1",
                      "projectName": "주문 캐싱",
                      "techStack": ["Redis"],
                      "role": "백엔드",
                      "architecture": "Cache-Aside",
                      "decisions": ["Memcached vs Redis → Redis 채택"]
                    }
                    """;

            Project project = objectMapper.readValue(legacyJson, Project.class);

            assertThat(project.depthSignals()).isNotNull();
            assertThat(project.depthSignals().tradeoffs()).isEmpty();
            assertThat(project.depthSignals().alternatives()).isEmpty();
            assertThat(project.depthSignals().quantitative()).isEmpty();
            assertThat(project.depthSignals().decisionRationale()).isEmpty();
        }

        @Test
        @DisplayName("Project.depthSignals 가 empty 이면 직렬화 결과에 depthSignals 키 자체가 제외된다")
        void emptyDepthSignals_excluded_from_project_serialization() throws Exception {
            Project project = new Project(
                    "p1", "주문 캐싱",
                    List.of("Redis"), "백엔드", "Cache-Aside",
                    List.of("Memcached vs Redis → Redis"),
                    DepthSignals.empty()
            );

            String json = objectMapper.writeValueAsString(project);

            assertThat(json).doesNotContain("depthSignals");
        }

        @Test
        @DisplayName("Project.depthSignals 에 값이 있으면 직렬화 결과에 포함된다")
        void nonEmptyDepthSignals_included_in_project_serialization() throws Exception {
            Project project = new Project(
                    "p1", "주문 캐싱",
                    List.of("Redis"), "백엔드", "Cache-Aside",
                    List.of(),
                    new DepthSignals(List.of("t1"), List.of(), List.of(), List.of())
            );

            String json = objectMapper.writeValueAsString(project);

            assertThat(json).contains("depthSignals");
            assertThat(json).contains("\"tradeoffs\":[\"t1\"]");
        }
    }
}
