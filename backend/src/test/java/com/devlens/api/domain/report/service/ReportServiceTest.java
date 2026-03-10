package com.devlens.api.domain.report.service;

import com.devlens.api.domain.feedback.entity.Feedback;
import com.devlens.api.domain.feedback.entity.FeedbackCategory;
import com.devlens.api.domain.feedback.entity.FeedbackSeverity;
import com.devlens.api.domain.feedback.repository.FeedbackRepository;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.domain.interview.service.InterviewFinder;
import com.devlens.api.domain.report.dto.ReportResponse;
import com.devlens.api.domain.report.entity.InterviewReport;
import com.devlens.api.domain.report.repository.ReportRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.AiClient;
import com.devlens.api.infra.ai.dto.GeneratedReport;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.http.HttpStatus;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Collections;
import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private FeedbackRepository feedbackRepository;

    @Mock
    private InterviewFinder interviewFinder;

    @Mock
    private AiClient aiClient;

    @Test
    @DisplayName("이미 리포트가 존재하면 캐시된 리포트를 반환한다")
    void getReport_existingReport() {
        // given
        Interview interview = createCompletedInterview();
        InterviewReport existingReport = InterviewReport.builder()
                .interview(interview)
                .overallScore(85)
                .summary("전반적으로 우수한 면접")
                .strengths("논리적 사고|기술적 깊이")
                .improvements("구체적 예시 부족|시간 관리")
                .feedbackCount(5)
                .build();
        ReflectionTestUtils.setField(existingReport, "id", 1L);

        given(reportRepository.findByInterviewId(1L)).willReturn(Optional.of(existingReport));

        // when
        ReportResponse response = reportService.getReport(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOverallScore()).isEqualTo(85);
        assertThat(response.getSummary()).isEqualTo("전반적으로 우수한 면접");
        assertThat(response.getStrengths()).containsExactly("논리적 사고", "기술적 깊이");
        assertThat(response.getImprovements()).containsExactly("구체적 예시 부족", "시간 관리");

        then(interviewFinder).shouldHaveNoInteractions();
        then(aiClient).shouldHaveNoInteractions();
    }

    @Test
    @DisplayName("리포트가 없으면 AI로 생성하여 저장 후 반환한다")
    void getReport_generateNew() {
        // given
        Interview interview = createCompletedInterview();

        given(reportRepository.findByInterviewId(1L)).willReturn(Optional.empty());
        given(interviewFinder.findById(1L)).willReturn(interview);

        Feedback feedback = Feedback.builder()
                .interview(interview)
                .timestampSeconds(10.0)
                .category(FeedbackCategory.CONTENT)
                .severity(FeedbackSeverity.SUGGESTION)
                .content("답변이 다소 추상적입니다.")
                .build();
        ReflectionTestUtils.setField(feedback, "id", 1L);

        given(feedbackRepository.findByInterviewIdOrderByTimestampSeconds(1L))
                .willReturn(List.of(feedback));

        GeneratedReport generated = new GeneratedReport();
        ReflectionTestUtils.setField(generated, "overallScore", 75);
        ReflectionTestUtils.setField(generated, "summary", "개선이 필요한 면접");
        ReflectionTestUtils.setField(generated, "strengths", List.of("기본 개념 이해", "성실한 태도"));
        ReflectionTestUtils.setField(generated, "improvements", List.of("구체적 예시 부족", "답변 구조화"));

        given(aiClient.generateReport(anyString())).willReturn(generated);
        given(reportRepository.save(any(InterviewReport.class))).willAnswer(invocation -> {
            InterviewReport report = invocation.getArgument(0);
            ReflectionTestUtils.setField(report, "id", 1L);
            return report;
        });

        // when
        ReportResponse response = reportService.getReport(1L);

        // then
        assertThat(response.getId()).isEqualTo(1L);
        assertThat(response.getOverallScore()).isEqualTo(75);
        assertThat(response.getSummary()).isEqualTo("개선이 필요한 면접");
        assertThat(response.getStrengths()).containsExactly("기본 개념 이해", "성실한 태도");
        assertThat(response.getImprovements()).containsExactly("구체적 예시 부족", "답변 구조화");

        then(reportRepository).should().save(any(InterviewReport.class));
        then(aiClient).should().generateReport(anyString());
    }

    @Test
    @DisplayName("피드백이 없으면 BusinessException이 발생한다")
    void generateAndSaveReport_noFeedback() {
        // given
        Interview interview = createCompletedInterview();

        given(reportRepository.findByInterviewId(1L)).willReturn(Optional.empty());
        given(interviewFinder.findById(1L)).willReturn(interview);
        given(feedbackRepository.findByInterviewIdOrderByTimestampSeconds(1L))
                .willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> reportService.getReport(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REPORT_001");
                });
    }

    @Test
    @DisplayName("면접 세션을 찾을 수 없으면 예외가 전파된다")
    void generateAndSaveReport_interviewNotFound() {
        // given
        given(reportRepository.findByInterviewId(999L)).willReturn(Optional.empty());
        given(interviewFinder.findById(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        // when & then
        assertThatThrownBy(() -> reportService.getReport(999L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.NOT_FOUND);
                    assertThat(be.getCode()).isEqualTo("INTERVIEW_001");
                });
    }

    private Interview createCompletedInterview() {
        Interview interview = Interview.builder()
                .position("백엔드 개발자")
                .level(InterviewLevel.JUNIOR)
                .interviewType(InterviewType.CS)
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interview.updateStatus(InterviewStatus.COMPLETED);
        return interview;
    }
}
