package com.rehearse.api.infra.ai;

import com.rehearse.api.infra.ai.context.ContextBuildRequest;
import com.rehearse.api.infra.ai.context.InterviewContextBuilder;
import com.rehearse.api.infra.ai.context.layer.SkeletonCallType;
import com.tngtech.archunit.core.domain.JavaClass;
import com.tngtech.archunit.core.domain.JavaClasses;
import com.tngtech.archunit.core.domain.JavaMethodCall;
import com.tngtech.archunit.core.importer.ClassFileImporter;
import com.tngtech.archunit.core.importer.ImportOption;
import com.tngtech.archunit.lang.ArchCondition;
import com.tngtech.archunit.lang.ConditionEvents;
import com.tngtech.archunit.lang.SimpleConditionEvent;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.classes;
import static com.tngtech.archunit.lang.syntax.ArchRuleDefinition.noClasses;
import static org.assertj.core.api.Assertions.assertThat;

/**
 * Context Engineering 정적 규칙.
 * - Resume 트랙 callType 은 모두 prompt template 파일이 존재해야 한다 (enum skeleton 폴백 대신 외부 파일 운영).
 * - ContextBuildRequest 인스턴스화는 infra.ai.prompt 또는 domain.*.service 패키지에서만 허용 (아키텍처 우회 방지).
 * - *PromptBuilder 가 AiClient.chat 직접 호출 시 반드시 InterviewContextBuilder 경유 (fail-silent 재진입 방지).
 */
class ContextEngineeringArchTest {

    private static final String RESUME_DIR = "/prompts/template/resume/";
    private static final String DEFAULT_DIR = "/prompts/template/";

    @Test
    void every_resume_skeleton_call_type_has_template_file() {
        List<String> missing = new ArrayList<>();
        for (SkeletonCallType callType : SkeletonCallType.values()) {
            if (!callType.value().startsWith("resume_")) {
                continue;
            }
            String filename = callType.value().replace('_', '-') + ".txt";
            boolean exists = getClass().getResource(RESUME_DIR + filename) != null
                    || getClass().getResource(DEFAULT_DIR + filename) != null;
            if (!exists) {
                missing.add(filename);
            }
        }
        assertThat(missing)
                .as("Resume 트랙 템플릿 미발견 callType: %s", missing)
                .isEmpty();
    }

    @Test
    void context_build_request_constructed_only_from_allowed_packages() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");

        noClasses()
                .that().resideOutsideOfPackages(
                        "com.rehearse.api.infra.ai.prompt..",
                        "com.rehearse.api.domain..service..",
                        "com.rehearse.api.infra.ai.context..")
                .should().callConstructorWhere(
                        com.tngtech.archunit.core.domain.JavaConstructorCall.Predicates
                                .target(com.tngtech.archunit.core.domain.properties.HasOwner.Predicates.With.owner(
                                        com.tngtech.archunit.core.domain.JavaClass.Predicates.assignableTo(ContextBuildRequest.class))))
                .check(classes);
    }

    @Test
    void prompt_builders_calling_ai_chat_must_go_through_interview_context_builder() {
        JavaClasses classes = new ClassFileImporter()
                .withImportOption(ImportOption.Predefined.DO_NOT_INCLUDE_TESTS)
                .importPackages("com.rehearse.api");

        ArchCondition<JavaClass> routeThroughContextBuilder = new ArchCondition<>(
                "route AiClient.chat through InterviewContextBuilder") {
            @Override
            public void check(JavaClass item, ConditionEvents events) {
                boolean callsAiChat = item.getMethodCallsFromSelf().stream()
                        .anyMatch(ContextEngineeringArchTest::isAiClientChatCall);
                if (!callsAiChat) {
                    return;
                }
                boolean dependsOnContextBuilder = item.getDirectDependenciesFromSelf().stream()
                        .anyMatch(dep -> dep.getTargetClass().isAssignableTo(InterviewContextBuilder.class));
                if (!dependsOnContextBuilder) {
                    events.add(SimpleConditionEvent.violated(item,
                            String.format("%s calls AiClient.chat without depending on InterviewContextBuilder", item.getName())));
                }
            }
        };

        classes()
                .that().haveSimpleNameEndingWith("PromptBuilder")
                .should(routeThroughContextBuilder)
                .check(classes);
    }

    private static boolean isAiClientChatCall(JavaMethodCall call) {
        return call.getTargetOwner().isAssignableTo(AiClient.class)
                && "chat".equals(call.getName());
    }
}
