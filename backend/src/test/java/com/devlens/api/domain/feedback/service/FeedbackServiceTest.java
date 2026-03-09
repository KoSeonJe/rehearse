package com.devlens.api.domain.feedback.service;

import com.devlens.api.domain.feedback.dto.*;
import com.devlens.api.domain.feedback.entity.Feedback;
import com.devlens.api.domain.feedback.entity.FeedbackCategory;
import com.devlens.api.domain.feedback.entity.FeedbackSeverity;
import com.devlens.api.domain.feedback.repository.FeedbackRepository;
import com.devlens.api.domain.feedback.repository.InterviewAnswerRepository;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.AiClient;
import com.devlens.api.infra.ai.dto.GeneratedFeedback;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.Spy;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class FeedbackServiceTest {

    @InjectMocks
    private FeedbackService feedbackService;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private InterviewAnswerRepository interviewAnswerRepository;

    @Mock
    private InterviewRepository interviewRepository;

    @Mock
    private AiClient aiClient;

    @Spy
    private ObjectMapper objectMapper = new ObjectMapper();

    @Test
    @DisplayName("피드백 생성 성공")
    void generateFeedback_success() {
        // given
        Interview interview = createCompletedInterview();
        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        GeneratedFeedback gf = new GeneratedFeedback();
        ReflectionTestUtils.setField(gf, "timestampSeconds", 15.5);
        ReflectionTestUtils.setField(gf, "category", "CONTENT");
        ReflectionTestUtils.setField(gf, "severity", "SUGGESTION");
        ReflectionTestUtils.setField(gf, "content", "답변이 다소 추상적입니다.");
        ReflectionTestUtils.setField(gf, "suggestion", "구체적인 예시를 들어 설명하세요.");

        given(aiClient.generateFeedback(anyString())).willReturn(List.of(gf));
        given(feedbackRepository.saveAll(anyList())).willAnswer(invocation -> {
            List<Feedback> feedbacks = invocation.getArgument(0);
            for (int i = 0; i < feedbacks.size(); i++) {
                ReflectionTestUtils.setField(feedbacks.get(i), "id", (long) (i + 1));
            }
            return feedbacks;
        });

        GenerateFeedbackRequest request = createFeedbackRequest();

        // when
        FeedbackListResponse response = feedbackService.generateFeedback(1L, request);

        // then
        assertThat(response.getInterviewId()).isEqualTo(1L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getFeedbacks().get(0).getContent()).isEqualTo("답변이 다소 추상적입니다.");
        assertThat(response.getFeedbacks().get(0).getCategory()).isEqualTo(FeedbackCategory.CONTENT);

        then(interviewAnswerRepository).should().save(any());
        then(feedbackRepository).should().saveAll(anyList());
    }

    @Test
    @DisplayName("완료되지 않은 면접에서 피드백 생성 시 예외 발생")
    void generateFeedback_notCompleted() {
        // given
        Interview interview = createMockInterview(); // status = READY
        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        GenerateFeedbackRequest request = createFeedbackRequest();

        // when & then
        assertThatThrownBy(() -> feedbackService.generateFeedback(1L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("FEEDBACK_001");
                });
    }

    @Test
    @DisplayName("피드백 조회 성공")
    void getFeedbacks_success() {
        // given
        Interview interview = createCompletedInterview();
        given(interviewRepository.findById(1L)).willReturn(Optional.of(interview));

        Feedback feedback = Feedback.builder()
                .interview(interview)
                .timestampSeconds(10.0)
                .category(FeedbackCategory.VERBAL)
                .severity(FeedbackSeverity.INFO)
                .content("좋은 답변입니다.")
                .build();
        ReflectionTestUtils.setField(feedback, "id", 1L);

        given(feedbackRepository.findByInterviewIdOrderByTimestampSeconds(1L)).willReturn(List.of(feedback));

        // when
        FeedbackListResponse response = feedbackService.getFeedbacks(1L);

        // then
        assertThat(response.getInterviewId()).isEqualTo(1L);
        assertThat(response.getTotalCount()).isEqualTo(1);
        assertThat(response.getFeedbacks().get(0).getContent()).isEqualTo("좋은 답변입니다.");
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

    private Interview createCompletedInterview() {
        Interview interview = createMockInterview();
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interview.updateStatus(InterviewStatus.COMPLETED);
        return interview;
    }

    private GenerateFeedbackRequest createFeedbackRequest() {
        AnswerData answerData = new AnswerData();
        ReflectionTestUtils.setField(answerData, "questionIndex", 0);
        ReflectionTestUtils.setField(answerData, "questionContent", "HashMap과 TreeMap의 차이점은?");
        ReflectionTestUtils.setField(answerData, "answerText", "HashMap은 해시 기반이고 TreeMap은 트리 기반입니다.");
        ReflectionTestUtils.setField(answerData, "nonVerbalSummary", "시선 안정적");

        GenerateFeedbackRequest request = new GenerateFeedbackRequest();
        ReflectionTestUtils.setField(request, "answers", List.of(answerData));
        return request;
    }
}
