package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.*;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
import com.rehearse.api.infra.ai.prompt.FollowUpPromptBuilder;
import com.rehearse.api.infra.ai.prompt.QuestionGenerationPromptBuilder;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;
import org.springframework.http.MediaType;
import org.springframework.web.client.ResourceAccessException;
import org.springframework.web.client.RestClient;
import org.springframework.web.client.RestClientException;

import java.util.List;
import java.util.Set;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("ClaudeApiClient — API 호출, 재시도, 에러 핸들링")
class ClaudeApiClientTest {

    @Mock private RestClient.Builder restClientBuilder;
    @Mock private RestClient restClient;
    @Mock private RestClient.RequestBodyUriSpec requestBodyUriSpec;
    @Mock private RestClient.RequestBodySpec requestBodySpec;
    @Mock private RestClient.ResponseSpec responseSpec;

    @Mock private QuestionGenerationPromptBuilder questionPromptBuilder;
    @Mock private FollowUpPromptBuilder followUpPromptBuilder;
    @Mock private AiResponseParser responseParser;

    private ClaudeApiClient claudeApiClient;

    @BeforeEach
    void setUp() {
        given(restClientBuilder.baseUrl(anyString())).willReturn(restClientBuilder);
        given(restClientBuilder.requestFactory(any())).willReturn(restClientBuilder);
        given(restClientBuilder.build()).willReturn(restClient);

        claudeApiClient = new ClaudeApiClient(
                restClientBuilder,
                questionPromptBuilder,
                followUpPromptBuilder,
                responseParser,
                "test-api-key",
                "claude-test-model",
                "https://api.anthropic.com/v1/messages"
        );
    }

    // -----------------------------------------------------------------------
    // 공통 헬퍼
    // -----------------------------------------------------------------------

    private void stubRestClientChain(ClaudeResponse response) {
        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
        given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.body(ClaudeResponse.class)).willReturn(response);
    }

    private void stubRestClientChainThrows(RuntimeException ex) {
        given(restClient.post()).willReturn(requestBodyUriSpec);
        given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
        given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
        given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
        given(requestBodySpec.retrieve()).willReturn(responseSpec);
        given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
        given(responseSpec.body(ClaudeResponse.class)).willThrow(ex);
    }

    private ClaudeResponse validResponse(String text) {
        ClaudeResponse response = mock(ClaudeResponse.class);
        ClaudeResponse.Content content = mock(ClaudeResponse.Content.class);
        given(content.getText()).willReturn(text);
        given(response.getContent()).willReturn(List.of(content));
        given(response.getStopReason()).willReturn("end_turn");
        return response;
    }

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

    // -----------------------------------------------------------------------
    // GenerateQuestions
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateQuestions — 질문 생성")
    class GenerateQuestions {

        @Test
        @DisplayName("정상 응답 시 질문 리스트를 반환한다")
        void generateQuestions_validResponse_returnsQuestions() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{\"questions\":[]}");
            stubRestClientChain(response);

            GeneratedQuestion question = mock(GeneratedQuestion.class);
            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of(question));
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when
            List<GeneratedQuestion> result = claudeApiClient.generateQuestions(request);

            // then
            assertThat(result).hasSize(1).containsExactly(question);
        }

        @Test
        @DisplayName("빈 questions 리스트를 반환하면 PARSE_FAILED 예외가 발생한다")
        void generateQuestions_emptyQuestions_throwsParseFailed() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{}");
            stubRestClientChain(response);

            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of());
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
        }

        @Test
        @DisplayName("null questions를 반환하면 PARSE_FAILED 예외가 발생한다")
        void generateQuestions_nullQuestions_throwsParseFailed() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{}");
            stubRestClientChain(response);

            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(null);
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
        }

        @Test
        @DisplayName("프롬프트 빌더가 호출되어 시스템/사용자 프롬프트가 생성된다")
        void generateQuestions_callsPromptBuilder() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system-prompt");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user-prompt");

            ClaudeResponse response = validResponse("{}");
            stubRestClientChain(response);

            GeneratedQuestion question = mock(GeneratedQuestion.class);
            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of(question));
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when
            claudeApiClient.generateQuestions(request);

            // then
            then(questionPromptBuilder).should().buildSystemPrompt(request);
            then(questionPromptBuilder).should().buildUserPrompt(request);
        }
    }

    // -----------------------------------------------------------------------
    // GenerateFollowUp
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateFollowUpQuestion — 후속 질문 생성")
    class GenerateFollowUp {

        @Test
        @DisplayName("정상 응답 시 GeneratedFollowUp을 반환한다")
        void generateFollowUpQuestion_validResponse_returnsFollowUp() {
            // given
            FollowUpGenerationRequest request = followUpRequest();
            given(followUpPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(followUpPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{\"question\":\"후속 질문\"}");
            stubRestClientChain(response);

            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedFollowUp.class)))
                    .willReturn(followUp);

            // when
            GeneratedFollowUp result = claudeApiClient.generateFollowUpQuestion(request);

            // then
            assertThat(result).isEqualTo(followUp);
        }

        @Test
        @DisplayName("후속 질문은 FOLLOW_UP_MODEL과 별도 프롬프트 빌더를 사용한다")
        void generateFollowUpQuestion_usesFollowUpPromptBuilder() {
            // given
            FollowUpGenerationRequest request = followUpRequest();
            given(followUpPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(followUpPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{\"question\":\"후속\"}");
            stubRestClientChain(response);

            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedFollowUp.class)))
                    .willReturn(followUp);

            // when
            claudeApiClient.generateFollowUpQuestion(request);

            // then
            then(followUpPromptBuilder).should().buildSystemPrompt(request);
            then(followUpPromptBuilder).should().buildUserPrompt(request);
            then(questionPromptBuilder).shouldHaveNoInteractions();
        }
    }

    // -----------------------------------------------------------------------
    // RetryBehavior
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("RetryBehavior — 재시도 로직")
    class RetryBehavior {

        @Test
        @DisplayName("RestClientException 발생 시 최대 3회까지 재시도 후 TIMEOUT 예외를 던진다")
        void retry_restClientException_exhaustsAndThrowsTimeout() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            given(restClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
            given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(ClaudeResponse.class))
                    .willThrow(new ResourceAccessException("connection timeout"));

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.TIMEOUT.getCode()));

            then(restClient).should(times(3)).post();
        }

        @Test
        @DisplayName("1회 실패 후 2회째 성공하면 결과를 정상 반환한다")
        void retry_firstFailSecondSuccess_returnsResult() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = validResponse("{}");

            given(restClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
            given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(ClaudeResponse.class))
                    .willThrow(new ResourceAccessException("timeout"))
                    .willReturn(response);

            GeneratedQuestion question = mock(GeneratedQuestion.class);
            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of(question));
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when
            List<GeneratedQuestion> result = claudeApiClient.generateQuestions(request);

            // then
            assertThat(result).hasSize(1);
            then(restClient).should(times(2)).post();
        }

        @Test
        @DisplayName("BusinessException(4xx 에러)은 재시도 없이 즉시 전파된다")
        void retry_businessException_notRetried() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            BusinessException clientError = new BusinessException(AiErrorCode.CLIENT_ERROR);

            given(restClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
            given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(ClaudeResponse.class)).willThrow(clientError);

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.CLIENT_ERROR.getCode()));

            then(restClient).should(times(1)).post();
        }

        @Test
        @DisplayName("모든 재시도 소진 후 TIMEOUT 예외가 발생한다")
        void retry_allAttemptsExhausted_throwsTimeout() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            stubRestClientChainThrows(new RestClientException("server error"));

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.TIMEOUT.getCode()));
        }

        @Test
        @DisplayName("Thread.sleep 중 인터럽트 발생 시 인터럽트 플래그가 복원되고 SERVER_ERROR 예외가 발생한다")
        void retry_interrupted_restoresInterruptFlagAndThrowsServerError() throws Exception {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            given(restClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
            given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(ClaudeResponse.class))
                    .willThrow(new ResourceAccessException("timeout"));

            // when — 별도 스레드에서 실행 후 인터럽트
            BusinessException[] caughtEx = new BusinessException[1];

            Thread testThread = new Thread(() -> {
                try {
                    claudeApiClient.generateQuestions(request);
                } catch (BusinessException e) {
                    caughtEx[0] = e;
                }
            });
            testThread.start();

            // 첫 번째 재시도의 sleep 도중 인터럽트
            Thread.sleep(200);
            testThread.interrupt();
            testThread.join(3000);

            // then
            assertThat(caughtEx[0]).isNotNull();
            assertThat(caughtEx[0].getCode()).isEqualTo(AiErrorCode.SERVER_ERROR.getCode());
        }
    }

    // -----------------------------------------------------------------------
    // ResponseHandling
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("ResponseHandling — 응답 처리")
    class ResponseHandling {

        @Test
        @DisplayName("API가 null 응답을 반환하면 EMPTY_RESPONSE 예외가 발생한다")
        void responseHandling_nullResponse_throwsEmptyResponse() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            given(restClient.post()).willReturn(requestBodyUriSpec);
            given(requestBodyUriSpec.contentType(any(MediaType.class))).willReturn(requestBodySpec);
            given(requestBodySpec.header(anyString(), anyString())).willReturn(requestBodySpec);
            given(requestBodySpec.body(any(Object.class))).willReturn(requestBodySpec);
            given(requestBodySpec.retrieve()).willReturn(responseSpec);
            given(responseSpec.onStatus(any(), any())).willReturn(responseSpec);
            given(responseSpec.body(ClaudeResponse.class)).willReturn(null);

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.EMPTY_RESPONSE.getCode()));
        }

        @Test
        @DisplayName("빈 content 리스트를 반환하면 EMPTY_RESPONSE 예외가 발생한다")
        void responseHandling_emptyContent_throwsEmptyResponse() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = mock(ClaudeResponse.class);
            given(response.getContent()).willReturn(List.of());
            stubRestClientChain(response);

            // when & then
            assertThatThrownBy(() -> claudeApiClient.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.EMPTY_RESPONSE.getCode()));
        }

        @Test
        @DisplayName("stop_reason이 max_tokens인 경우에도 응답이 정상 반환된다 (경고 로그만 발생)")
        void responseHandling_maxTokensStopReason_returnsNormally() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = mock(ClaudeResponse.class);
            ClaudeResponse.Content content = mock(ClaudeResponse.Content.class);
            given(content.getText()).willReturn("{}");
            given(response.getContent()).willReturn(List.of(content));
            given(response.getStopReason()).willReturn("max_tokens");
            stubRestClientChain(response);

            GeneratedQuestion question = mock(GeneratedQuestion.class);
            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of(question));
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when
            List<GeneratedQuestion> result = claudeApiClient.generateQuestions(request);

            // then — 예외 없이 정상 반환
            assertThat(result).hasSize(1);
        }

        @Test
        @DisplayName("usage 정보가 있을 때 토큰 사용량이 로깅된다 (예외 없음)")
        void responseHandling_usagePresent_logsWithoutException() {
            // given
            QuestionGenerationRequest request = questionRequest();
            given(questionPromptBuilder.buildSystemPrompt(request)).willReturn("system");
            given(questionPromptBuilder.buildUserPrompt(request)).willReturn("user");

            ClaudeResponse response = mock(ClaudeResponse.class);
            ClaudeResponse.Content content = mock(ClaudeResponse.Content.class);
            ClaudeResponse.Usage usage = mock(ClaudeResponse.Usage.class);
            given(content.getText()).willReturn("{}");
            given(response.getContent()).willReturn(List.of(content));
            given(response.getStopReason()).willReturn("end_turn");
            given(response.getUsage()).willReturn(usage);
            given(usage.getInputTokens()).willReturn(100);
            given(usage.getOutputTokens()).willReturn(50);
            given(usage.getCacheCreationInputTokens()).willReturn(10);
            given(usage.getCacheReadInputTokens()).willReturn(5);
            stubRestClientChain(response);

            GeneratedQuestion question = mock(GeneratedQuestion.class);
            GeneratedQuestionsWrapper wrapper = mock(GeneratedQuestionsWrapper.class);
            given(wrapper.getQuestions()).willReturn(List.of(question));
            given(responseParser.parseJsonResponse(anyString(), eq(GeneratedQuestionsWrapper.class)))
                    .willReturn(wrapper);

            // when
            List<GeneratedQuestion> result = claudeApiClient.generateQuestions(request);

            // then
            assertThat(result).hasSize(1);
            then(response).should(atLeastOnce()).getUsage();
        }
    }
}
