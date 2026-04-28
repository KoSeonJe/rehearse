package com.rehearse.api.domain.feedback.session.controller;

import com.rehearse.api.domain.feedback.session.SessionFeedbackService;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackPayload;
import com.rehearse.api.domain.feedback.session.dto.SessionFeedbackResponse;
import com.rehearse.api.domain.feedback.session.entity.SessionFeedbackStatus;
import com.rehearse.api.domain.feedback.session.exception.SessionFeedbackBusyException;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.support.WithMockUserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminSessionFeedbackController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
class AdminSessionFeedbackControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private SessionFeedbackService sessionFeedbackService;

    @Test
    @DisplayName("ADMIN 아닌 사용자(USER)는 GET 요청 시 403 반환")
    @WithMockUserId(value = 1L, role = "USER")
    void getSessionFeedback_returns_403_when_not_admin() throws Exception {
        mockMvc.perform(get("/api/admin/interviews/1/session-feedback"))
                .andExpect(status().isForbidden());
    }

    @Test
    @DisplayName("ADMIN 사용자는 GET 요청 시 200 반환")
    @WithMockUserId(value = 1L, role = "ADMIN")
    void getSessionFeedback_returns_200_when_admin() throws Exception {
        SessionFeedbackResponse response = SessionFeedbackResponse.builder()
                .id(10L)
                .interviewId(1L)
                .status(SessionFeedbackStatus.PRELIMINARY)
                .overall(null)
                .strengths(List.of())
                .gaps(List.of())
                .delivery(null)
                .weekPlan(List.of())
                .coverage("all turns scored")
                .deliveryRetryable(true)
                .lastFailureReason(null)
                .retryAttempts(0)
                .createdAt(LocalDateTime.now())
                .updatedAt(LocalDateTime.now())
                .build();
        given(sessionFeedbackService.getByInterview(1L)).willReturn(response);

        mockMvc.perform(get("/api/admin/interviews/1/session-feedback"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.data.interviewId").value(1));
    }

    @Test
    @DisplayName("retry 중복 시 409 반환")
    @WithMockUserId(value = 1L, role = "ADMIN")
    void retryDelivery_returns_409_when_busy() throws Exception {
        willThrow(new SessionFeedbackBusyException())
                .given(sessionFeedbackService).retryDelivery(eq(1L), any());

        mockMvc.perform(post("/api/admin/interviews/1/session-feedback/retry-delivery"))
                .andExpect(status().isConflict());
    }

    @Test
    @DisplayName("ADMIN 사용자는 retry 요청 시 200 반환")
    @WithMockUserId(value = 1L, role = "ADMIN")
    void retryDelivery_returns_200_when_admin() throws Exception {
        willDoNothing().given(sessionFeedbackService).retryDelivery(eq(1L), any());

        mockMvc.perform(post("/api/admin/interviews/1/session-feedback/retry-delivery"))
                .andExpect(status().isOk());
    }
}
