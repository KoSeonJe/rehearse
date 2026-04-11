package com.rehearse.api.infra.google;

import com.google.cloud.texttospeech.v1.AudioConfig;
import com.google.cloud.texttospeech.v1.AudioEncoding;
import com.google.cloud.texttospeech.v1.SynthesisInput;
import com.google.cloud.texttospeech.v1.SynthesizeSpeechResponse;
import com.google.cloud.texttospeech.v1.TextToSpeechClient;
import com.google.cloud.texttospeech.v1.VoiceSelectionParams;
import com.google.protobuf.ByteString;
import com.rehearse.api.domain.tts.exception.TtsErrorCode;
import com.rehearse.api.domain.tts.service.TtsService;
import com.rehearse.api.global.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnBean;
import org.springframework.stereotype.Component;

@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnBean(TextToSpeechClient.class)
public class GoogleCloudTtsService implements TtsService {

    private final TextToSpeechClient ttsClient;

    @Value("${google.tts.voice-name:ko-KR-Chirp3-HD-Schedar}")
    private String voiceName;

    @Value("${google.tts.language-code:ko-KR}")
    private String languageCode;

    @Override
    public byte[] synthesize(String text) {
        if (text == null || text.isBlank()) {
            throw new BusinessException(TtsErrorCode.EMPTY_TEXT);
        }

        SynthesisInput input = SynthesisInput.newBuilder()
                .setText(text)
                .build();

        VoiceSelectionParams voice = VoiceSelectionParams.newBuilder()
                .setLanguageCode(languageCode)
                .setName(voiceName)
                .build();

        AudioConfig audioConfig = AudioConfig.newBuilder()
                .setAudioEncoding(AudioEncoding.MP3)
                .build();

        try {
            SynthesizeSpeechResponse response =
                    ttsClient.synthesizeSpeech(input, voice, audioConfig);
            ByteString audioContent = response.getAudioContent();
            log.debug("Google Cloud TTS 완료: {}자 → {}bytes",
                    text.length(), audioContent.size());
            return audioContent.toByteArray();
        } catch (Exception e) {
            log.error("Google Cloud TTS API 호출 실패", e);
            throw new BusinessException(TtsErrorCode.API_CALL_FAILED);
        }
    }
}
