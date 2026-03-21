package com.rehearse.api.infra.ai.prompt;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.persona.PersonaResolver;
import com.rehearse.api.infra.ai.persona.ProfileYamlLoader;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;

class QuestionGenerationPromptBuilderTest {

    private QuestionGenerationPromptBuilder builder;

    @BeforeEach
    void setUp() {
        ProfileYamlLoader loader = new ProfileYamlLoader();
        PersonaResolver personaResolver = new PersonaResolver(loader);
        builder = new QuestionGenerationPromptBuilder(personaResolver);
        // @PostConstruct 수동 호출
        ReflectionTestUtils.invokeMethod(builder, "init");
    }

    @Test
    @DisplayName("CS_FUNDAMENTAL 포함 + csSubTopics 있을 때 system prompt에 CS 세부 주제 블록이 포함된다")
    void buildSystemPrompt_csSubTopicsPresent_containsCsSubtopicBlock() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL),
                Set.of("OS", "NETWORK"),
                null,
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).contains("CS 세부 주제");
    }

    @Test
    @DisplayName("CS_FUNDAMENTAL 미포함 시 system prompt에 CS 세부 주제 내용(OS, NETWORK)이 없다")
    void buildSystemPrompt_noCsFundamental_doesNotContainCsSubtopicContent() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.JUNIOR,
                Set.of(InterviewType.JAVA_SPRING),
                Set.of("OS", "NETWORK"),
                null,
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).doesNotContain("에서만 출제");
    }

    @Test
    @DisplayName("resumeText가 있을 때 system prompt에 이력서 활용 블록이 포함된다")
    void buildSystemPrompt_resumeTextPresent_containsResumeBlock() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.JUNIOR,
                Set.of(InterviewType.RESUME_BASED),
                null,
                "Spring Boot 프로젝트 경험 2년",
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).contains("이력서 활용");
    }

    @Test
    @DisplayName("resumeText가 null이면 system prompt에 이력서 활용 내용(RESUME_BASED 지침)이 없다")
    void buildSystemPrompt_resumeTextNull_doesNotContainResumeContent() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.JUNIOR,
                Set.of(InterviewType.JAVA_SPRING),
                null,
                null,
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).doesNotContain("RESUME_BASED 질문은 이력서의");
    }

    @Test
    @DisplayName("interviewTypes=[JAVA_SPRING, SYSTEM_DESIGN] 시 해당 2개 가이드만 포함된다")
    void buildSystemPrompt_specificInterviewTypes_containsOnlyMatchingGuides() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.MID,
                Set.of(InterviewType.JAVA_SPRING, InterviewType.SYSTEM_DESIGN),
                null,
                null,
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).contains("JAVA_SPRING");
        assertThat(prompt).contains("SYSTEM_DESIGN");
        assertThat(prompt).doesNotContain("CS_FUNDAMENTAL:");
        assertThat(prompt).doesNotContain("BEHAVIORAL:");
    }

    @Test
    @DisplayName("level=JUNIOR 시 system prompt에 JUNIOR 가이드만 포함되고 MID/SENIOR 가이드는 없다")
    void buildSystemPrompt_juniorLevel_containsOnlyJuniorGuide() {
        QuestionGenerationRequest req = new QuestionGenerationRequest(
                Position.BACKEND,
                null,
                InterviewLevel.JUNIOR,
                Set.of(InterviewType.JAVA_SPRING),
                null,
                null,
                30,
                TechStack.JAVA_SPRING
        );

        String prompt = builder.buildSystemPrompt(req);

        assertThat(prompt).contains("JUNIOR:");
        assertThat(prompt).doesNotContain("MID:");
        assertThat(prompt).doesNotContain("SENIOR:");
    }
}
