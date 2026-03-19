package com.rehearse.api.domain.report.service;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.questionset.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.entity.InterviewReport;
import com.rehearse.api.domain.report.repository.ReportRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
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
import static org.mockito.BDDMockito.*;

@ExtendWith(MockitoExtension.class)
class ReportServiceTest {

    @InjectMocks
    private ReportService reportService;

    @Mock
    private ReportRepository reportRepository;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private QuestionSetFeedbackRepository feedbackRepository;

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
                .strengths(List.of("논리적 사고", "기술적 깊이"))
                .improvements(List.of("구체적 예시 부족", "시간 관리"))
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
    }

    @Test
    @DisplayName("리포트가 없고 분석 미완료이면 ANALYSIS_NOT_COMPLETED 예외가 발생한다")
    void getReport_analysisNotCompleted() {
        // given
        given(reportRepository.findByInterviewId(1L)).willReturn(Optional.empty());
        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(Collections.emptyList());

        // when & then
        assertThatThrownBy(() -> reportService.getReport(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.CONFLICT);
                    assertThat(be.getCode()).isEqualTo("REPORT_003");
                });
    }

    @Test
    @DisplayName("리포트가 없고 분석 완료이면 REPORT_GENERATING 예외가 발생한다 (202)")
    void getReport_reportGenerating() {
        // given
        given(reportRepository.findByInterviewId(1L)).willReturn(Optional.empty());

        com.rehearse.api.domain.questionset.entity.QuestionSet completedSet =
                mock(com.rehearse.api.domain.questionset.entity.QuestionSet.class);
        given(completedSet.getAnalysisStatus())
                .willReturn(com.rehearse.api.domain.questionset.entity.AnalysisStatus.COMPLETED);

        given(questionSetRepository.findByInterviewIdOrderByOrderIndex(1L))
                .willReturn(List.of(completedSet));

        // when & then
        assertThatThrownBy(() -> reportService.getReport(1L))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getStatus()).isEqualTo(HttpStatus.ACCEPTED);
                    assertThat(be.getCode()).isEqualTo("REPORT_004");
                });
    }

    private Interview createCompletedInterview() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .build();
        ReflectionTestUtils.setField(interview, "id", 1L);
        interview.updateStatus(InterviewStatus.IN_PROGRESS);
        interview.updateStatus(InterviewStatus.COMPLETED);
        return interview;
    }
}
