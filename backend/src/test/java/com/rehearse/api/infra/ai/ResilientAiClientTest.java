package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.infra.ai.adapter.FollowUpGenerationAdapter;
import com.rehearse.api.infra.ai.adapter.QuestionGenerationAdapter;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.context.metrics.ContextEngineeringMetrics;
import com.rehearse.api.infra.ai.metrics.AiCallMetrics;
import io.micrometer.core.instrument.simple.SimpleMeterRegistry;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.mock;

/**
 * ResilientAiClient — 초기화 조합 및 legacy 3-메서드 adapter 위임 스모크 테스트.
 *
 * <p>chat() 경로의 상세 fallback 분기는 {@link ResilientAiClientFallbackTest},
 * 각 adapter 의 ChatRequest 변환/파싱은 {@code adapter/*AdapterTest} 에서 커버한다.</p>
 */
@ExtendWith(MockitoExtension.class)
@DisplayName("ResilientAiClient — 초기화 + legacy 3-메서드 adapter 위임")
class ResilientAiClientTest {

    @Mock private OpenAiClient openAiClient;
    @Mock private ClaudeApiClient claudeApiClient;
    @Mock private SttService sttService;
    @Mock private QuestionGenerationAdapter questionAdapter;
    @Mock private FollowUpGenerationAdapter followUpAdapter;

    private QuestionGenerationRequest questionRequest() {
        return new QuestionGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                Set.of(InterviewType.CS_FUNDAMENTAL), null, null, 30, null);
    }

    private FollowUpGenerationRequest followUpRequest() {
        return new FollowUpGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                "질문 내용", "답변 내용", null, null, null);
    }

    private MultipartFile audioFile() {
        return new MockMultipartFile("audio", "test.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private ResilientAiClient resilientClient(OpenAiClient oa, ClaudeApiClient ca, SttService stt) {
        SimpleMeterRegistry reg = new SimpleMeterRegistry();
        AiCallMetrics noopMetrics = new AiCallMetrics(reg, new ContextEngineeringMetrics(reg));
        return new ResilientAiClient(oa, ca, stt, noopMetrics, questionAdapter, followUpAdapter);
    }

    @Nested
    @DisplayName("초기화 — 의존성 조합별 생성 가능 여부")
    class Initialization {

        @Test
        @DisplayName("OpenAI와 Claude 모두 null이면 IllegalStateException이 발생한다")
        void init_bothNull_throwsIllegalStateException() {
            assertThatThrownBy(() -> resilientClient(null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("OpenAI만 있으면 정상 생성된다")
        void init_onlyOpenAi_succeeds() {
            assertThat(resilientClient(openAiClient, null, null)).isNotNull();
        }

        @Test
        @DisplayName("Claude만 있으면 정상 생성된다")
        void init_onlyClaude_succeeds() {
            assertThat(resilientClient(null, claudeApiClient, null)).isNotNull();
        }

        @Test
        @DisplayName("OpenAI와 Claude 모두 있으면 정상 생성된다")
        void init_both_succeeds() {
            assertThat(resilientClient(openAiClient, claudeApiClient, sttService)).isNotNull();
        }
    }

    @Nested
    @DisplayName("legacy 3-메서드 위임 — AbstractAiClient → Adapter 호출 스모크")
    class LegacyDelegation {

        @Test
        @DisplayName("generateQuestions() 는 questionAdapter.adapt() 로 위임한다")
        void generateQuestions_delegatesToAdapter() {
            ResilientAiClient client = resilientClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest req = questionRequest();
            GeneratedQuestion question = mock(GeneratedQuestion.class);
            given(questionAdapter.adapt(eq(client), eq(req))).willReturn(List.of(question));

            List<GeneratedQuestion> result = client.generateQuestions(req);

            assertThat(result).containsExactly(question);
            then(questionAdapter).should().adapt(client, req);
        }

        @Test
        @DisplayName("generateFollowUpQuestion() 은 followUpAdapter.adapt() 로 위임한다")
        void generateFollowUp_delegatesToAdapter() {
            ResilientAiClient client = resilientClient(openAiClient, claudeApiClient, sttService);
            FollowUpGenerationRequest req = followUpRequest();
            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(followUpAdapter.adapt(eq(client), eq(req))).willReturn(followUp);

            GeneratedFollowUp result = client.generateFollowUpQuestion(req);

            assertThat(result).isEqualTo(followUp);
            then(followUpAdapter).should().adapt(client, req);
        }

        @Test
        @DisplayName("generateFollowUpWithAudio() 는 followUpAdapter.adaptWithAudio() 로 위임한다")
        void generateFollowUpWithAudio_delegatesToAdapter() {
            ResilientAiClient client = resilientClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest req = followUpRequest();
            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(followUpAdapter.adaptWithAudio(eq(client), eq(audio), eq(req), eq(sttService)))
                    .willReturn(followUp);

            GeneratedFollowUp result = client.generateFollowUpWithAudio(audio, req);

            assertThat(result).isEqualTo(followUp);
            then(followUpAdapter).should().adaptWithAudio(client, audio, req, sttService);
        }
    }
}
