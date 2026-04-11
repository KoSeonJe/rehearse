package com.rehearse.api.domain.tts.service;

public interface TtsService {
    byte[] synthesize(String text);
}
