package com.rehearse.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;

@DisplayName("Nonverbal Orphan ArchUnit - 삭제된 매퍼/로더 부활 차단")
class NonverbalOrphanArchTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");
    }

    @Test
    @DisplayName("NonverbalRubricScorer / NonverbalContextWeightsLoader 클래스는 다시 만들 수 없다")
    void deletedNonverbalMappersMustNotResurface() {
        noClasses()
                .that().haveSimpleName("NonverbalRubricScorer")
                .or().haveSimpleName("NonverbalContextWeightsLoader")
                .or().haveSimpleName("NonverbalContextWeight")
                .or().haveSimpleName("NonverbalTurnScore")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
