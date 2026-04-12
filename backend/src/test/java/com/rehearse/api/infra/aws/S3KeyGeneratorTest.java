package com.rehearse.api.infra.aws;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.HashSet;
import java.util.Set;

import static org.assertj.core.api.Assertions.*;

class S3KeyGeneratorTest {

    @Test
    @DisplayName("generateRawVideoKey: 고정 시각에서 키가 올바른 형식으로 생성된다")
    void generateRawVideoKey_format() {
        // given
        Clock fixed = Clock.fixed(Instant.parse("2026-04-12T14:30:00Z"), ZoneOffset.UTC);
        S3KeyGenerator generator = new S3KeyGenerator(fixed);

        // when
        String key = generator.generateRawVideoKey(123L, 456L);

        // then
        assertThat(key).matches("^interviews/raw/2026/04/12/123/456/[a-f0-9]{12}\\.webm$");
    }

    @Test
    @DisplayName("generateRawVideoKey: UTC 자정에도 날짜 파티션이 올바르게 생성된다")
    void generateRawVideoKey_utcMidnight() {
        // given
        Clock fixed = Clock.fixed(Instant.parse("2026-04-12T00:00:00Z"), ZoneOffset.UTC);
        S3KeyGenerator generator = new S3KeyGenerator(fixed);

        // when
        String key = generator.generateRawVideoKey(1L, 1L);

        // then
        assertThat(key).startsWith("interviews/raw/2026/04/12/");
    }

    @Test
    @DisplayName("generateRawVideoKey: 100회 호출 시 모두 고유한 키가 생성된다")
    void generateRawVideoKey_uniqueUuid() {
        // given
        S3KeyGenerator generator = new S3KeyGenerator();
        Set<String> keys = new HashSet<>();

        // when
        for (int i = 0; i < 100; i++) {
            keys.add(generator.generateRawVideoKey(1L, 1L));
        }

        // then
        assertThat(keys).hasSize(100);
    }

    @Test
    @DisplayName("generateRawVideoKey: 음수 interviewId 전달 시 IllegalArgumentException이 발생한다")
    void generateRawVideoKey_negative_throws() {
        // given
        S3KeyGenerator generator = new S3KeyGenerator();

        // when & then
        assertThatThrownBy(() -> generator.generateRawVideoKey(-1L, 1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("interviewId");

        assertThatThrownBy(() -> generator.generateRawVideoKey(1L, -1L))
                .isInstanceOf(IllegalArgumentException.class)
                .hasMessageContaining("questionSetId");
    }
}
