package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.CreateInterviewRequest;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
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
            ReflectionTestUtils.setField(request, "interviewTypes", List.of(InterviewType.CS_FUNDAMENTAL));
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
}
