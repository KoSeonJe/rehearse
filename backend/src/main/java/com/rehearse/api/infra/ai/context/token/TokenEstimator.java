package com.rehearse.api.infra.ai.context.token;

import com.rehearse.api.infra.ai.dto.ChatMessage;
import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class TokenEstimator {

    // 4 chars ≈ 1 token (OpenAI cl100k_base heuristic for English/Korean mixed text)
    private static final int CHARS_PER_TOKEN = 4;

    public int estimate(String text) {
        if (text == null || text.isEmpty()) {
            return 0;
        }
        return Math.max(1, text.length() / CHARS_PER_TOKEN);
    }

    public int estimate(List<ChatMessage> messages) {
        if (messages == null || messages.isEmpty()) {
            return 0;
        }
        return messages.stream()
                .mapToInt(msg -> estimate(msg.content()))
                .sum();
    }
}
