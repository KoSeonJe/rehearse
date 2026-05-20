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

    public boolean isEmpty() {
        return tradeoffs.isEmpty() && alternatives.isEmpty()
                && quantitative.isEmpty() && decisionRationale.isEmpty();
    }

    public static final class EmptyValueFilter {
        @Override
        public boolean equals(Object obj) {
            return obj instanceof DepthSignals signals && signals.isEmpty();
        }

        @Override
        public int hashCode() {
            return 0;
        }
    }
}
