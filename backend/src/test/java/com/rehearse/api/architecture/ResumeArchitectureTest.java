package com.rehearse.api.architecture;

import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import org.junit.jupiter.api.BeforeAll;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noMethods;

@DisplayName("Resume Architecture - 폐기된 트랙 잔재 부활 차단 (plan-481 P3)")
class ResumeArchitectureTest {

    private static JavaClasses importedClasses;

    @BeforeAll
    static void importClasses() {
        importedClasses = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");
    }

    @Test
    @DisplayName("Resume FSM / Orchestrator 계열 클래스는 다시 만들 수 없다")
    void deletedResumeFsmClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("ResumeInterviewOrchestrator")
                .or().haveSimpleName("PlaygroundModeHandler")
                .or().haveSimpleName("InterrogationModeHandler")
                .or().haveSimpleName("ResumeModeTransitionPolicy")
                .or().haveSimpleName("ResumeReplanLoader")
                .or().haveSimpleName("ResumeTurnEventPublisher")
                .or().haveSimpleName("ResumeQuestionResultGenerator")
                .or().haveSimpleName("ResumeTrackPolicy")
                .or().haveSimpleName("InterviewTurnPolicyResolver")
                .or().haveSimpleName("ClockWatcher")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Chain / Plan 계열 클래스는 다시 만들 수 없다")
    void deletedChainAndPlanClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("ChainStateTracker")
                .or().haveSimpleName("ChainStateTrackerSnapshot")
                .or().haveSimpleName("ChainReference")
                .or().haveSimpleName("ChainStep")
                .or().haveSimpleName("InterrogationChain")
                .or().haveSimpleName("InterrogationPhase")
                .or().haveSimpleName("PlaygroundPhase")
                .or().haveSimpleName("Priority")
                .or().haveSimpleName("ResumeClaim")
                .or().haveSimpleName("ClaimType")
                .or().haveSimpleName("StepType")
                .or().haveSimpleName("ResumeMode")
                .or().haveSimpleName("InterviewPlan")
                .or().haveSimpleName("ProjectPlan")
                .or().haveSimpleName("ProjectPlanListJsonConverter")
                .or().haveSimpleName("ResumeInterviewPlanner")
                .or().haveSimpleName("ResumeInterviewPlanValidator")
                .or().haveSimpleName("ResumePlanPreparationService")
                .or().haveSimpleName("PreparedResume")
                .or().haveSimpleName("InterviewPlanPersister")
                .or().haveSimpleName("InterviewPlanRuntimeCache")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("InterviewRuntimeState / 캐시 계열 클래스는 다시 만들 수 없다")
    void deletedRuntimeStateClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("InterviewRuntimeState")
                .or().haveSimpleName("InterviewRuntimeStateCache")
                .or().haveSimpleName("ResumeSkeletonRuntimeCache")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("TurnAnalysis interface 및 분석 마커 계열은 다시 만들 수 없다")
    void deletedTurnAnalysisClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("TurnAnalysis")
                .or().haveSimpleName("TurnAnalysisResult")
                .or().haveSimpleName("TurnAnalysisPipeline")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Perspective 도메인 클래스는 다시 만들 수 없다")
    void deletedPerspectiveClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("AnswerFeedbackPerspective")
                .or().haveSimpleName("AskedPerspectives")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("컨텍스트 / 컴팩션 계열 클래스는 다시 만들 수 없다")
    void deletedContextCompactionClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("DialogueCompactor")
                .or().haveSimpleName("DialogueHistoryLayer")
                .or().haveSimpleName("SessionStateLayer")
                .or().haveSimpleName("SessionStateSnapshot")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Rubric 매핑 YAML 잔재 클래스는 다시 만들 수 없다")
    void deletedRubricMappingClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("MappingResult")
                .or().haveSimpleName("MappingRule")
                .or().haveSimpleName("RubricResolutionContext")
                .or().haveSimpleName("RubricMappingFamily")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("Resume Fallback 클래스는 다시 만들 수 없다")
    void deletedResumeFallbackClassesMustNotResurface() {
        noClasses()
                .that().haveSimpleName("ResumeFallbackQuestions")
                .or().haveSimpleName("ResumeFallbackBestAnswers")
                .should().resideInAnyPackage("..")
                .allowEmptyShould(true)
                .check(importedClasses);
    }

    @Test
    @DisplayName("applyL1FalseNegativeGuard 메서드는 다시 만들 수 없다 (false-negative 보정 로직 폐기)")
    void deletedAnswerAnalysisGuardMethodMustNotResurface() {
        noMethods()
                .that().haveName("applyL1FalseNegativeGuard")
                .should().bePublic()
                .orShould().beProtected()
                .orShould().bePrivate()
                .allowEmptyShould(true)
                .check(importedClasses);
    }
}
