package com.rehearse.api.domain.resume.domain;

public record ChainStep(
        int level,
        StepType type,
        String question
) {}
