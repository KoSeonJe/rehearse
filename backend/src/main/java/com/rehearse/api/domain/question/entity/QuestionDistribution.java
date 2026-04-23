package com.rehearse.api.domain.question.entity;

import com.rehearse.api.domain.interview.entity.InterviewType;

import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

public class QuestionDistribution {

    private final Map<InterviewType, Integer> distribution;

    private QuestionDistribution(Map<InterviewType, Integer> distribution) {
        this.distribution = distribution;
    }

    public static QuestionDistribution create(List<InterviewType> types, int totalCount) {
        int base = totalCount / types.size();
        int remainder = totalCount % types.size();

        Map<InterviewType, Integer> distribution = new LinkedHashMap<>();
        for (int i = 0; i < types.size(); i++) {
            distribution.put(types.get(i), base + (i < remainder ? 1 : 0));
        }
        return new QuestionDistribution(distribution);
    }

    public Map<InterviewType, Integer> getCacheableTypes() {
        return distribution.entrySet().stream()
                .filter(e -> e.getKey().isCacheable())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }

    public Map<InterviewType, Integer> getFreshTypes() {
        return distribution.entrySet().stream()
                .filter(e -> !e.getKey().isCacheable())
                .collect(Collectors.toMap(Map.Entry::getKey, Map.Entry::getValue));
    }
}
