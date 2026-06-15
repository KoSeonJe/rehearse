package com.rehearse.api.domain.feedback.session.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DeliverySection(
        @JsonProperty("filler_words") String fillerWords,
        @JsonProperty("tone_pattern") String tonePattern,
        String action
) {}
