package com.rehearse.api.domain.feedback.session.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record StrengthItem(
        String dimension,
        String observation,
        @JsonProperty("why_matters") String whyMatters
) {}
