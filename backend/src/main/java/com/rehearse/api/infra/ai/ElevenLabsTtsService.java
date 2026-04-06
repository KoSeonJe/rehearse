package com.rehearse.api.infra.ai;

import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.exception.ElevenLabsErrorCode;
import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnExpression;
import org.springframework.boot.web.client.ClientHttpRequestFactories;
import org.springframework.boot.web.client.ClientHttpRequestFactorySettings;
import org.springframework.http.*;
import org.springframework.stereotype.Component;
import org.springframework.web.client.RestClientException;
import org.springframework.web.client.RestTemplate;

import java.time.Duration;
import java.util.Map;

@Slf4j
@Component
@ConditionalOnExpression("!'${elevenlabs.api-key:}'.isEmpty()")
public class ElevenLabsTtsService {

    private final String apiKey;
    private final String voiceId;
    private final String modelId;
    private final RestTemplate restTemplate;

    public ElevenLabsTtsService(
            @Value("${elevenlabs.api-key:}") String apiKey,
            @Value("${elevenlabs.voice-id:cgSgspJ2msm6clMCkdW9}") String voiceId,
            @Value("${elevenlabs.model-id:eleven_multilingual_v2}") String modelId) {
        this.apiKey = apiKey;
        this.voiceId = voiceId;
        this.modelId = modelId;

        ClientHttpRequestFactorySettings settings = ClientHttpRequestFactorySettings.DEFAULTS
                .withConnectTimeout(Duration.ofSeconds(5))
                .withReadTimeout(Duration.ofSeconds(30));
        this.restTemplate = new RestTemplate(ClientHttpRequestFactories.get(settings));
    }

    @PostConstruct
    void init() {
        log.info("=== ElevenLabsTtsService 활성화: ElevenLabs TTS 사용 (voiceId={}) ===", voiceId);
    }

    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(ElevenLabsErrorCode.EMPTY_TEXT);
        }

        String url = "https://api.elevenlabs.io/v1/text-to-speech/" + voiceId;

        HttpHeaders headers = new HttpHeaders();
        headers.set("xi-api-key", apiKey);
        headers.setContentType(MediaType.APPLICATION_JSON);
        headers.setAccept(java.util.List.of(MediaType.parseMediaType("audio/mpeg")));

        Map<String, Object> body = Map.of(
                "text", text,
                "model_id", modelId,
                "voice_settings", Map.of(
                        "stability", 0.5,
                        "similarity_boost", 0.75
                )
        );

        try {
            ResponseEntity<byte[]> response = restTemplate.exchange(
                    url, HttpMethod.POST, new HttpEntity<>(body, headers), byte[].class);

            log.debug("ElevenLabs TTS 완료: {}자 → {}bytes", text.length(),
                    response.getBody() != null ? response.getBody().length : 0);
            return response.getBody() != null ? response.getBody() : new byte[0];
        } catch (RestClientException e) {
            log.error("ElevenLabs API 호출 실패", e);
            throw new BusinessException(ElevenLabsErrorCode.API_CALL_FAILED);
        }
    }
}