package com.rehearse.api.domain.resume.domain;

import java.util.EnumSet;
import java.util.List;
import java.util.Set;

public record InterrogationChain(
        String topic,
        double confidence,
        List<ChainStep> steps
) {

    private static final Set<StepType> REQUIRED_TYPES = EnumSet.of(
            StepType.WHAT, StepType.HOW, StepType.WHY_MECH, StepType.TRADEOFF
    );

    public InterrogationChain {
        if (steps == null || steps.isEmpty()) {
            throw new IllegalArgumentException("InterrogationChain steps must not be null or empty");
        }
        Set<StepType> presentTypes = EnumSet.noneOf(StepType.class);
        for (ChainStep step : steps) {
            if (!presentTypes.add(step.type())) {
                throw new IllegalArgumentException("Duplicate StepType in chain: " + step.type());
            }
        }
        if (!presentTypes.equals(REQUIRED_TYPES)) {
            throw new IllegalArgumentException(
                    "InterrogationChain must contain exactly one of each: WHAT, HOW, WHY_MECH, TRADEOFF. Got: " + presentTypes
            );
        }
        steps = List.copyOf(steps);
    }
}
