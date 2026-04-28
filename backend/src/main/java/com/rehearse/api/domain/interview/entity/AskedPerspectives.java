package com.rehearse.api.domain.interview.entity;

import com.rehearse.api.domain.interview.Perspective;
import com.rehearse.api.domain.interview.dto.FollowUpRequest.FollowUpExchange;

import java.util.List;
import java.util.Objects;
import java.util.Optional;

public record AskedPerspectives(List<Perspective> values) {

    public AskedPerspectives {
        values = values != null ? List.copyOf(values) : List.of();
    }

    public static AskedPerspectives from(List<FollowUpExchange> exchanges) {
        if (exchanges == null || exchanges.isEmpty()) {
            return new AskedPerspectives(List.of());
        }
        return new AskedPerspectives(exchanges.stream()
                .map(FollowUpExchange::getSelectedPerspective)
                .filter(Objects::nonNull)
                .map(AskedPerspectives::parse)
                .filter(Optional::isPresent)
                .map(Optional::get)
                .distinct()
                .toList());
    }

    public static AskedPerspectives empty() {
        return new AskedPerspectives(List.of());
    }

    public boolean isEmpty() {
        return values.isEmpty();
    }

    private static Optional<Perspective> parse(String raw) {
        if (raw == null || raw.isBlank()) {
            return Optional.empty();
        }
        try {
            return Optional.of(Perspective.valueOf(raw.trim().toUpperCase()));
        } catch (IllegalArgumentException ignored) {
            return Optional.empty();
        }
    }
}
