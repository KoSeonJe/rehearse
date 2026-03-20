package com.rehearse.api.infra.ai;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;

import static org.assertj.core.api.Assertions.assertThat;

class MockSttServiceTest {

    private final MockSttService mockSttService = new MockSttService();

    @Test
    @DisplayName("Mock STT는 항상 비어있지 않은 텍스트를 반환한다")
    void transcribe_returnsMockText() {
        // given
        MockMultipartFile audioFile = new MockMultipartFile(
                "audio", "test.webm", "audio/webm", new byte[]{1, 2, 3});

        // when
        String result = mockSttService.transcribe(audioFile);

        // then
        assertThat(result).isNotNull().isNotBlank();
    }

    @Test
    @DisplayName("Mock STT는 파일 크기와 무관하게 동일한 텍스트를 반환한다")
    void transcribe_consistentResult() {
        // given
        MockMultipartFile smallFile = new MockMultipartFile(
                "audio", "small.webm", "audio/webm", new byte[]{1});
        MockMultipartFile largeFile = new MockMultipartFile(
                "audio", "large.webm", "audio/webm", new byte[1024]);

        // when
        String result1 = mockSttService.transcribe(smallFile);
        String result2 = mockSttService.transcribe(largeFile);

        // then
        assertThat(result1).isEqualTo(result2);
    }
}
