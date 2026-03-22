package com.rehearse.api.domain.questionpool.util;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.List;
import java.util.stream.Collectors;

public final class CacheKeyGenerator {

    private CacheKeyGenerator() {
    }

    public static String generate(Position position, InterviewLevel level,
                                  TechStack techStack, InterviewType type,
                                  List<String> csSubTopics) {
        String base = position.name() + ":" + level.name() + ":"
                + techStack.name() + ":" + type.name();

        if (type == InterviewType.CS_FUNDAMENTAL
                && csSubTopics != null && !csSubTopics.isEmpty()) {
            String sorted = csSubTopics.stream().sorted().collect(Collectors.joining(","));
            return base + ":" + sorted;
        }
        return base;
    }
}
