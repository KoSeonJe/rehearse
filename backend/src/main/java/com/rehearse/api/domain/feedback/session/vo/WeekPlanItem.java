package com.rehearse.api.domain.feedback.session.vo;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

import java.util.List;

@JsonIgnoreProperties(ignoreUnknown = true)
public record WeekPlanItem(
        int priority,
        String topic,
        List<String> resources,
        String practice
) {}
