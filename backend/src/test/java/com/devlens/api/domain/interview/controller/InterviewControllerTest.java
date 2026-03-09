package com.devlens.api.domain.interview.controller;

import com.devlens.api.domain.interview.dto.InterviewResponse;
import com.devlens.api.domain.interview.dto.QuestionResponse;
import com.devlens.api.domain.interview.dto.UpdateStatusResponse;
import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.domain.interview.service.InterviewService;
import com.devlens.api.global.exception.BusinessException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(InterviewController.class)
class InterviewControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InterviewService interviewService;

    @Test
    @DisplayName("POST /api/v1/interviews - 면접 세션 생성 성공 (201)")
    void createInterview_success() throws Exception {
        // given
        InterviewResponse response = createMockInterviewResponse();
        given(interviewService.createInterview(any())).willReturn(response);

        String requestBody = """
                {
                    "position": "백엔드 개발자",
                    "level": "JUNIOR",
                    "interviewType": "CS"
                }
                """;

        // when & then
        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.position").value("백엔드 개발자"))
                .andExpect(jsonPath("$.data.level").value("JUNIOR"))
                .andExpect(jsonPath("$.data.interviewType").value("CS"))
                .andExpect(jsonPath("$.data.status").value("READY"))
                .andExpect(jsonPath("$.data.questions").isArray())
                .andExpect(jsonPath("$.data.questions.length()").value(2));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - position 빈값 시 400 VALIDATION_ERROR")
    void createInterview_blankPosition() throws Exception {
        String requestBody = """
                {
                    "position": "",
                    "level": "JUNIOR",
                    "interviewType": "CS"
                }
                """;

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors").isArray())
                .andExpect(jsonPath("$.errors[0].field").value("position"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - position 100자 초과 시 400")
    void createInterview_positionTooLong() throws Exception {
        String longPosition = "a".repeat(101);
        String requestBody = String.format("""
                {
                    "position": "%s",
                    "level": "JUNIOR",
                    "interviewType": "CS"
                }
                """, longPosition);

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"))
                .andExpect(jsonPath("$.errors[0].field").value("position"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - level 누락 시 400")
    void createInterview_missingLevel() throws Exception {
        String requestBody = """
                {
                    "position": "백엔드 개발자",
                    "interviewType": "CS"
                }
                """;

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    @Test
    @DisplayName("POST /api/v1/interviews - Claude API 실패 시 502")
    void createInterview_claudeApiFail() throws Exception {
        given(interviewService.createInterview(any()))
                .willThrow(new BusinessException(HttpStatus.BAD_GATEWAY, "AI_001", "Claude API 호출 실패"));

        String requestBody = """
                {
                    "position": "백엔드 개발자",
                    "level": "JUNIOR",
                    "interviewType": "CS"
                }
                """;

        mockMvc.perform(post("/api/v1/interviews")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadGateway())
                .andExpect(jsonPath("$.code").value("AI_001"))
                .andExpect(jsonPath("$.message").value("Claude API 호출 실패"));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - 조회 성공 (200)")
    void getInterview_success() throws Exception {
        InterviewResponse response = createMockInterviewResponse();
        given(interviewService.getInterview(1L)).willReturn(response);

        mockMvc.perform(get("/api/v1/interviews/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.questions.length()").value(2));
    }

    @Test
    @DisplayName("GET /api/v1/interviews/{id} - 존재하지 않는 세션 404")
    void getInterview_notFound() throws Exception {
        given(interviewService.getInterview(999L))
                .willThrow(new BusinessException(HttpStatus.NOT_FOUND, "INTERVIEW_001", "면접 세션을 찾을 수 없습니다."));

        mockMvc.perform(get("/api/v1/interviews/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.code").value("INTERVIEW_001"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - 상태 변경 성공 (200)")
    void updateStatus_success() throws Exception {
        UpdateStatusResponse response = UpdateStatusResponse.builder().id(1L).status(InterviewStatus.IN_PROGRESS).build();
        given(interviewService.updateStatus(eq(1L), any())).willReturn(response);

        String requestBody = """
                {
                    "status": "IN_PROGRESS"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(1))
                .andExpect(jsonPath("$.data.status").value("IN_PROGRESS"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - 잘못된 상태 전이 409")
    void updateStatus_invalidTransition() throws Exception {
        given(interviewService.updateStatus(eq(1L), any()))
                .willThrow(new BusinessException(HttpStatus.CONFLICT, "INTERVIEW_002", "잘못된 상태 전이입니다."));

        String requestBody = """
                {
                    "status": "COMPLETED"
                }
                """;

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.code").value("INTERVIEW_002"));
    }

    @Test
    @DisplayName("PATCH /api/v1/interviews/{id}/status - status 누락 시 400")
    void updateStatus_missingStatus() throws Exception {
        String requestBody = "{}";

        mockMvc.perform(patch("/api/v1/interviews/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(requestBody))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    private InterviewResponse createMockInterviewResponse() {
        QuestionResponse q1 = QuestionResponse.builder()
                .id(1L).content("HashMap과 TreeMap의 차이점은?").category("자료구조").order(1).build();
        QuestionResponse q2 = QuestionResponse.builder()
                .id(2L).content("프로세스와 스레드의 차이점은?").category("운영체제").order(2).build();

        return InterviewResponse.builder()
                .id(1L)
                .position("백엔드 개발자")
                .level(InterviewLevel.JUNIOR)
                .interviewType(InterviewType.CS)
                .status(InterviewStatus.READY)
                .questions(List.of(q1, q2))
                .createdAt(LocalDateTime.of(2026, 3, 10, 14, 30, 0))
                .build();
    }
}
