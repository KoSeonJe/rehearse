package com.rehearse.api.domain.resume.entity;

public record ChainStep(
        int level,
        StepType type,
        String question
) {}
