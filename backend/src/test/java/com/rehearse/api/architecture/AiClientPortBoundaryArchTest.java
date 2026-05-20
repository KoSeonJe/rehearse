package com.rehearse.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("AiClient 회귀 방지 - God Interface 부활 차단")
class AiClientPortBoundaryArchTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");
    }

    @Test
    @DisplayName("도메인 패키지는 폐기된 AiClient/OpenAiClient/ClaudeApiClient 를 import 할 수 없다")
    void domain_must_not_depend_on_deprecated_ai_clients() {
        noClasses()
                .that().resideInAPackage("..domain..")
                .should().dependOnClassesThat().haveFullyQualifiedName("com.rehearse.api.infra.ai.AiClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.rehearse.api.infra.ai.OpenAiClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.rehearse.api.infra.ai.ClaudeApiClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.rehearse.api.infra.ai.ResilientAiClient")
                .orShould().dependOnClassesThat().haveFullyQualifiedName("com.rehearse.api.infra.ai.AbstractAiClient")
                .check(importedClasses);
    }
}
