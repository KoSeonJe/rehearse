package com.rehearse.api.domain.reviewbookmark.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkExistsResponse;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkStatusFilter;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkListItem;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkResponse;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;
import com.rehearse.api.domain.reviewbookmark.service.ReviewBookmarkQueryService;
import com.rehearse.api.domain.reviewbookmark.service.ReviewBookmarkService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.support.WithMockUserId;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.time.LocalDateTime;
import java.util.List;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(value = ReviewBookmarkController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
class ReviewBookmarkControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private ReviewBookmarkService reviewBookmarkService;

    @MockitoBean
    private ReviewBookmarkQueryService reviewBookmarkQueryService;

    @Nested
    @DisplayName("POST /api/v1/review-bookmarks")
    class Create {

        @Test
        @WithMockUserId
        @DisplayName("정상 생성 시 201 반환")
        void create_success_returns201() throws Exception {
            // given
            ReviewBookmarkResponse response = new ReviewBookmarkResponse(100L, 10L, null, LocalDateTime.now());
            given(reviewBookmarkService.create(eq(1L), any())).willReturn(response);

            String body = """
                    { "timestampFeedbackId": 10 }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/review-bookmarks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isCreated())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100));
        }

        @Test
        @WithMockUserId
        @DisplayName("이미 존재하면 409 반환")
        void create_alreadyExists_returns409() throws Exception {
            // given
            given(reviewBookmarkService.create(eq(1L), any()))
                    .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS));

            String body = """
                    { "timestampFeedbackId": 10 }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/review-bookmarks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isConflict());
        }

        @Test
        @WithMockUserId
        @DisplayName("피드백 미존재 시 404 반환")
        void create_tsfNotFound_returns404() throws Exception {
            // given
            given(reviewBookmarkService.create(eq(1L), any()))
                    .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.TIMESTAMP_FEEDBACK_NOT_FOUND));

            String body = """
                    { "timestampFeedbackId": 999 }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/review-bookmarks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 호출하면 401")
        void create_withoutAuth_returns401() throws Exception {
            // given
            String body = """
                    { "timestampFeedbackId": 10 }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/review-bookmarks")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("DELETE /api/v1/review-bookmarks/{id}")
    class Delete {

        @Test
        @WithMockUserId
        @DisplayName("정상 삭제 시 204 반환")
        void delete_success_returns204() throws Exception {
            // given
            willDoNothing().given(reviewBookmarkService).delete(1L, 100L);

            // when & then
            mockMvc.perform(delete("/api/v1/review-bookmarks/100"))
                    .andExpect(status().isNoContent());
        }

        @Test
        @WithMockUserId
        @DisplayName("타인 소유 시 403 반환")
        void delete_forbidden_returns403() throws Exception {
            // given
            willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS))
                    .given(reviewBookmarkService).delete(1L, 100L);

            // when & then
            mockMvc.perform(delete("/api/v1/review-bookmarks/100"))
                    .andExpect(status().isForbidden());
        }

        @Test
        @WithMockUserId
        @DisplayName("미존재 시 404 반환")
        void delete_notFound_returns404() throws Exception {
            // given
            willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND))
                    .given(reviewBookmarkService).delete(1L, 999L);

            // when & then
            mockMvc.perform(delete("/api/v1/review-bookmarks/999"))
                    .andExpect(status().isNotFound());
        }

        @Test
        @DisplayName("인증 없이 호출하면 401")
        void delete_withoutAuth_returns401() throws Exception {
            // when & then
            mockMvc.perform(delete("/api/v1/review-bookmarks/100"))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/review-bookmarks")
    class List_ {

        @Test
        @WithMockUserId
        @DisplayName("목록 조회 시 200과 items/total 반환")
        void list_success_returns200() throws Exception {
            // given
            ReviewBookmarkListItem item = new ReviewBookmarkListItem(
                    1L, 10L, "자기소개를 해주세요.", "모범 답변", "안녕하세요.",
                    "더 자세히", "BEHAVIORAL", "BACKEND", "시니어 엔지니어",
                    LocalDateTime.now(), LocalDateTime.now(), null);
            given(reviewBookmarkQueryService.listByUser(1L, BookmarkStatusFilter.ALL)).willReturn(java.util.List.of(item));

            // when & then
            mockMvc.perform(get("/api/v1/review-bookmarks")
                            .param("status", "all"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.total").value(1))
                    .andExpect(jsonPath("$.data.items[0].id").value(1));
        }

        @Test
        @WithMockUserId
        @DisplayName("status 미입력 시 기본값 all 적용")
        void list_defaultStatus_all() throws Exception {
            // given
            given(reviewBookmarkQueryService.listByUser(1L, BookmarkStatusFilter.ALL)).willReturn(java.util.List.of());

            // when & then
            mockMvc.perform(get("/api/v1/review-bookmarks"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.total").value(0));
        }

        @Test
        @WithMockUserId
        @DisplayName("유효하지 않은 status 값이면 400 반환")
        void list_invalidStatus_returns400() throws Exception {
            // given
            given(reviewBookmarkQueryService.listByUser(eq(1L), any()))
                    .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.INVALID_STATUS_FILTER));

            // when & then
            mockMvc.perform(get("/api/v1/review-bookmarks")
                            .param("status", "invalid_value"))
                    .andExpect(status().isBadRequest());
        }
    }

    @Nested
    @DisplayName("PATCH /api/v1/review-bookmarks/{id}/status")
    class UpdateStatus {

        @Test
        @WithMockUserId
        @DisplayName("정상 수정 시 200 반환")
        void updateStatus_success_returns200() throws Exception {
            // given
            ReviewBookmarkResponse response = new ReviewBookmarkResponse(100L, 10L, LocalDateTime.now(), LocalDateTime.now());
            given(reviewBookmarkService.updateStatus(eq(1L), eq(100L), any())).willReturn(response);

            String body = """
                    { "resolved": true }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/review-bookmarks/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.resolvedAt").isNotEmpty());
        }

        @Test
        @WithMockUserId
        @DisplayName("resolved=false 시 resolvedAt null 반환")
        void updateStatus_resolvedFalse_returnsResolvedAtNull() throws Exception {
            // given
            ReviewBookmarkResponse response = new ReviewBookmarkResponse(100L, 10L, null, LocalDateTime.now());
            given(reviewBookmarkService.updateStatus(eq(1L), eq(100L), any())).willReturn(response);

            String body = """
                    { "resolved": false }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/review-bookmarks/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.id").value(100))
                    .andExpect(jsonPath("$.data.resolvedAt").doesNotExist());
        }

        @Test
        @WithMockUserId
        @DisplayName("타인 소유 시 403 반환")
        void updateStatus_forbidden_returns403() throws Exception {
            // given
            given(reviewBookmarkService.updateStatus(eq(1L), eq(100L), any()))
                    .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS));

            String body = """
                    { "resolved": true }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/review-bookmarks/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isForbidden());
        }

        @Test
        @DisplayName("인증 없이 호출하면 401")
        void updateStatus_withoutAuth_returns401() throws Exception {
            // given
            String body = """
                    { "resolved": true }
                    """;

            // when & then
            mockMvc.perform(patch("/api/v1/review-bookmarks/100/status")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(body))
                    .andExpect(status().isUnauthorized());
        }
    }

    @Nested
    @DisplayName("GET /api/v1/review-bookmarks/exists")
    class Exists {

        @Test
        @WithMockUserId
        @DisplayName("정상 조회 시 200과 items 반환")
        void exists_success_returns200() throws Exception {
            // given
            BookmarkExistsResponse response = new BookmarkExistsResponse(
                    List.of(new BookmarkIdPair(10L, 100L)));
            given(reviewBookmarkQueryService.findBookmarkPairs(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/review-bookmarks/exists")
                            .param("timestampFeedbackIds", "10", "20"))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.success").value(true))
                    .andExpect(jsonPath("$.data.items[0].timestampFeedbackId").value(10))
                    .andExpect(jsonPath("$.data.items[0].bookmarkId").value(100));
        }

        @Test
        @WithMockUserId
        @DisplayName("빈 ID 목록 조회 시 빈 items 반환")
        void exists_emptyIds_returnsEmptyItems() throws Exception {
            // given
            BookmarkExistsResponse response = new BookmarkExistsResponse(List.of());
            given(reviewBookmarkQueryService.findBookmarkPairs(eq(1L), any())).willReturn(response);

            // when & then
            mockMvc.perform(get("/api/v1/review-bookmarks/exists")
                            .param("timestampFeedbackIds", ""))
                    .andExpect(status().isOk())
                    .andExpect(jsonPath("$.data.items").isEmpty());
        }
    }
}
