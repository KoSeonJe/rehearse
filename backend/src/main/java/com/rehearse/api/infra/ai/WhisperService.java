package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.WhisperErrorCode;
import io.github.resilience4j.ratelimiter.annotation.RateLimiter;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.core.io.ByteArrayResource;
import org.springframework.http.*;
import org.springframework.http.client.SimpleClientHttpRequestFactory;
import org.springframework.stereotype.Component;
import org.springframework.util.LinkedMultiValueMap;
import org.springframework.util.MultiValueMap;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;
import org.springframework.web.multipart.MultipartFile;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.time.Duration;

@Slf4j
@Component
@ConditionalOnExpression("!'${openai.api-key:}'.isEmpty()")
public class WhisperService implements SttService {

    private static final String WHISPER_API_URL = "https://api.openai.com/v1/audio/transcriptions";

    private final String apiKey;
    private final RestTemplate restTemplate;

    public WhisperService(@Value("${openai.api-key:}") String apiKey) {
        this.apiKey = apiKey;

        SimpleClientHttpRequestFactory factory = new SimpleClientHttpRequestFactory();
        factory.setConnectTimeout(Duration.ofSeconds(5));
        factory.setReadTimeout(Duration.ofSeconds(60));
        this.restTemplate = new RestTemplate(factory);
    }

    @PostConstruct
    void init() {
        log.info("=== WhisperService 활성화: OpenAI Whisper STT 사용 ===");
    }

    @Override
    @RateLimiter(name = "whisper-api")
    public String transcribe(MultipartFile audioFile) {
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
            throw new BusinessException(WhisperErrorCode.FILE_READ_FAILED);
        } catch (RestClientException e) {
            log.error("Whisper API 호출 실패", e);
            throw new BusinessException(WhisperErrorCode.API_CALL_FAILED);
        }
    }
}
