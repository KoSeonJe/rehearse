package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.*;
import com.rehearse.api.domain.interview.dto.FollowUpContext;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.Question;
import com.rehearse.api.domain.questionset.entity.QuestionType;
import com.rehearse.api.domain.questionset.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import com.rehearse.api.infra.ai.dto.FollowUpGenerationRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewServiceTest {

    @InjectMocks
    private InterviewService interviewService;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionRepository questionRepository;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private AiClient aiClient;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private com.rehearse.api.infra.ai.SttService sttService;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Mock
    private com.rehearse.api.domain.questionset.service.QuestionSetService questionSetService;

    @Mock
    private FollowUpTransactionHandler followUpTransactionHandler;

    @Test
    @DisplayName("면접 세션 생성 시 비동기 질문 생성을 트리거하고 즉시 응답한다")
    void createInterview_success() {
        // given
        CreateInterviewRequest request = new CreateInterviewRequest();
        ReflectionTestUtils.setField(request, "position", Position.BACKEND);
        ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
        ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.CS_FUNDAMENTAL));
        ReflectionTestUtils.setField(request, "durationMinutes", 30);

        given(interviewRepository.save(any(Interview.class)))
                .willAnswer(invocation -> {
                    Interview interview = invocation.getArgument(0);
                    ReflectionTestUtils.setField(interview, "id", 1L);
                    return interview;
                });

        // when
        InterviewResponse response = interviewService.createInterview(request, null);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
        assertThat(response.getLevel()).isEqualTo(InterviewLevel.JUNIOR);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
        assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
        assertThat(response.getQuestionSets()).isEmpty();

        then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
        then(interviewRepository).should().save(any(Interview.class));
    }

    @Test
    @DisplayName("존재하지 않는 면접 세션 조회 시 BusinessException이 발생한다")
    void getInterview_notFound() {
        given(interviewFinder.findById(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        assertThatThrownBy(() -> interviewService.getInterview(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                });
    }

    @Test
    @DisplayName("면접 세션 조회 성공")
    void getInterview_success() {
        Interview interview = createMockInterview();
        given(interviewFinder.findById(1L)).willReturn(interview);
        given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(List.of());

        InterviewResponse response = interviewService.getInterview(1L);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPosition()).isEqualTo(Position.BACKEND);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
        assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
    }

    @Test
    @DisplayName("READY -> IN_PROGRESS 상태 전이 성공 (질문 생성 완료 시)")
    void updateStatus_readyToInProgress() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        UpdateStatusResponse response = interviewService.updateStatus(1L, request);

        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("질문 생성 미완료 시 IN_PROGRESS 전환 실패")
    void updateStatus_questionGenerationNotCompleted() {
        Interview interview = createMockInterview(); // PENDING 상태
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        assertThatThrownBy(() -> interviewService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_004");
                });
    }

    @Test
    @DisplayName("존재하지 않는 면접 세션 상태 변경 시 BusinessException이 발생한다")
    void updateStatus_notFound() {
        given(interviewFinder.findById(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        assertThatThrownBy(() -> interviewService.updateStatus(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                });
    }

    @Test
    @DisplayName("READY -> COMPLETED 잘못된 상태 전이 시 BusinessException이 발생한다")
    void updateStatus_invalidTransition() {
        Interview interview = createMockInterview();
        interview.completeQuestionGeneration();
        given(interviewFinder.findById(1L)).willReturn(interview);

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.COMPLETED);

        assertThatThrownBy(() -> interviewService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_002");
                });
    }

    @Test
    @DisplayName("FAILED 상태에서 질문 생성 재시도 성공")
    void retryQuestionGeneration_success() {
        Interview interview = createMockInterview();
        interview.failQuestionGeneration("Claude API timeout");
        given(interviewFinder.findById(1L)).willReturn(interview);
        given(questionSetRepository.findByInterviewIdWithQuestions(1L)).willReturn(Collections.emptyList());

        InterviewResponse response = interviewService.retryQuestionGeneration(1L);

        assertThat(response.getQuestionGenerationStatus()).isEqualTo(QuestionGenerationStatus.PENDING);
        assertThat(response.getFailureReason()).isNull();
        then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
    }

    @Test
    @DisplayName("FAILED가 아닌 상태에서 재시도 시 예외 발생")
    void retryQuestionGeneration_notFailed() {
        Interview interview = createMockInterview(); // PENDING
        given(interviewFinder.findById(1L)).willReturn(interview);

        assertThatThrownBy(() -> interviewService.retryQuestionGeneration(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_005");
                });
    }

    @Test
    @DisplayName("후속 질문 생성 성공")
    void generateFollowUp_success() {
        // given
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1);
        given(followUpTransactionHandler.loadFollowUpContext(1L, 10L)).willReturn(context);

        GeneratedFollowUp followUp = new GeneratedFollowUp();
        ReflectionTestUtils.setField(followUp, "question", "HashMap의 해시 충돌 해결 방법은?");
        ReflectionTestUtils.setField(followUp, "reason", "자료구조 깊이 확인");
        ReflectionTestUtils.setField(followUp, "type", "DEEP_DIVE");

        given(aiClient.generateFollowUpQuestion(any(FollowUpGenerationRequest.class)))
                .willReturn(followUp);

        Question savedQuestion = Question.builder()
                .questionType(QuestionType.FOLLOWUP)
                .questionText("HashMap의 해시 충돌 해결 방법은?")
                .orderIndex(1)
                .build();
        ReflectionTestUtils.setField(savedQuestion, "id", 100L);
        given(followUpTransactionHandler.saveFollowUpResult(eq(10L), any(GeneratedFollowUp.class), eq(1)))
                .willReturn(savedQuestion);

        org.springframework.mock.web.MockMultipartFile audioFile =
                new org.springframework.mock.web.MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});
        given(sttService.transcribe(audioFile)).willReturn("HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.");

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "HashMap과 TreeMap의 차이점은?");
        ReflectionTestUtils.setField(request, "nonVerbalSummary", "시선 안정적");

        // when
        FollowUpResponse response = interviewService.generateFollowUp(1L, request, audioFile);

        // then
        assertThat(response.getQuestionId()).isEqualTo(100L);
        assertThat(response.getQuestion()).isEqualTo("HashMap의 해시 충돌 해결 방법은?");
        assertThat(response.getReason()).isEqualTo("자료구조 깊이 확인");
        assertThat(response.getType()).isEqualTo("DEEP_DIVE");
    }

    @Test
    @DisplayName("오디오 파일이 있으면 STT로 텍스트를 추출하여 후속질문 생성")
    void generateFollowUp_withAudioFile() {
        // given
        FollowUpContext context = new FollowUpContext(
                Position.BACKEND, null, InterviewLevel.JUNIOR, 10L, 1);
        given(followUpTransactionHandler.loadFollowUpContext(1L, 10L)).willReturn(context);

        org.springframework.mock.web.MockMultipartFile audioFile =
                new org.springframework.mock.web.MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        given(sttService.transcribe(audioFile)).willReturn("STT로 추출된 답변 텍스트입니다.");

        GeneratedFollowUp followUp = new GeneratedFollowUp();
        ReflectionTestUtils.setField(followUp, "question", "구체적으로 어떤 최적화를 하셨나요?");
        ReflectionTestUtils.setField(followUp, "reason", "깊이 확인");
        ReflectionTestUtils.setField(followUp, "type", "DEEP_DIVE");

        given(aiClient.generateFollowUpQuestion(any(FollowUpGenerationRequest.class)))
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
        ReflectionTestUtils.setField(request, "answerText", "");

        // when
        FollowUpResponse response = interviewService.generateFollowUp(1L, request, audioFile);

        // then
        assertThat(response.getQuestionId()).isEqualTo(101L);
        assertThat(response.getAnswerText()).isEqualTo("STT로 추출된 답변 텍스트입니다.");
        then(sttService).should().transcribe(audioFile);
    }

    @Test
    @DisplayName("오디오 파일과 answerText 모두 비어있으면 예외 발생")
    void generateFollowUp_noAnswerText_noAudio() {
        // given
        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");
        ReflectionTestUtils.setField(request, "answerText", "");

        // when & then — resolveAnswerText에서 예외 발생 (DB 호출 전)
        assertThatThrownBy(() -> interviewService.generateFollowUp(1L, request, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_006");
                });
    }

    @Test
    @DisplayName("STT 호출 중 예외 발생 시 그대로 전파된다")
    void generateFollowUp_sttThrowsException_propagates() {
        // given
        org.springframework.mock.web.MockMultipartFile audioFile =
                new org.springframework.mock.web.MockMultipartFile("audio", "audio.webm", "audio/webm", new byte[]{1, 2, 3});

        given(sttService.transcribe(audioFile))
                .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "WHISPER_003", "음성 인식 API 호출에 실패했습니다."));

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");
        ReflectionTestUtils.setField(request, "answerText", "");

        // when & then
        assertThatThrownBy(() -> interviewService.generateFollowUp(1L, request, audioFile))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo("WHISPER_003");
                });
    }

    @Test
    @DisplayName("진행 중이 아닌 면접에서 후속질문 생성 시 예외 발생")
    void generateFollowUp_notInProgress() {
        // given
        given(followUpTransactionHandler.loadFollowUpContext(1L, 10L))
                .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_003", "면접이 진행 중이 아닙니다."));

        FollowUpRequest request = new FollowUpRequest();
        ReflectionTestUtils.setField(request, "questionSetId", 10L);
        ReflectionTestUtils.setField(request, "questionContent", "질문");
        ReflectionTestUtils.setField(request, "answerText", "답변");

        // when & then
        assertThatThrownBy(() -> interviewService.generateFollowUp(1L, request, null))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_003");
                });
    }

    private Interview createMockInterview() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        return interview;
    }

    private QuestionSet createMockQuestionSet(Interview interview) {
        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionCategory.CS)
                .orderIndex(0)
                .build();
        ReflectionTestUtils.setField(qs, "id", 10L);

        Question mainQuestion = Question.builder()
                .questionType(QuestionType.MAIN)
                .questionText("HashMap과 TreeMap의 차이점은?")
                .orderIndex(0)
                .build();
        qs.addQuestion(mainQuestion);
        return qs;
    }
}
