package com.rehearse.api.infra.ai.persona;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

class PersonaResolverTest {

    private PersonaResolver personaResolver;

    @BeforeEach
    void setUp() {
        ProfileYamlLoader loader = new ProfileYamlLoader();
        personaResolver = new PersonaResolver(loader);
    }

    @Test
    @DisplayName("BACKEND + JAVA_SPRING resolve 시 fullPersona에 Java 키워드가 포함된다")
    void resolve_backendJavaSpring_fullPersonaContainsJavaKeyword() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

        assertThat(profile.fullPersona()).contains("Java");
    }

    @Test
    @DisplayName("BACKEND + JAVA_SPRING resolve 시 fullPersona에 백엔드 페르소나 블록이 포함된다")
    void resolve_backendJavaSpring_fullPersonaContainsBackendBlock() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

        assertThat(profile.fullPersona()).contains("백엔드");
    }

    @Test
    @DisplayName("FRONTEND + REACT_TS resolve 시 fullPersona에 React 키워드가 포함된다")
    void resolve_frontendReactTs_fullPersonaContainsReactKeyword() {
        ResolvedProfile profile = personaResolver.resolve(Position.FRONTEND, TechStack.REACT_TS);

        assertThat(profile.fullPersona()).contains("React");
    }

    @Test
    @DisplayName("FRONTEND + REACT_TS resolve 시 fullPersona에 프론트엔드 키워드가 포함된다")
    void resolve_frontendReactTs_fullPersonaContainsFrontendKeyword() {
        ResolvedProfile profile = personaResolver.resolve(Position.FRONTEND, TechStack.REACT_TS);

        assertThat(profile.fullPersona()).contains("프론트엔드");
    }

    @Test
    @DisplayName("overlay가 없는 TechStack(PYTHON_DJANGO) resolve 시 fromBaseOnly 프로필을 반환한다")
    void resolve_pythonDjango_noOverlay_returnsBaseOnlyProfile() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.PYTHON_DJANGO);

        assertThat(profile.interviewTypeGuideMap()).isEmpty();
    }

    @Test
    @DisplayName("overlay가 없는 TechStack resolve 시 verbalExpertise가 빈 문자열이다")
    void resolve_noOverlay_verbalExpertiseIsEmpty() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.PYTHON_DJANGO);

        assertThat(profile.verbalExpertise()).isEmpty();
    }

    @Test
    @DisplayName("getPersona(FULL)은 전체 페르소나 문자열을 반환한다")
    void getPersona_full_returnsFullPersona() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

        assertThat(profile.getPersona(PersonaDepth.FULL)).isEqualTo(profile.fullPersona());
    }

    @Test
    @DisplayName("getPersona(MEDIUM)은 축약 페르소나 문자열을 반환한다")
    void getPersona_medium_returnsMediumPersona() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

        String medium = profile.getPersona(PersonaDepth.MEDIUM);
        assertThat(medium).isEqualTo(profile.mediumPersona());
        assertThat(medium).contains("백엔드(Java/Spring)");
    }

    @Test
    @DisplayName("getPersona(MINIMAL)은 1문장 페르소나를 반환한다")
    void getPersona_minimal_returnsMinimalPersona() {
        ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

        String minimal = profile.getPersona(PersonaDepth.MINIMAL);
        assertThat(minimal).isEqualTo(profile.minimalPersona());
        assertThat(minimal).isNotBlank();
    }

    @ParameterizedTest
    @EnumSource(Position.class)
    @DisplayName("5개 Position의 기본 TechStack 모두 resolve에 성공한다")
    void resolve_allPositionsWithDefaultTechStack_succeeds(Position position) {
        TechStack defaultStack = TechStack.getDefaultForPosition(position);

        ResolvedProfile profile = personaResolver.resolve(position, defaultStack);

        assertThat(profile).isNotNull();
        assertThat(profile.fullPersona()).isNotBlank();
        assertThat(profile.mediumPersona()).isNotBlank();
        assertThat(profile.minimalPersona()).isNotBlank();
    }
}
