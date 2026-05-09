package com.rehearse.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("AiDto Architecture - 도메인-인프라 경계")
class AiDtoArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");
    }

    @Test
    @DisplayName("도메인 비즈 영역(controller/repository/event/exception/entity/vo)은 infra.ai.dto 직접 의존 금지")
    void domain_layer_must_not_depend_on_infra_ai_dto_except_service_packages() {
        noClasses()
                .that().resideInAnyPackage(
                        "..domain..controller..",
                        "..domain..repository..",
                        "..domain..event..",
                        "..domain..exception..",
                        "..domain..entity..",
                        "..domain..vo.."
                )
                .should().dependOnClassesThat().resideInAPackage("..infra.ai.dto..")
                .check(importedClasses);
    }
}
