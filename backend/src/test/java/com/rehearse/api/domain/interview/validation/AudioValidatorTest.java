package com.rehearse.api.domain.interview.validation;

import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.global.config.InterviewProperties;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.multipart.MultipartFile;

import java.util.Set;

import static org.assertj.core.api.Assertions.assertThatCode;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("AudioValidator - 오디오 입력 검증")
class AudioValidatorTest {

    private AudioValidator validator;

    @BeforeEach
    void setUp() {
        InterviewProperties properties = new InterviewProperties(
                new InterviewProperties.Retry(5, 30),
                new InterviewProperties.Audio(
                        1024L,
                        300,
                        Set.of("audio/webm", "audio/mp4", "audio/mpeg", "audio/wav")
                )
        );
        validator = new AudioValidator(properties);
    }

    @Nested
    @DisplayName("MIME 화이트리스트")
    class MimeWhitelist {

        @Test
        @DisplayName("허용되지 않는 MIME 은 AUDIO_MIME_NOT_ALLOWED 로 거부된다")
        void rejectsDisallowedMime() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "evil.exe", "application/octet-stream", new byte[]{0, 0, 0, 0});

            assertThatThrownBy(() -> validator.validate(file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(InterviewErrorCode.AUDIO_MIME_NOT_ALLOWED.getCode());
        }

        @Test
        @DisplayName("Content-Type 누락은 AUDIO_MIME_NOT_ALLOWED 로 거부된다")
        void rejectsMissingContentType() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "no-mime", null, validWebmHeader());

            assertThatThrownBy(() -> validator.validate(file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(InterviewErrorCode.AUDIO_MIME_NOT_ALLOWED.getCode());
        }
    }

    @Nested
    @DisplayName("최대 용량")
    class MaxBytes {

        @Test
        @DisplayName("maxBytes 초과 파일은 AUDIO_DURATION_EXCEEDED 로 거부된다")
        void rejectsOverSizedFile() {
            byte[] payload = new byte[2048];
            byte[] header = validWebmHeader();
            System.arraycopy(header, 0, payload, 0, header.length);
            MultipartFile file = new MockMultipartFile(
                    "audio", "big.webm", "audio/webm", payload);

            assertThatThrownBy(() -> validator.validate(file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(InterviewErrorCode.AUDIO_DURATION_EXCEEDED.getCode());
        }
    }

    @Nested
    @DisplayName("매직바이트 검증")
    class MagicBytes {

        @Test
        @DisplayName("MIME 은 audio/webm 인데 헤더가 다르면 AUDIO_MAGIC_BYTE_MISMATCH 로 거부된다")
        void rejectsWebmMimeWithMismatchedHeader() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "fake.webm", "audio/webm",
                    new byte[]{0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

            assertThatThrownBy(() -> validator.validate(file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(InterviewErrorCode.AUDIO_MAGIC_BYTE_MISMATCH.getCode());
        }

        @Test
        @DisplayName("audio/mp4 인데 ftyp 박스 시그니처가 없으면 거부된다")
        void rejectsMp4MimeWithoutFtyp() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "fake.mp4", "audio/mp4",
                    new byte[]{0x00, 0x00, 0x00, 0x18, 'X', 'X', 'X', 'X', 0x00, 0x00, 0x00, 0x00});

            assertThatThrownBy(() -> validator.validate(file))
                    .isInstanceOf(BusinessException.class)
                    .extracting(e -> ((BusinessException) e).getCode())
                    .isEqualTo(InterviewErrorCode.AUDIO_MAGIC_BYTE_MISMATCH.getCode());
        }

        @Test
        @DisplayName("정상 audio/webm EBML 헤더는 통과한다")
        void acceptsValidWebmHeader() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "valid.webm", "audio/webm", validWebmHeader());

            assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상 audio/mp4 ftyp 박스는 통과한다")
        void acceptsValidMp4Header() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "valid.mp4", "audio/mp4",
                    new byte[]{0x00, 0x00, 0x00, 0x18, 'f', 't', 'y', 'p', 'i', 's', 'o', 'm'});

            assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상 audio/wav RIFF/WAVE 헤더는 통과한다")
        void acceptsValidWavHeader() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "valid.wav", "audio/wav",
                    new byte[]{'R', 'I', 'F', 'F', 0x24, 0x00, 0x00, 0x00, 'W', 'A', 'V', 'E'});

            assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("정상 audio/mpeg ID3 헤더는 통과한다")
        void acceptsValidMpegId3Header() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "valid.mp3", "audio/mpeg",
                    new byte[]{'I', 'D', '3', 0x03, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

            assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        }
    }

    @Nested
    @DisplayName("빈 파일 처리")
    class EmptyFile {

        @Test
        @DisplayName("null 파일은 통과한다 (controller 가 옵셔널 처리)")
        void passesWhenFileNull() {
            assertThatCode(() -> validator.validate(null)).doesNotThrowAnyException();
        }

        @Test
        @DisplayName("빈 파일은 통과한다")
        void passesWhenFileEmpty() {
            MultipartFile file = new MockMultipartFile(
                    "audio", "empty", "audio/webm", new byte[0]);

            assertThatCode(() -> validator.validate(file)).doesNotThrowAnyException();
        }
    }

    private static byte[] validWebmHeader() {
        return new byte[]{
                (byte) 0x1A, (byte) 0x45, (byte) 0xDF, (byte) 0xA3,
                0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
    }
}
