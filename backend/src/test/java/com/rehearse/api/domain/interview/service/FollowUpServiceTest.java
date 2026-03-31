package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FollowUpServiceTest {

    @InjectMocks
    private FollowUpService followUpService;

    @Mock
    private AiClient aiClient;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Test
    @DisplayName("후속 질문 생성 성공 — GPT-audio가 STT + 후속질문을 한 번에 처리")
    void generateFollowUp_success() {
        // given
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1);
        given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context);

        GeneratedFollowUp followUp = new GeneratedFollowUp();
        ReflectionTestUtils.setField(followUp, "question", "HashMap의 해시 충돌 해결 방법은?");
        ReflectionTestUtils.setField(followUp, "reason", "자료구조 깊이 확인");
        ReflectionTestUtils.setField(followUp, "type", "DEEP_DIVE");
        ReflectionTestUtils.setField(followUp, "answerText", "HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.");

        given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                .willReturn(followUp);

        Question savedQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText("HashMap의 해시 충돌 해결 방법은?")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(savedQuestion, "id", 100L);
        given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(1)))
                .willReturn(savedQuestion);

        MockMultipartFile audioFile =
                new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "HashMap과 TreeMap의 차이점은?");
        ReflectionTestUtils.setField(request, "nonVerbalSummary", "시선 안정적");

        // when
        FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request, audioFile);

        // then
        assertThat(response.getQuestionId()).isEqualTo(100L);
        assertThat(response.getQuestion()).isEqualTo("HashMap의 해시 충돌 해결 방법은?");
        assertThat(response.getReason()).isEqualTo("자료구조 깊이 확인");
        assertThat(response.getType()).isEqualTo("DEEP_DIVE");
        assertThat(response.getAnswerText()).isEqualTo("HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.");
        then(aiClient).should().generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class));
    }

    @Test
    @DisplayName("오디오 파일로 GPT-audio 호출 시 answerText가 응답에 포함된다")
    void generateFollowUp_withAudioFile() {
        // given
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1);
        given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context);

        MockMultipartFile audioFile =
                new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        GeneratedFollowUp followUp = new GeneratedFollowUp();
        ReflectionTestUtils.setField(followUp, "question", "구체적으로 어떤 최적화를 하셨나요?");
        ReflectionTestUtils.setField(followUp, "reason", "깊이 확인");
        ReflectionTestUtils.setField(followUp, "type", "DEEP_DIVE");
        ReflectionTestUtils.setField(followUp, "answerText", "GPT-audio로 추출된 답변 텍스트입니다.");

        given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                .willReturn(followUp);

        Question savedQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText("구체적으로 어떤 최적화를 하셨나요?")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(savedQuestion, "id", 101L);
        given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(1)))
                .willReturn(savedQuestion);

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "성능 최적화 경험을 말씀해주세요.");

        // when
        FollowUpResponse response = followUpService.generateFollowUp(1L, 1L, request, audioFile);

        // then
        assertThat(response.getQuestionId()).isEqualTo(101L);
        assertThat(response.getAnswerText()).isEqualTo("GPT-audio로 추출된 답변 텍스트입니다.");
    }

    @Test
    @DisplayName("오디오 파일이 없으면 예외 발생")
    void generateFollowUp_noAudioFile() {
        // given
        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");

        // when & then — audioFile null이면 즉시 예외 (DB 호출 전)
        assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_006");
                });
    }

    @Test
    @DisplayName("GPT-audio 호출 중 예외 발생 시 그대로 전파된다")
    void generateFollowUp_audioApiThrowsException_propagates() {
        // given
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1);
        given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L)).willReturn(context);

        MockMultipartFile audioFile =
                new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        given(aiClient.generateFollowUpWithAudio(any(), any(FollowUpGenerationRequest.class)))
                .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_005", "AI 서비스를 사용할 수 없습니다."));

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");

        // when & then
        assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request, audioFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("AI_005");
                });
    }

    @Test
    @DisplayName("진행 중이 아닌 면접에서 후속질문 생성 시 예외 발생")
    void generateFollowUp_notInProgress() {
        // given
        given(followUpTransactionHandler.loadFollowUpContext(1L, 1L, 10L))
                .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_003", "면접이 진행 중이 아닙니다."));

        MockMultipartFile audioFile =
                new MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");

        // when & then
        assertThatThrownBy(() -> followUpService.generateFollowUp(1L, 1L, request, audioFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_003");
                });
    }
}
