package com.rehearse.api.domain.tts.controller;

import com.rehearse.api.domain.tts.service.TtsService;
import com.rehearse.api.global.config.InternalApiKeyFilter;
import com.rehearse.api.global.config.TestSecurityConfig;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.global.security.config.SecurityConfig;
import com.rehearse.api.global.support.WithMockUserId;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.context.annotation.ComponentScan;
import org.springframework.context.annotation.FilterType;
import org.springframework.context.annotation.Import;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(value = TtsController.class,
        excludeFilters = @ComponentScan.Filter(type = FilterType.ASSIGNABLE_TYPE,
                classes = {SecurityConfig.class, InternalApiKeyFilter.class}))
@Import(TestSecurityConfig.class)
@TestPropertySource(properties = "google.tts.enabled=true")
@WithMockUserId
class TtsControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockitoBean
    private TtsService ttsService;

    @Nested
    @DisplayName("POST /api/v1/tts")
    class Synthesize {

        @Test
        @DisplayName("POST /api/v1/tts - 정상 요청 시 200과 audio/mpeg Content-Type 반환")
        void synthesize_validRequest_returns200WithAudioMpeg() throws Exception {
            // given
            byte[] audioBytes = "fake-audio-data".getBytes();
            given(ttsService.synthesize("안녕하세요, 테스트입니다.")).willReturn(audioBytes);

            String requestBody = """
                    {
                        "text": "안녕하세요, 테스트입니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk())
                    .andExpect(header().string("Content-Type", "audio/mpeg"))
                    .andExpect(header().longValue("Content-Length", audioBytes.length))
                    .andExpect(content().bytes(audioBytes));
        }

        @Test
        @DisplayName("POST /api/v1/tts - 정상 요청 시 TtsService.synthesize가 호출된다")
        void synthesize_validRequest_callsService() throws Exception {
            // given
            byte[] audioBytes = new byte[]{1, 2, 3};
            given(ttsService.synthesize(anyString())).willReturn(audioBytes);

            String requestBody = """
                    {
                        "text": "서비스 호출 확인용 텍스트입니다."
                    }
                    """;

            // when
            mockMvc.perform(post("/api/v1/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isOk());

            // then
            then(ttsService).should().synthesize("서비스 호출 확인용 텍스트입니다.");
        }

        @Test
        @DisplayName("POST /api/v1/tts - text가 blank이면 400 반환")
        void synthesize_blankText_returns400() throws Exception {
            // given
            String requestBody = """
                    {
                        "text": ""
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/tts - text가 1000자 초과이면 400 반환")
        void synthesize_textExceeds1000Chars_returns400() throws Exception {
            // given
            String longText = "a".repeat(1001);
            String requestBody = String.format("""
                    {
                        "text": "%s"
                    }
                    """, longText);

            // when & then
            mockMvc.perform(post("/api/v1/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isBadRequest());
        }

        @Test
        @DisplayName("POST /api/v1/tts - 서비스에서 예외 발생 시 500 반환")
        void synthesize_serviceThrowsException_returns500() throws Exception {
            // given
            given(ttsService.synthesize(anyString()))
                    .willThrow(new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "TTS_001", "TTS 변환에 실패했습니다."));

            String requestBody = """
                    {
                        "text": "예외 발생 테스트입니다."
                    }
                    """;

            // when & then
            mockMvc.perform(post("/api/v1/tts")
                            .contentType(MediaType.APPLICATION_JSON)
                            .content(requestBody))
                    .andExpect(status().isInternalServerError());
        }
    }
}
