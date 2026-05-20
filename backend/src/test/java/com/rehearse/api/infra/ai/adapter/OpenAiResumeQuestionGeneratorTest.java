package com.rehearse.api.infra.ai.adapter;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.resume.entity.CandidateLevel;
import com.rehearse.api.domain.resume.entity.Project;
import com.rehearse.api.domain.resume.entity.ResumeSkeleton;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiResponseParser;
import com.rehearse.api.infra.ai.client.OpenAiResumeQuestionClient;
import com.rehearse.api.infra.ai.dto.GeneratedResumeQuestions;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.ResumeQuestionPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@DisplayName("OpenAiResumeQuestionGenerator — PromptBuilder 결과를 Client.call 인자로 전달하고 응답을 GeneratedResumeQuestions 로 매핑한다")
class OpenAiResumeQuestionGeneratorTest {

    private static final String VALID_JSON = """
            {
              "openers": [
                {"question":"자기소개","tts_question":"tts-o","best_answer":"ans-o"}
              ],
              "mains": [
                {"question":"주요 의사결정","tts_question":"tts-m","best_answer":"ans-m"}
              ]
            }
            """;

    private OpenAiResumeQuestionClient client;
    private ResumeQuestionPromptBuilder promptBuilder;
    private OpenAiResumeQuestionGenerator generator;

    @BeforeEach
    void setUp() {
        client = mock(OpenAiResumeQuestionClient.class);
        promptBuilder = mock(ResumeQuestionPromptBuilder.class);
        AiResponseParser parser = new AiResponseParser(new ObjectMapper());
        generator = new OpenAiResumeQuestionGenerator(client, promptBuilder, parser);
    }

    private ResumeSkeleton sampleSkeleton() {
        return new ResumeSkeleton(
                "r-1", "hash", CandidateLevel.MID, "backend",
                List.of(new Project("p1", "주문 캐싱", List.of("Redis"), "백엔드", "Cache-Aside", List.of("TTL"))));
    }

    @Test
    @DisplayName("PromptBuilder.build 결과 (system + user) 를 Client.call 에 그대로 전달한다")
    void generate_passesPromptPairToClient() {
        ResumeSkeleton skeleton = sampleSkeleton();
        when(promptBuilder.build(eq(skeleton), eq(1), eq(7)))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("sys-p", "usr-p"));
        when(client.call(eq("sys-p"), eq("usr-p"))).thenReturn(VALID_JSON);

        generator.generate(skeleton, 1, 7);

        verify(client).call(eq("sys-p"), eq("usr-p"));
    }

    @Test
    @DisplayName("정상 JSON 응답을 GeneratedResumeQuestions 로 매핑한다")
    void generate_mapsValidJson() {
        ResumeSkeleton skeleton = sampleSkeleton();
        when(promptBuilder.build(any(), anyInt(), anyInt()))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn(VALID_JSON);

        GeneratedResumeQuestions result = generator.generate(skeleton, 1, 7);

        assertThat(result.openers()).hasSize(1);
        assertThat(result.mains()).hasSize(1);
        assertThat(result.openers().get(0).question()).isEqualTo("자기소개");
    }

    @Test
    @DisplayName("Client 응답이 깨진 JSON 이면 PARSE_FAILED 가 던져진다")
    void generate_malformedJson_throwsParseFailed() {
        ResumeSkeleton skeleton = sampleSkeleton();
        when(promptBuilder.build(any(), anyInt(), anyInt()))
                .thenReturn(new ResumeQuestionPromptBuilder.PromptPair("sys", "usr"));
        when(client.call(any(), any())).thenReturn("not a json");

        assertThatThrownBy(() -> generator.generate(skeleton, 1, 7))
                .isInstanceOf(BusinessException.class)
                .extracting("errorCode")
                .isEqualTo(AiErrorCode.PARSE_FAILED);
    }
}
