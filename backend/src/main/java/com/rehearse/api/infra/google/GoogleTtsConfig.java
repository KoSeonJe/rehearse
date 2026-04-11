package com.rehearse.api.infra.google;

import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.io.IOException;

@Configuration
@ConditionalOnProperty(prefix = "google.tts", name = "enabled", havingValue = "true")
public class GoogleTtsConfig {

    @Bean(destroyMethod = "close")
    public TextToSpeechClient textToSpeechClient() throws IOException {
        return TextToSpeechClient.create();
    }
}
