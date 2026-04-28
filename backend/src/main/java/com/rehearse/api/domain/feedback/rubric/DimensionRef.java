package com.rehearse.api.domain.feedback.rubric;

public record DimensionRef(
        String ref,
        double weight,
        String conditional
) {

    public DimensionRef(String ref, double weight) {
        this(ref, weight, null);
    }

    public boolean isConditional() {
        return conditional != null && !conditional.isBlank();
    }
}
