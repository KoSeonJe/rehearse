package com.rehearse.api.domain.feedback.rubric.entity;

import java.util.Map;

public class RubricFamily {

    private final Map<String, RubricDimension> dimensions;

    public RubricFamily(Map<String, RubricDimension> dimensions) {
        this.dimensions = Map.copyOf(dimensions);
    }

    public RubricDimension getDimension(String ref) {
        return dimensions.get(ref);
    }

    public Map<String, RubricDimension> getDimensions() {
        return dimensions;
    }
}
