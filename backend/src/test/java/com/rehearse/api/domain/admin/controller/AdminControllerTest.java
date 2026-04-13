package com.rehearse.api.domain.admin.controller;

import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.admin.password=test-pass")
class AdminControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @Nested
    @DisplayName("POST /api/v1/admin/verify")
    class VerifyAdminPassword {

        @Test
        @DisplayName("POST /api/v1/admin/verify - 올바른 비밀번호 입력 시 200 반환")
        void verifyAdminPassword_correctPassword_returns200() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": "test-pass"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/admin/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true));
        }

        @Test
        @DisplayName("POST /api/v1/admin/verify - 잘못된 비밀번호 입력 시 401 반환")
        void verifyAdminPassword_wrongPassword_returns401() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": "wrong-password"
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/admin/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.success").value(false))
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));
        }

        @Test
        @DisplayName("POST /api/v1/admin/verify - 비밀번호가 빈 문자열이면 400 반환")
        void verifyAdminPassword_blankPassword_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/admin/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /api/v1/admin/verify - password 필드가 누락되면 400 반환")
        void verifyAdminPassword_missingPassword_returns400() throws Exception {
            // given
            String requestBody = "{}";

            // when & then
            mockMvc.perform(post("/api/v1/admin/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }

        @Test
        @DisplayName("POST /api/v1/admin/verify - 공백만 포함된 비밀번호는 400 반환")
        void verifyAdminPassword_whitespaceOnlyPassword_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                        "password": "   "
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/admin/verify")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }
}
