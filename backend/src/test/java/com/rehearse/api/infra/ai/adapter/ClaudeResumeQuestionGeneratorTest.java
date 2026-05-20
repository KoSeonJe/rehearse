package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.ClaudeResumeQuestionClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("ClaudeResumeQuestionGenerator — system prompt 에 JSON 강제 지시문을 prepend 한다")
class ClaudeResumeQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {
              "openers":[{"question":"자기소개","tts_question":"tts-o","best_answer":"ans-o"}],
              "mains":[{"question":"의사결정","tts_question":"tts-m","best_answer":"ans-m"}]
            }
            """;

    private ClaudeResumeQuestionClient client;
    private ResumeQuestionPromptBuilder promptBuilder;
    private ClaudeResumeQuestionGenerator generator;

    @BeforeEach
    void setUp() {
        client = mock(ClaudeResumeQuestionClient.class);
        promptBuilder = mock(ResumeQuestionPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper(), null, null);
        generator = new ClaudeResumeQuestionGenerator(client, promptBuilder, parser);
    }

    private ResumeSkeleton sampleSkeleton() {
        return new ResumeSkeleton(
                "r-1", "hash", CandidateLevel.MID, "backend",
                List.of(new Project("p1", "주문 캐싱", List.of("Redis"), "백엔드", "Cache-Aside", List.of("TTL"))));
    }

    @Test
    @DisplayName("system prompt 앞에 JSON 강제 지시문이 prepend 된다")
    void generate_prependsJsonInstructionToSystemPrompt() {
        when(promptBuilder.build(any(), anyInt(), anyInt()))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("base-sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        generator.generate(sampleSkeleton(), 1, 7);

        ArgumentCaptor<String> sysCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(sysCaptor.capture(), any());
        String captured = sysCaptor.getValue();
        assertThat(captured).contains("JSON");
        assertThat(captured).contains("base-sys");
        assertThat(captured.indexOf("JSON")).isLessThan(captured.indexOf("base-sys"));
    }

    @Test
    @DisplayName("user prompt 는 변경 없이 전달된다")
    void generate_userPromptPassedThrough() {
        when(promptBuilder.build(any(), anyInt(), anyInt()))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("sys", "user-content"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        generator.generate(sampleSkeleton(), 1, 7);

        ArgumentCaptor<String> userCaptor = ArgumentCaptor.forClass(String.class);
        verify(client).call(any(), userCaptor.capture());
        assertThat(userCaptor.getValue()).isEqualTo("user-content");
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedResumeQuestions 로 매핑한다")
    void generate_mapsValidJson() {
        when(promptBuilder.build(any(), anyInt(), anyInt()))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedResumeQuestions result = generator.generate(sampleSkeleton(), 1, 7);

        assertThat(result.openers()).hasSize(1);
        assertThat(result.mains()).hasSize(1);
    }
}
