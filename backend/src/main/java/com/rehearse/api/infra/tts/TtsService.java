package com.rehearse.api.infra.tts;

public interface TtsService {
    byte[] synthesize(String text);
}
