package com.rehearse.api.domain.file.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.exception.FileErrorCode;
import com.rehearse.api.domain.file.service.InternalFileService;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import com.rehearse.api.global.config.TestSecurityConfig;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willDoNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(InternalFileController.class)
@Import(TestSecurityConfig.class)
class InternalFileControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private InternalFileService internalFileService;

    // InternalApiKeyFilter는 @Value("${internal.api-key:}") 기본값이 빈 문자열이므로
    // api-key 미설정 환경에서는 필터를 통과한다 (warnIfKeyNotConfigured 경고만 출력)

    // ─────────────────────────────────────────────────────────────
    // PUT /api/internal/files/{id}/status
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("PUT /api/internal/files/{id}/status - 유효한 요청이면 200을 반환한다")
    void updateFileStatus_성공_200반환() throws Exception {
        // given
        willDoNothing().given(internalFileService).updateFileStatus(eq(1L), any());

        String body = """
                {
                    "status": "UPLOADED"
                }
                """;

        // when & then
        mockMvc.perform(put("/api/internal/files/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true));
    }

    @Test
    @DisplayName("PUT /api/internal/files/{id}/status - status 필드 누락 시 400을 반환한다")
    void updateFileStatus_status누락시_400반환() throws Exception {
        // given
        String body = "{}";

        // when & then
        mockMvc.perform(put("/api/internal/files/1/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("VALIDATION_ERROR"));
    }

    // ─────────────────────────────────────────────────────────────
    // GET /api/internal/files/by-s3-key
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("GET /api/internal/files/by-s3-key - 존재하는 S3 키이면 200과 파일 정보를 반환한다")
    void findByS3Key_성공_200반환() throws Exception {
        // given
        FileMetadata file = createFileMetadata(10L, "uploads/video.webm", FileStatus.UPLOADED);
        given(internalFileService.findByS3Key("uploads/video.webm")).willReturn(file);

        // when & then
        mockMvc.perform(get("/api/internal/files/by-s3-key")
                        .param("key", "uploads/video.webm"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.id").value(10))
                .andExpect(jsonPath("$.data.s3Key").value("uploads/video.webm"))
                .andExpect(jsonPath("$.data.status").value("UPLOADED"))
                .andExpect(jsonPath("$.data.fileType").value("VIDEO"));
    }

    @Test
    @DisplayName("GET /api/internal/files/by-s3-key - 존재하지 않는 S3 키이면 404를 반환한다")
    void findByS3Key_미존재시_404반환() throws Exception {
        // given
        given(internalFileService.findByS3Key("no/such/key.webm"))
                .willThrow(new BusinessException(FileErrorCode.S3_KEY_NOT_FOUND));

        // when & then
        mockMvc.perform(get("/api/internal/files/by-s3-key")
                        .param("key", "no/such/key.webm"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.code").value("FILE_003"));
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private FileMetadata createFileMetadata(Long id, String s3Key, FileStatus targetStatus) {
        FileMetadata file = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key(s3Key)
                .bucket("rehearse-bucket")
                .contentType("video/webm")
                .build();
        ReflectionTestUtils.setField(file, "id", id);

        if (targetStatus == FileStatus.UPLOADED) {
            file.updateStatus(FileStatus.UPLOADED);
        }
        return file;
    }
}
