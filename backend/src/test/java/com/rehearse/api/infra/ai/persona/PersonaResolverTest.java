package com.rehearse.api.infra.ai.persona;

import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("PersonaResolver - 페르소나 해석")
class PersonaResolverTest {

    private PersonaResolver personaResolver;

    @BeforeEach
    void setUp() {
        ProfileYamlLoader loader = new ProfileYamlLoader();
        personaResolver = new PersonaResolver(loader);
    }

    @Nested
    @DisplayName("resolve 메서드")
    class Resolve {

        @Test
        @DisplayName("BACKEND + JAVA_SPRING 시 fullPersona에 Java 키워드가 포함된다")
        void resolve_backendJavaSpring_fullPersonaContainsJavaKeyword() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

            // then
            assertThat(profile.fullPersona()).contains("Java");
        }

        @Test
        @DisplayName("BACKEND + JAVA_SPRING 시 fullPersona에 백엔드 페르소나 블록이 포함된다")
        void resolve_backendJavaSpring_fullPersonaContainsBackendBlock() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

            // then
            assertThat(profile.fullPersona()).contains("백엔드");
        }

        @Test
        @DisplayName("BACKEND + PYTHON_DJANGO 시 fullPersona에 Python 키워드가 포함된다")
        void resolve_pythonDjango_fullPersonaContainsPythonKeyword() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.PYTHON_DJANGO);

            // then
            assertThat(profile.fullPersona()).contains("Python");
            assertThat(profile.interviewTypeGuideMap()).isNotEmpty();
        }

        @Test
        @DisplayName("BACKEND + PYTHON_DJANGO 시 verbalExpertise가 존재한다")
        void resolve_pythonDjango_verbalExpertiseIsNotEmpty() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.PYTHON_DJANGO);

            // then
            assertThat(profile.verbalExpertise()).isNotBlank();
        }

        @Test
        @DisplayName("FRONTEND + REACT_TS 시 fullPersona에 React 키워드가 포함된다")
        void resolve_frontendReactTs_fullPersonaContainsReactKeyword() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.FRONTEND, TechStack.REACT_TS);

            // then
            assertThat(profile.fullPersona()).contains("React");
        }

        @Test
        @DisplayName("FRONTEND + REACT_TS 시 fullPersona에 프론트엔드 키워드가 포함된다")
        void resolve_frontendReactTs_fullPersonaContainsFrontendKeyword() {
            // when
            ResolvedProfile profile = personaResolver.resolve(Position.FRONTEND, TechStack.REACT_TS);

            // then
            assertThat(profile.fullPersona()).contains("프론트엔드");
        }

        @ParameterizedTest
        @EnumSource(Position.class)
        @DisplayName("5개 Position의 기본 TechStack 모두 resolve에 성공한다")
        void resolve_allPositionsWithDefaultTechStack_succeeds(Position position) {
            // given
            TechStack defaultStack = TechStack.getDefaultForPosition(position);

            // when
            ResolvedProfile profile = personaResolver.resolve(position, defaultStack);

            // then
            assertThat(profile).isNotNull();
            assertThat(profile.fullPersona()).isNotBlank();
            assertThat(profile.mediumPersona()).isNotBlank();
            assertThat(profile.minimalPersona()).isNotBlank();
        }
    }

    @Nested
    @DisplayName("getPersona 메서드")
    class GetPersona {

        @Test
        @DisplayName("getPersona(FULL)은 전체 페르소나 문자열을 반환한다")
        void getPersona_full_returnsFullPersona() {
            // given
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

            // when & then
            assertThat(profile.getPersona(PersonaDepth.FULL)).isEqualTo(profile.fullPersona());
        }

        @Test
        @DisplayName("getPersona(MEDIUM)은 축약 페르소나 문자열을 반환한다")
        void getPersona_medium_returnsMediumPersona() {
            // given
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

            // when
            String medium = profile.getPersona(PersonaDepth.MEDIUM);

            // then
            assertThat(medium).isEqualTo(profile.mediumPersona());
            assertThat(medium).contains("백엔드(Java/Spring)");
        }

        @Test
        @DisplayName("getPersona(MINIMAL)은 1문장 페르소나를 반환한다")
        void getPersona_minimal_returnsMinimalPersona() {
            // given
            ResolvedProfile profile = personaResolver.resolve(Position.BACKEND, TechStack.JAVA_SPRING);

            // when
            String minimal = profile.getPersona(PersonaDepth.MINIMAL);

            // then
            assertThat(minimal).isEqualTo(profile.minimalPersona());
            assertThat(minimal).isNotBlank();
        }
    }

}
