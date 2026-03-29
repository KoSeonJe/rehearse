package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.CreateInterviewRequest;
import com.rehearse.api.domain.interview.dto.InterviewResponse;
import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.interview.event.QuestionGenerationRequestedEvent;
import com.rehearse.api.domain.interview.repository.InterviewRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.PdfTextExtractor;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.context.ApplicationEventPublisher;
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
        InterviewResponse response = interviewCreationService.createInterview(request, null);

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
}
