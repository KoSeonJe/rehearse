package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.CreateInterviewRequest;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.domain.resume.exception.ResumeErrorCode;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class InterviewCreationServiceTest {

    @InjectMocks
    private InterviewCreationService interviewCreationService;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private PdfTextExtractor pdfTextExtractor;

    @Mock
    private ApplicationEventPublisher eventPublisher;

    @Nested
    @DisplayName("createInterview 메서드")
    class CreateInterview {

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
            InterviewResponse response = interviewCreationService.createInterview(1L, request, null);

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
        @DisplayName("직무에 맞지 않는 기술 스택 지정 시 BusinessException이 발생한다")
        void createInterview_invalidTechStack_throwsBusinessException() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.CS_FUNDAMENTAL));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);
            ReflectionTestUtils.setField(request, "techStack", TechStack.REACT_TS);

            // when & then
            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_007");
                    });

            then(interviewRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("PDF 추출 실패 시 예외가 전파된다")
        void createInterview_pdfExtractionFails_throwsException() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.RESUME_BASED));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", "pdf-content".getBytes());

            given(pdfTextExtractor.extract(any()))
                    .willThrow(new RuntimeException("PDF 파싱 실패"));

            // when & then
            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, resumeFile))
                    .isInstanceOf(RuntimeException.class)
                    .hasMessage("PDF 파싱 실패");

            then(interviewRepository).shouldHaveNoInteractions();
            then(eventPublisher).shouldHaveNoInteractions();
        }
    }

    @Nested
    @DisplayName("Resume Exclusivity 규칙 (4 케이스)")
    class ResumeExclusivity {

        @Test
        @DisplayName("resumeFile + types={CS_FUNDAMENTAL, RESUME_BASED} → 400 RESUME_EXCLUSIVITY_VIOLATION")
        void createInterview_resumeFile_mixedTypes_throwsExclusivityViolation() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes",
                    List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.RESUME_BASED));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", "pdf-content".getBytes());

            // when & then
            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, resumeFile))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCode()).isEqualTo(ResumeErrorCode.RESUME_EXCLUSIVITY_VIOLATION.getCode());
                    });

            then(interviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("resumeFile + types={RESUME_BASED} 단독 → 200 정상 생성")
        void createInterview_resumeFile_resumeBasedOnly_success() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.RESUME_BASED));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", "pdf-content".getBytes());

            given(pdfTextExtractor.extract(any())).willReturn("이력서 내용");
            given(interviewRepository.save(any(Interview.class)))
                    .willAnswer(invocation -> {
                        Interview interview = invocation.getArgument(0);
                        ReflectionTestUtils.setField(interview, "id", 2L);
                        return interview;
                    });

            // when
            InterviewResponse response = interviewCreationService.createInterview(1L, request, resumeFile);

            // then
            assertThat(response.getId()).isEqualTo(2L);
            then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
        }

        @Test
        @DisplayName("resumeFile=null + types={RESUME_BASED} → 400 RESUME_REQUIRED_FOR_RESUME_BASED")
        void createInterview_noFile_resumeBased_throwsRequired() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.RESUME_BASED));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            // when & then
            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCode()).isEqualTo(ResumeErrorCode.RESUME_REQUIRED_FOR_RESUME_BASED.getCode());
                    });

            then(interviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("resumeFile=null + types={CS_FUNDAMENTAL, BEHAVIORAL} → 200 (일반 혼합 허용)")
        void createInterview_noFile_normalMixedTypes_success() {
            // given
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes",
                    List.of(InterviewType.CS_FUNDAMENTAL, InterviewType.BEHAVIORAL));
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            given(interviewRepository.save(any(Interview.class)))
                    .willAnswer(invocation -> {
                        Interview interview = invocation.getArgument(0);
                        ReflectionTestUtils.setField(interview, "id", 3L);
                        return interview;
                    });

            // when
            InterviewResponse response = interviewCreationService.createInterview(1L, request, null);

            // then
            assertThat(response.getId()).isEqualTo(3L);
            then(eventPublisher).should().publishEvent(any(QuestionGenerationRequestedEvent.class));
        }

        @Test
        @DisplayName("interviewTypes=빈 리스트 + resumeFile=null → 400 INTERVIEW_010")
        void createInterview_emptyTypes_throwsInvalidInterviewTypes() {
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of());
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, null))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_010");
                    });

            then(interviewRepository).shouldHaveNoInteractions();
        }

        @Test
        @DisplayName("interviewTypes=빈 리스트 + resumeFile=첨부 → 400 INTERVIEW_010 (파일 있어도 타입 없으면 거부)")
        void createInterview_emptyTypesWithFile_throwsInvalidInterviewTypes() {
            CreateInterviewRequest request = new CreateInterviewRequest();
            ReflectionTestUtils.setField(request, "position", Position.BACKEND);
            ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
            ReflectionTestUtils.setField(request, "interviewTypes", List.of());
            ReflectionTestUtils.setField(request, "durationMinutes", 30);

            MockMultipartFile resumeFile = new MockMultipartFile(
                    "resume", "resume.pdf", "application/pdf", "pdf-content".getBytes());

            assertThatThrownBy(() -> interviewCreationService.createInterview(1L, request, resumeFile))
                    .isInstanceOf(BusinessException.class)
                    .satisfies(ex -> {
                        BusinessException be = (BusinessException) ex;
                        assertThat(be.getStatus()).isEqualTo(HttpStatus.BAD_REQUEST);
                        assertThat(be.getCode()).isEqualTo("INTERVIEW_010");
                    });

            then(interviewRepository).shouldHaveNoInteractions();
        }
    }
}
