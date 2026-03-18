package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.stereotype.Service;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;

@Slf4j
@Service
public class WhisperService {

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final RestTemplate restTemplate;

    public WhisperService(@Value("${openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;
        this.restTemplate = new RestTemplate();
    }

    public String transcribe(MultipartFile audioFile) {
        if (apiKey == null || apiKey.isBlank()) {
            throw new BusinessException(HttpStatus.SERVICE_UNAVAILABLE, "WHISPER_001", "Whisper API 키가 설정되지 않았습니다.");
        }

        try {
            HttpHeaders headers = new HttpHeaders();
            headers.setBearerAuth(apiKey);
            headers.setContentType(MediaType.MULTIPART_FORM_DATA);

            MultiValueMap<String, Object> body = new LinkedMultiValueMap<>();
            body.add("file", new ByteArrayResource(audioFile.getBytes()) {
                @Override
                public String getFilename() {
                    String original = audioFile.getOriginalFilename();
                    return original != null ? original : "audio.webm";
                }
            });
            body.add("model", "whisper-1");
            body.add("language", "ko");
            body.add("response_format", "text");

            HttpEntity<MultiValueMap<String, Object>> request = new HttpEntity<>(body, headers);

            ResponseEntity<String> response = restTemplate.exchange(
                    WHISPER_API_URL, HttpMethod.POST, request, String.class);

            String transcript = response.getBody();
            log.info("Whisper STT 완료: {}자", transcript != null ? transcript.length() : 0);
            return transcript != null ? transcript.trim() : "";
        } catch (IOException e) {
            log.error("오디오 파일 읽기 실패", e);
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "WHISPER_002", "오디오 파일 읽기에 실패했습니다.");
        } catch (RestClientException e) {
            log.error("Whisper API 호출 실패", e);
            throw new BusinessException(HttpStatus.BAD_GATEWAY, "WHISPER_003", "음성 인식 API 호출에 실패했습니다.");
        }
    }
}
