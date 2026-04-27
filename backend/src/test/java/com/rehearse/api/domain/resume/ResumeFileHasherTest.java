package com.rehearse.api.domain.resume;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("ResumeFileHasher - SHA-256 해시 계산")
class ResumeFileHasherTest {

    private final ResumeFileHasher hasher = new ResumeFileHasher();

    @Test
    @DisplayName("hash_returns_known_sha256_hex_for_given_input")
    void hash_returns_known_sha256_hex_for_given_input() {
        byte[] input = "hello".getBytes();
        // SHA-256("hello") = 2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824
        String expected = "2cf24dba5fb0a30e26e83b2ac5b9e29e1b161e5c1fa7425e73043362938b9824";

        String result = hasher.hash(input);

        assertThat(result).isEqualTo(expected);
    }

    @Test
    @DisplayName("hash_returns_64_char_hex_string_for_any_input")
    void hash_returns_64_char_hex_string_for_any_input() {
        byte[] input = "Java 백엔드 이력서 내용".getBytes();

        String result = hasher.hash(input);

        assertThat(result).hasSize(64);
        assertThat(result).matches("[0-9a-f]+");
    }

    @Test
    @DisplayName("hash_returns_same_value_for_identical_inputs")
    void hash_returns_same_value_for_identical_inputs() {
        byte[] input = "same content".getBytes();

        String first = hasher.hash(input);
        String second = hasher.hash(input);

        assertThat(first).isEqualTo(second);
    }

    @Test
    @DisplayName("hash_returns_different_values_for_different_inputs")
    void hash_returns_different_values_for_different_inputs() {
        byte[] input1 = "content A".getBytes();
        byte[] input2 = "content B".getBytes();

        String hash1 = hasher.hash(input1);
        String hash2 = hasher.hash(input2);

        assertThat(hash1).isNotEqualTo(hash2);
    }

    @Test
    @DisplayName("hash_handles_empty_byte_array_without_exception")
    void hash_handles_empty_byte_array_without_exception() {
        byte[] empty = new byte[0];

        String result = hasher.hash(empty);

        assertThat(result).hasSize(64);
    }
}
