package com.rehearse.api.domain.tts.controller;

import com.rehearse.api.domain.tts.dto.TtsRequest;
import com.rehearse.api.domain.tts.service.TtsService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/tts")
@RequiredArgsConstructor
@ConditionalOnProperty(prefix = "google.tts", name = "enabled", havingValue = "true")
public class TtsController {

    private final TtsService ttsService;

    @PostMapping
    public ResponseEntity<byte[]> synthesize(@Valid @RequestBody TtsRequest request) {
        byte[] audio = ttsService.synthesize(request.text());

        HttpHeaders headers = new HttpHeaders();
        headers.setContentType(MediaType.parseMediaType("audio/mpeg"));
        headers.setContentLength(audio.length);

        return ResponseEntity.ok()
                .headers(headers)
                .body(audio);
    }
}
