package com.rehearse.api.domain.resume.entity;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("ProjectPlanListJsonConverter - List<ProjectPlan> ↔ JSON 라운드트립")
class ProjectPlanListJsonConverterTest {

    private final ProjectPlanListJsonConverter converter = new ProjectPlanListJsonConverter();

    @Test
    @DisplayName("convertToDatabaseColumn_returns_empty_array_when_null")
    void convert_to_database_returns_empty_array_when_null() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    @DisplayName("convertToEntityAttribute_returns_empty_list_when_null_or_blank")
    void convert_to_entity_returns_empty_list_when_null_or_blank() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
        assertThat(converter.convertToEntityAttribute("")).isEmpty();
    }

    @Test
    @DisplayName("roundtrip_preserves_all_fields")
    void roundtrip_serializes_and_deserializes_correctly() {
        ChainReference chain = new ChainReference("p1::Redis", "Redis", 1, List.of(1, 2, 3));
        InterrogationPhase interrogation = new InterrogationPhase(List.of(chain), List.of());
        PlaygroundPhase playground = new PlaygroundPhase("opener", List.of("p1_c1"));
        ProjectPlan original = new ProjectPlan("p1", "Project Alpha", 1, playground, interrogation);

        String json = converter.convertToDatabaseColumn(List.of(original));
        List<ProjectPlan> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).hasSize(1);
        assertThat(restored.get(0).projectId()).isEqualTo("p1");
        assertThat(restored.get(0).interrogationPhase().primaryChains().get(0).chainId()).isEqualTo("p1::Redis");
        assertThat(restored.get(0).playgroundPhase().expectedClaimsCoverage()).containsExactly("p1_c1");
    }
}
