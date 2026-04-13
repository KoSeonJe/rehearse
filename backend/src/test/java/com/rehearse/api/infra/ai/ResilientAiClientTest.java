package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.QuestionGenerationRequest;
import com.rehearse.api.infra.ai.exception.AiErrorCode;
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
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
@DisplayName("ResilientAiClient — 이중 클라이언트 폴백, nullable 의존성, 에러 분류")
class ResilientAiClientTest {

    @Mock private OpenAiClient openAiClient;
    @Mock private ClaudeApiClient claudeApiClient;
    @Mock private SttService sttService;

    // -----------------------------------------------------------------------
    // 공통 헬퍼
    // -----------------------------------------------------------------------

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

    private FollowUpGenerationRequest followUpRequestWithAnswer(String answerText) {
        return new FollowUpGenerationRequest(
                Position.BACKEND, null, InterviewLevel.JUNIOR,
                "질문 내용", answerText, null, null, null);
    }

    private MultipartFile audioFile() {
        return new MockMultipartFile("audio", "test.webm", "audio/webm", new byte[]{1, 2, 3});
    }

    private GeneratedFollowUp followUp(String answerText) {
        GeneratedFollowUp f = new GeneratedFollowUp();
        return f.withAnswerText(answerText);
    }

    // -----------------------------------------------------------------------
    // Initialization
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("초기화 — 의존성 조합별 생성 가능 여부")
    class Initialization {

        @Test
        @DisplayName("OpenAI와 Claude 모두 null이면 IllegalStateException이 발생한다")
        void init_bothNull_throwsIllegalStateException() {
            assertThatThrownBy(() -> new ResilientAiClient(null, null, null))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("OpenAI만 있으면 정상 생성된다")
        void init_onlyOpenAi_succeeds() {
            // when & then — 예외 없이 생성되어야 함
            ResilientAiClient client = new ResilientAiClient(openAiClient, null, null);
            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("Claude만 있으면 정상 생성된다")
        void init_onlyClaude_succeeds() {
            // when & then
            ResilientAiClient client = new ResilientAiClient(null, claudeApiClient, null);
            assertThat(client).isNotNull();
        }

        @Test
        @DisplayName("OpenAI와 Claude 모두 있으면 정상 생성된다")
        void init_both_succeeds() {
            // when & then
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            assertThat(client).isNotNull();
        }
    }

    // -----------------------------------------------------------------------
    // GenerateQuestions
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateQuestions — OpenAI 우선, Claude 폴백")
    class GenerateQuestions {

        @Test
        @DisplayName("OpenAI 성공 시 Claude는 호출되지 않는다")
        void generateQuestions_openAiSuccess_claudeNotCalled() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            GeneratedQuestion question = mock(GeneratedQuestion.class);
            given(openAiClient.generateQuestions(request)).willReturn(List.of(question));

            // when
            List<GeneratedQuestion> result = client.generateQuestions(request);

            // then
            assertThat(result).containsExactly(question);
            then(claudeApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("OpenAI 실패 시 Claude로 폴백하여 결과를 반환한다")
        void generateQuestions_openAiFails_fallbackToClaude() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            GeneratedQuestion question = mock(GeneratedQuestion.class);
            given(openAiClient.generateQuestions(request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(claudeApiClient.generateQuestions(request)).willReturn(List.of(question));

            // when
            List<GeneratedQuestion> result = client.generateQuestions(request);

            // then
            assertThat(result).containsExactly(question);
            then(claudeApiClient).should().generateQuestions(request);
        }

        @Test
        @DisplayName("OpenAI와 Claude 모두 실패하면 SERVICE_UNAVAILABLE 예외가 발생한다")
        void generateQuestions_bothFail_throwsServiceUnavailable() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            given(openAiClient.generateQuestions(request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(claudeApiClient.generateQuestions(request)).willThrow(new RuntimeException("Claude 장애"));

            // when & then
            assertThatThrownBy(() -> client.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }

        @Test
        @DisplayName("CLIENT_ERROR 예외 발생 시 폴백 없이 즉시 예외를 전파한다")
        void generateQuestions_clientError_noFallback() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            BusinessException clientError = new BusinessException(AiErrorCode.CLIENT_ERROR);
            given(openAiClient.generateQuestions(request)).willThrow(clientError);

            // when & then
            assertThatThrownBy(() -> client.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.CLIENT_ERROR.getCode()));
            then(claudeApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("PARSE_FAILED 예외 발생 시 폴백 없이 즉시 예외를 전파한다")
        void generateQuestions_parseFailed_noFallback() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            BusinessException parseFailed = new BusinessException(AiErrorCode.PARSE_FAILED);
            given(openAiClient.generateQuestions(request)).willThrow(parseFailed);

            // when & then
            assertThatThrownBy(() -> client.generateQuestions(request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.PARSE_FAILED.getCode()));
            then(claudeApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("OpenAI가 null이면 Claude를 직접 호출한다")
        void generateQuestions_openAiNull_callsClaudeDirectly() {
            // given
            ResilientAiClient client = new ResilientAiClient(null, claudeApiClient, sttService);
            QuestionGenerationRequest request = questionRequest();
            GeneratedQuestion question = mock(GeneratedQuestion.class);
            given(claudeApiClient.generateQuestions(request)).willReturn(List.of(question));

            // when
            List<GeneratedQuestion> result = client.generateQuestions(request);

            // then
            assertThat(result).containsExactly(question);
            then(claudeApiClient).should().generateQuestions(request);
        }
    }

    // -----------------------------------------------------------------------
    // GenerateFollowUp
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateFollowUpQuestion — 후속 질문 생성")
    class GenerateFollowUp {

        @Test
        @DisplayName("OpenAI 성공 시 결과를 직접 반환한다")
        void generateFollowUp_openAiSuccess_returnsDirectly() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            FollowUpGenerationRequest request = followUpRequest();
            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(openAiClient.generateFollowUpQuestion(request)).willReturn(followUp);

            // when
            GeneratedFollowUp result = client.generateFollowUpQuestion(request);

            // then
            assertThat(result).isEqualTo(followUp);
            then(claudeApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("OpenAI 실패 시 Claude로 폴백한다")
        void generateFollowUp_openAiFails_fallbackToClaude() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            FollowUpGenerationRequest request = followUpRequest();
            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(openAiClient.generateFollowUpQuestion(request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(claudeApiClient.generateFollowUpQuestion(request)).willReturn(followUp);

            // when
            GeneratedFollowUp result = client.generateFollowUpQuestion(request);

            // then
            assertThat(result).isEqualTo(followUp);
            then(claudeApiClient).should().generateFollowUpQuestion(request);
        }
    }

    // -----------------------------------------------------------------------
    // GenerateFollowUpWithAudio
    // -----------------------------------------------------------------------

    @Nested
    @DisplayName("generateFollowUpWithAudio — 오디오 기반 후속 질문 생성")
    class GenerateFollowUpWithAudio {

        @Test
        @DisplayName("OpenAI 성공 시 결과를 직접 반환한다")
        void generateFollowUpWithAudio_openAiSuccess_returnsDirectly() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();
            GeneratedFollowUp followUp = mock(GeneratedFollowUp.class);
            given(openAiClient.generateFollowUpWithAudio(audio, request)).willReturn(followUp);

            // when
            GeneratedFollowUp result = client.generateFollowUpWithAudio(audio, request);

            // then
            assertThat(result).isEqualTo(followUp);
            then(sttService).shouldHaveNoInteractions();
            then(claudeApiClient).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("OpenAI 실패 시 STT + Claude 폴백 경로를 사용한다")
        void generateFollowUpWithAudio_openAiFails_sttAndClaudeFallback() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();
            String transcribedText = "STT 변환 텍스트";
            GeneratedFollowUp followUpBase = mock(GeneratedFollowUp.class);
            GeneratedFollowUp followUpWithAnswer = mock(GeneratedFollowUp.class);

            given(openAiClient.generateFollowUpWithAudio(audio, request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(sttService.transcribe(audio)).willReturn(transcribedText);
            given(claudeApiClient.generateFollowUpQuestion(any(FollowUpGenerationRequest.class))).willReturn(followUpBase);
            given(followUpBase.withAnswerText(transcribedText)).willReturn(followUpWithAnswer);

            // when
            GeneratedFollowUp result = client.generateFollowUpWithAudio(audio, request);

            // then
            assertThat(result).isEqualTo(followUpWithAnswer);
            then(sttService).should().transcribe(audio);
            then(claudeApiClient).should().generateFollowUpQuestion(any(FollowUpGenerationRequest.class));
        }

        @Test
        @DisplayName("SttService가 null이면 SERVICE_UNAVAILABLE 예외가 발생한다")
        void generateFollowUpWithAudio_sttNull_throwsServiceUnavailable() {
            // given — sttService = null
            ResilientAiClient client = new ResilientAiClient(null, claudeApiClient, null);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();

            // when & then
            assertThatThrownBy(() -> client.generateFollowUpWithAudio(audio, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }

        @Test
        @DisplayName("STT 실패 시 SERVICE_UNAVAILABLE 예외가 발생한다")
        void generateFollowUpWithAudio_sttFails_throwsServiceUnavailable() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();

            given(openAiClient.generateFollowUpWithAudio(audio, request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(sttService.transcribe(audio)).willThrow(new RuntimeException("STT 장애"));

            // when & then
            assertThatThrownBy(() -> client.generateFollowUpWithAudio(audio, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }

        @Test
        @DisplayName("OpenAI와 Claude 모두 실패하는 이중 장애 시 SERVICE_UNAVAILABLE 예외가 발생한다")
        void generateFollowUpWithAudio_dualFailure_throwsServiceUnavailable() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();
            String transcribedText = "STT 텍스트";

            given(openAiClient.generateFollowUpWithAudio(audio, request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(sttService.transcribe(audio)).willReturn(transcribedText);
            given(claudeApiClient.generateFollowUpQuestion(any(FollowUpGenerationRequest.class)))
                    .willThrow(new RuntimeException("Claude 장애"));

            // when & then
            assertThatThrownBy(() -> client.generateFollowUpWithAudio(audio, request))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> assertThat(((BusinessException) ex).getCode())
                            .isEqualTo(AiErrorCode.SERVICE_UNAVAILABLE.getCode()));
        }

        @Test
        @DisplayName("STT 결과가 Claude 요청의 answerText로 전달된다")
        void generateFollowUpWithAudio_sttResultPassedToClaude() {
            // given
            ResilientAiClient client = new ResilientAiClient(openAiClient, claudeApiClient, sttService);
            MultipartFile audio = audioFile();
            FollowUpGenerationRequest request = followUpRequest();
            String transcribedText = "사용자가 말한 답변 내용";
            GeneratedFollowUp followUpBase = mock(GeneratedFollowUp.class);
            GeneratedFollowUp followUpWithAnswer = mock(GeneratedFollowUp.class);

            given(openAiClient.generateFollowUpWithAudio(audio, request)).willThrow(new RuntimeException("OpenAI 장애"));
            given(sttService.transcribe(audio)).willReturn(transcribedText);
            given(claudeApiClient.generateFollowUpQuestion(argThat(req ->
                    transcribedText.equals(req.answerText())))).willReturn(followUpBase);
            given(followUpBase.withAnswerText(transcribedText)).willReturn(followUpWithAnswer);

            // when
            GeneratedFollowUp result = client.generateFollowUpWithAudio(audio, request);

            // then — Claude가 STT 텍스트를 answerText로 포함한 요청을 받았음을 확인
            then(claudeApiClient).should().generateFollowUpQuestion(argThat(req ->
                    transcribedText.equals(req.answerText())));
            assertThat(result).isEqualTo(followUpWithAnswer);
        }
    }
}
