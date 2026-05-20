package com.rehearse.api.domain.resume.entity;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record DepthSignals(
        List<String> tradeoffs,
        List<String> alternatives,
        List<String> quantitative,
        List<String> decisionRationale
) {

    public DepthSignals {
        tradeoffs = tradeoffs == null ? List.of() : List.copyOf(tradeoffs);
        alternatives = alternatives == null ? List.of() : List.copyOf(alternatives);
        quantitative = quantitative == null ? List.of() : List.copyOf(quantitative);
        decisionRationale = decisionRationale == null ? List.of() : List.copyOf(decisionRationale);
    }

    public static DepthSignals empty() {
        return new DepthSignals(List.of(), List.of(), List.of(), List.of());
    }
}
