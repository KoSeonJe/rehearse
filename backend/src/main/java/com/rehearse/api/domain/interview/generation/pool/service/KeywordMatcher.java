package com.rehearse.api.domain.interview.generation.pool.service;

import org.springframework.stereotype.Component;

import java.util.List;

@Component
public class KeywordMatcher {

    public boolean matches(List<String> matchKeywords, int threshold, String userAnswer) {
        if (matchKeywords == null || matchKeywords.isEmpty() || userAnswer == null) {
            return false;
        }
        String lowerAnswer = userAnswer.toLowerCase();
        long matchCount = matchKeywords.stream()
                .filter(keyword -> lowerAnswer.contains(keyword.toLowerCase()))
                .count();
        return matchCount >= threshold;
    }
}
