package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiQuestionGeneratorClient;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiStandardQuestionGenerator — prompt 빌드 후 Client.call 응답을 GeneratedQuestion 으로 매핑한다")
class OpenAiStandardQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {"questions":[
              {"content":"GC 동작 원리?","category":"JVM","order":1,"evaluation_criteria":"GC","question_category":"CS"},
              {"content":"인덱스?","category":"DB","order":2,"evaluation_criteria":"idx","question_category":"CS"}
            ]}
            """;

    private OpenAiQuestionGeneratorClient client;
    private QuestionGenerationPromptBuilder promptBuilder;
    private OpenAiStandardQuestionGenerator generator;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiQuestionGeneratorClient.class);
        promptBuilder = mock(QuestionGenerationPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper(), null, null);
        generator = new OpenAiStandardQuestionGenerator(client, promptBuilder, parser);
    }

    private QuestionGenerationRequest sampleRequest() {
        return new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), Set.of(), null, null);
    }

    @Test
    @DisplayName("buildSystemPrompt / buildUserPrompt 결과를 그대로 Client.call 인자로 넘긴다")
    void generate_passesPromptBuilderOutputToClient() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("system-prompt");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("user-prompt");
        when(client.call(eq("system-prompt"), eq("user-prompt"))).thenReturn(VALID_JSON);

        generator.generate(req);

        verify(client).call(eq("system-prompt"), eq("user-prompt"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedQuestion 리스트로 매핑한다")
    void generate_mapsValidJsonToQuestionList() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("usr");
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        List<GeneratedQuestion> result = generator.generate(req);

        assertThat(result).hasSize(2);
        assertThat(result.get(0).content()).isEqualTo("GC 동작 원리?");
        assertThat(result.get(1).category()).isEqualTo("DB");
    }

    @Test
    @DisplayName("Client 응답이 깨진 JSON 이면 PARSE_FAILED 가 던져진다")
    void generate_malformedJson_throwsParseFailed() {
        QuestionGenerationRequest req = sampleRequest();
        when(promptBuilder.buildSystemPrompt(req)).thenReturn("sys");
        when(promptBuilder.buildUserPrompt(req)).thenReturn("usr");
        when(client.call(any(), any())).thenReturn("not a json");

        assertThatThrownBy(() -> generator.generate(req))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }
}
