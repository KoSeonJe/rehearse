package com.devlens.api.domain.interview.service;

import com.devlens.api.domain.interview.dto.*;
import com.devlens.api.domain.interview.entity.*;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.ClaudeApiClient;
import com.devlens.api.infra.ai.dto.GeneratedQuestion;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

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
    private ClaudeApiClient claudeApiClient;

    @Test
    @DisplayName("면접 세션 생성 시 Claude API로 질문을 생성하고 저장한다")
    void createInterview_success() {
        // given
        CreateInterviewRequest request = new CreateInterviewRequest();
        ReflectionTestUtils.setField(request, "position", "백엔드 개발자");
        ReflectionTestUtils.setField(request, "level", InterviewLevel.JUNIOR);
        ReflectionTestUtils.setField(request, "interviewType", InterviewType.CS);

        List<GeneratedQuestion> generatedQuestions = createMockGeneratedQuestions();

        given(claudeApiClient.generateQuestions(
                eq("백엔드 개발자"), eq(InterviewLevel.JUNIOR), eq(InterviewType.CS)))
                .willReturn(generatedQuestions);

        given(interviewRepository.save(any(Interview.class)))
                .willAnswer(invocation -> {
                    Interview interview = invocation.getArgument(0);
                    ReflectionTestUtils.setField(interview, "id", 1L);
                    for (int i = 0; i < interview.getQuestions().size(); i++) {
                        ReflectionTestUtils.setField(interview.getQuestions().get(i), "id", (long) (i + 1));
                    }
                    return interview;
                });

        // when
        InterviewResponse response = interviewService.createInterview(request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPosition()).isEqualTo("백엔드 개발자");
        assertThat(response.getLevel()).isEqualTo(InterviewLevel.JUNIOR);
        assertThat(response.getInterviewType()).isEqualTo(InterviewType.CS);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
        assertThat(response.getQuestions()).hasSize(2);
        assertThat(response.getQuestions().get(0).getContent()).isEqualTo("HashMap과 TreeMap의 차이점은?");

        then(interviewRepository).should().save(any(Interview.class));
    }

    @Test
    @DisplayName("존재하지 않는 면접 세션 조회 시 BusinessException이 발생한다")
    void getInterview_notFound() {
        // given
        given(interviewRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
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
        // given
        Interview interview = createMockInterview();

        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        // when
        InterviewResponse response = interviewService.getInterview(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getPosition()).isEqualTo("백엔드 개발자");
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.READY);
    }

    @Test
    @DisplayName("READY -> IN_PROGRESS 상태 전이 성공")
    void updateStatus_readyToInProgress() {
        // given
        Interview interview = createMockInterview();
        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.IN_PROGRESS);

        // when
        UpdateStatusResponse response = interviewService.updateStatus(1L, request);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getStatus()).isEqualTo(InterviewStatus.IN_PROGRESS);
    }

    @Test
    @DisplayName("READY -> COMPLETED 잘못된 상태 전이 시 BusinessException이 발생한다")
    void updateStatus_invalidTransition() {
        // given
        Interview interview = createMockInterview();
        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        UpdateStatusRequest request = new UpdateStatusRequest();
        ReflectionTestUtils.setField(request, "status", InterviewStatus.COMPLETED);

        // when & then
        assertThatThrownBy(() -> interviewService.updateStatus(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_002");
                });
    }

    private Interview createMockInterview() {
        Interview interview = Interview.builder()
                .position("백엔드 개발자")
                .level(InterviewLevel.JUNIOR)
                .interviewType(InterviewType.CS)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        return interview;
    }

    private List<GeneratedQuestion> createMockGeneratedQuestions() {
        GeneratedQuestion q1 = new GeneratedQuestion();
        ReflectionTestUtils.setField(q1, "content", "HashMap과 TreeMap의 차이점은?");
        ReflectionTestUtils.setField(q1, "category", "자료구조");
        ReflectionTestUtils.setField(q1, "order", 1);
        ReflectionTestUtils.setField(q1, "evaluationCriteria", "시간 복잡도 이해");

        GeneratedQuestion q2 = new GeneratedQuestion();
        ReflectionTestUtils.setField(q2, "content", "프로세스와 스레드의 차이점은?");
        ReflectionTestUtils.setField(q2, "category", "운영체제");
        ReflectionTestUtils.setField(q2, "order", 2);
        ReflectionTestUtils.setField(q2, "evaluationCriteria", "멀티스레딩 이해");

        return List.of(q1, q2);
    }
}
