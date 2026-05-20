package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeStandardQuestionGenerator — system prompt 에 JSON 강제 지시문을 prepend 한다")
class ClaudeStandardQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {"questions":[
              {"content":"GC 동작 원리?","category":"JVM","order":1,"evaluation_criteria":"GC","question_category":"CS"}
            ]}
            """;

    private ClaudeQuestionGeneratorClient client;
    private QuestionGenerationPromptBuilder promptBuilder;
    private ClaudeStandardQuestionGenerator generator;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeQuestionGeneratorClient.class);
        promptBuilder = mock(QuestionGenerationPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper(), null, null);
        generator = new ClaudeStandardQuestionGenerator(client, promptBuilder, parser);
    }

    private QuestionGenerationRequest sampleRequest() {
        return new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), Set.of(), null, null);
    }

    @Test
    @DisplayName("system prompt 앞에 JSON 강제 지시문이 prepend 된다 (Claude 는 response_format 지원 안 함)")
    void generate_prependsJsonInstructionToSystemPrompt() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("base-system-prompt");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("user-prompt");
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        generator.generate(req);

        ArgumentCaptor<String> systemCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(systemCaptor.capture(), any());
        String captured = systemCaptor.getValue();
        assertThat(captured).contains("JSON");
        assertThat(captured).contains("base-system-prompt");
        assertThat(captured.indexOf("JSON"))
                .isLessThan(captured.indexOf("base-system-prompt"));
    }

    @Test
    @DisplayName("user prompt 는 변경 없이 전달된다")
    void generate_userPromptPassedThrough() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("user-prompt-content");
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        generator.generate(req);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(any(), userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo("user-prompt-content");
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedQuestion 리스트로 매핑한다")
    void generate_mapsValidJsonToQuestionList() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("usr");
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        List<GeneratedQuestion> result = generator.generate(req);

        assertThat(result).hasSize(1);
        assertThat(result.get(0).content()).isEqualTo("GC 동작 원리?");
    }
}
