package com.rehearse.api.domain.feedback.session.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import com.fasterxml.jackson.annotation.JsonProperty;

@JsonIgnoreProperties(ignoreUnknown = true)
public record GapItem(
        String dimension,
        String observation,
        @JsonProperty("level_gap") String levelGap,
        @JsonProperty("concrete_action") String concreteAction
) {}
