package com.rehearse.api.domain.report.controller;

import com.rehearse.api.domain.report.dto.ReportResponse;
import com.rehearse.api.domain.report.service.ReportService;
import com.rehearse.api.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;

import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(ReportController.class)
class ReportControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReportService reportService;

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/report - 리포트 조회 성공 (200)")
    void getReport_success() throws Exception {
        // given
        ReportResponse response = ReportResponse.builder()
                .id(1L)
                .interviewId(1L)
                .overallScore(85)
                .summary("전반적으로 우수한 면접")
                .strengths(List.of("논리적 사고", "기술적 깊이"))
                .improvements(List.of("구체적 예시 부족", "시간 관리"))
                .feedbackCount(5)
                .build();

        given(reportService.getReport(1L)).willReturn(response);

        // when & then
        mockMvc.perform(get("/api/v1/interviews/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.interviewId").value(1))
                .andExpect(jsonPath("$.data.overallScore").value(85))
                .andExpect(jsonPath("$.data.summary").value("전반적으로 우수한 면접"))
                .andExpect(jsonPath("$.data.strengths").isArray())
                .andExpect(jsonPath("$.data.strengths.length()").value(2))
                .andExpect(jsonPath("$.data.improvements.length()").value(2))
                .andExpect(jsonPath("$.data.feedbackCount").value(5));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/report - 면접 세션 없음 (404)")
    void getReport_notFound() throws Exception {
        // given
        given(reportService.getReport(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/interviews/999/report"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("INTERVIEW_001"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id}/report - 피드백 없음 (409)")
    void getReport_noFeedback() throws Exception {
        // given
        given(reportService.getReport(1L))
                .willThrow(new BusinessException(HttpStatus.CONFLICT, "REPORT_001", "피드백이 없어 리포트를 생성할 수 없습니다."));

        // when & then
        mockMvc.perform(get("/api/v1/interviews/1/report"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("REPORT_001"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews/{id}/report - 리포트 생성 성공 (200)")
    void generateReport_success() throws Exception {
        ReportResponse response = ReportResponse.builder()
                .id(1L)
                .interviewId(1L)
                .overallScore(85)
                .summary("전반적으로 우수한 면접")
                .strengths(List.of("논리적 사고", "기술적 깊이"))
                .improvements(List.of("구체적 예시 부족", "시간 관리"))
                .feedbackCount(5)
                .build();

        given(reportService.generateReport(1L)).willReturn(response);

        mockMvc.perform(post("/api/v1/interviews/1/report"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.overallScore").value(85));
    }
}
