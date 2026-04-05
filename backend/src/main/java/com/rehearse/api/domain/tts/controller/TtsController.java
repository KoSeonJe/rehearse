package com.rehearse.api.domain.tts.controller;

import com.rehearse.api.domain.tts.dto.TtsRequest;
import com.rehearse.api.infra.ai.ElevenLabsTtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
@ConditionalOnBean(ElevenLabsTtsService.class)
public class TtsController {

    private final ElevenLabsTtsService elevenLabsTtsService;

    @PostMapping
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody TtsRequest request) {
        byte[] audio = elevenLabsTtsService.synthesize(request.text());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(audio.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(audio);
    }
}
