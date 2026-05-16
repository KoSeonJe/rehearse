package com.rehearse.api.domain.question.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.question.dto.AdminQuestionPoolResponse;
import com.rehearse.api.domain.question.dto.AdminQuestionPoolSearchCondition;
import com.rehearse.api.domain.question.service.AdminQuestionPoolService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = AdminQuestionPoolController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "app.admin.password=test-pass")
class AdminQuestionPoolControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private AdminQuestionPoolService adminQuestionPoolService;

    @Nested
    @DisplayName("GET /api/v1/admin/question-pools - 인증")
    class GetAuthentication {

        @Test
        @DisplayName("관리자 비밀번호가 없으면 401을 반환한다")
        void returns401_when_passwordMissing() throws Exception {
            mockMvc.perform(get("/api/v1/admin/question-pools"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));
        }

        @Test
        @DisplayName("관리자 비밀번호가 틀리면 401을 반환한다")
        void returns401_when_passwordInvalid() throws Exception {
            mockMvc.perform(get("/api/v1/admin/question-pools")
                            .header("X-Admin-Password", "wrong"))
                    .andExpect(status().isUnauthorized())
                    .andExpect(jsonPath("$.code").value("ADMIN_001"));
        }
    }

    @Nested
    @DisplayName("GET /api/v1/admin/question-pools - 목록 조회")
    class Search {

        @Test
        @DisplayName("필터와 페이지네이션으로 질문 풀 목록을 조회한다")
        void returnsQuestionPools_when_filtersProvided() throws Exception {
            AdminQuestionPoolResponse response = response(1L);
            given(adminQuestionPoolService.search(any(), any()))
                    .willReturn(new PageImpl<>(List.of(response), PageRequest.of(2, 10), 31));

            mockMvc.perform(get("/api/v1/admin/question-pools")
                            .header("X-Admin-Password", "test-pass")
                            .param("page", "2")
                            .param("size", "10")
                            .param("cacheKey", "JUNIOR")
                            .param("category", "운영체제")
                            .param("isActive", "true")
                            .param("keyword", "스레드"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.content[0].id").value(1))
                    .andExpect(jsonPath("$.data.content[0].cacheKey").value("JUNIOR:CS_FUNDAMENTAL"))
                    .andExpect(jsonPath("$.data.content[0].content").value("프로세스와 스레드의 차이는 무엇인가요?"))
                    .andExpect(jsonPath("$.data.content[0].bestAnswer").value("프로세스는 독립 주소 공간을 가집니다."))
                    .andExpect(jsonPath("$.data.totalElements").value(31));

            then(adminQuestionPoolService).should().search(
                    new AdminQuestionPoolSearchCondition("JUNIOR", "운영체제", true, "스레드"),
                    PageRequest.of(2, 10));
        }

        @Test
        @DisplayName("size가 100을 초과하면 100으로 보정한다")
        void clampsSize_when_sizeOver100() throws Exception {
            given(adminQuestionPoolService.search(any(), any()))
                    .willReturn(new PageImpl<>(List.of(), PageRequest.of(0, 100), 0));

            mockMvc.perform(get("/api/v1/admin/question-pools")
                            .header("X-Admin-Password", "test-pass")
                            .param("size", "200"))
                    .andExpect(status().isOk());

            then(adminQuestionPoolService).should().search(
                    new AdminQuestionPoolSearchCondition(null, null, null, null),
                    PageRequest.of(0, 100));
        }
    }

    @Nested
    @DisplayName("POST /api/v1/admin/question-pools - 생성")
    class Create {

        @Test
        @DisplayName("질문 풀을 생성하고 생성된 row를 반환한다")
        void createsQuestionPool_when_requestValid() throws Exception {
            given(adminQuestionPoolService.create(any())).willReturn(response(2L));

            String requestBody = """
                    {
                      "cacheKey": "JUNIOR:CS_FUNDAMENTAL",
                      "content": "프로세스와 스레드의 차이는 무엇인가요?",
                      "ttsContent": "프로세스와 스레드의 차이는 무엇인가요?",
                      "category": "운영체제",
                      "bestAnswer": "프로세스는 독립 주소 공간을 가집니다."
                    }
                    """;

            mockMvc.perform(post("/api/v1/admin/question-pools")
                            .header("X-Admin-Password", "test-pass")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.id").value(2))
                    .andExpect(jsonPath("$.data.isActive").value(true));
        }

        @Test
        @DisplayName("cacheKey가 비어 있으면 400을 반환한다")
        void returns400_when_cacheKeyBlank() throws Exception {
            String requestBody = objectMapper.writeValueAsString(new BlankCacheKeyRequest());

            mockMvc.perform(post("/api/v1/admin/question-pools")
                            .header("X-Admin-Password", "test-pass")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest())
                    .andExpect(jsonPath("$.success").value(false));
        }
    }

    private AdminQuestionPoolResponse response(Long id) {
        return new AdminQuestionPoolResponse(
                id,
                "JUNIOR:CS_FUNDAMENTAL",
                "프로세스와 스레드의 차이는 무엇인가요?",
                "프로세스와 스레드의 차이는 무엇인가요?",
                "운영체제",
                "프로세스는 독립 주소 공간을 가집니다.",
                true,
                LocalDateTime.of(2026, 5, 16, 10, 30));
    }

    private record BlankCacheKeyRequest(
            String cacheKey,
            String content
    ) {
        private BlankCacheKeyRequest() {
            this("", "질문입니다.");
        }
    }
}
