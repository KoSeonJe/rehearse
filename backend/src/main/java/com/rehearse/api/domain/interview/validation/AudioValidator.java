package com.rehearse.api.domain.interview.validation;

import com.rehearse.api.domain.interview.exception.InterviewErrorCode;
import com.rehearse.api.global.config.InterviewProperties;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.io.InputStream;
import java.util.Set;

@Slf4j
@Component
@RequiredArgsConstructor
public class AudioValidator {

    private static final int HEADER_READ_LENGTH = 12;

    private final InterviewProperties interviewProperties;

    public void validate(MultipartFile file) {
        if (file == null || file.isEmpty()) {
            return;
        }

        InterviewProperties.Audio audio = interviewProperties.audio();
        Set<String> mimeWhitelist = audio.mimeWhitelist();

        String contentType = file.getContentType();
        if (contentType == null || !mimeWhitelist.contains(contentType)) {
            log.warn("오디오 MIME 거부 — contentType={}, size={}", contentType, file.getSize());
            throw new BusinessException(InterviewErrorCode.AUDIO_MIME_NOT_ALLOWED);
        }

        if (file.getSize() > audio.maxBytes()) {
            log.warn("오디오 용량 초과 — size={}, maxBytes={}", file.getSize(), audio.maxBytes());
            throw new BusinessException(InterviewErrorCode.AUDIO_DURATION_EXCEEDED);
        }

        byte[] header;
        try (InputStream in = file.getInputStream()) {
            header = in.readNBytes(HEADER_READ_LENGTH);
        } catch (IOException e) {
            log.warn("오디오 헤더 read 실패 — contentType={}", contentType, e);
            throw new BusinessException(InterviewErrorCode.AUDIO_MAGIC_BYTE_MISMATCH);
        }

        if (!matchesMagicBytes(header, contentType)) {
            log.warn("오디오 매직바이트 불일치 — contentType={}, headerLen={}", contentType, header.length);
            throw new BusinessException(InterviewErrorCode.AUDIO_MAGIC_BYTE_MISMATCH);
        }
    }

    public int getMaxDurationSeconds() {
        return interviewProperties.audio().maxDurationSeconds();
    }

    private boolean matchesMagicBytes(byte[] header, String contentType) {
        return switch (contentType) {
            case "audio/webm" -> isWebm(header);
            case "audio/mp4" -> isMp4(header);
            case "audio/mpeg" -> isMpeg(header);
            case "audio/wav" -> isWav(header);
            default -> false;
        };
    }

    private boolean isWebm(byte[] h) {
        return h.length >= 4
                && (h[0] & 0xFF) == 0x1A
                && (h[1] & 0xFF) == 0x45
                && (h[2] & 0xFF) == 0xDF
                && (h[3] & 0xFF) == 0xA3;
    }

    private boolean isMp4(byte[] h) {
        return h.length >= 8
                && h[4] == 'f' && h[5] == 't' && h[6] == 'y' && h[7] == 'p';
    }

    private boolean isMpeg(byte[] h) {
        if (h.length < 3) {
            return false;
        }
        if (h[0] == 'I' && h[1] == 'D' && h[2] == '3') {
            return true;
        }
        return (h[0] & 0xFF) == 0xFF && (h[1] & 0xE0) == 0xE0;
    }

    private boolean isWav(byte[] h) {
        return h.length >= 12
                && h[0] == 'R' && h[1] == 'I' && h[2] == 'F' && h[3] == 'F'
                && h[8] == 'W' && h[9] == 'A' && h[10] == 'V' && h[11] == 'E';
    }
}
