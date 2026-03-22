package com.rehearse.api.domain.questionpool.util;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

public final class CacheKeyGenerator {

    /**
     * 포지션/스택에 무관한 공통 유형.
     * 캐시 키에서 position, techStack을 제외하여 데이터 1벌로 전 포지션 커버.
     */
    private static final Set<InterviewType> POSITION_AGNOSTIC_TYPES = Set.of(
            InterviewType.CS_FUNDAMENTAL,
            InterviewType.BEHAVIORAL,
            InterviewType.SYSTEM_DESIGN
    );

    private CacheKeyGenerator() {
    }

    public static String generate(Position position, InterviewLevel level,
                                  TechStack techStack, InterviewType type,
                                  List<String> csSubTopics) {

        String base;
        if (POSITION_AGNOSTIC_TYPES.contains(type)) {
            // 공통 유형: 레벨 + 유형만으로 키 생성
            base = level.name() + ":" + type.name();
        } else {
            // 포지션 특화 유형: 포지션 + 레벨 + 스택 + 유형
            base = position.name() + ":" + level.name() + ":"
                    + techStack.name() + ":" + type.name();
        }

        if (type == InterviewType.CS_FUNDAMENTAL
                && csSubTopics != null && !csSubTopics.isEmpty()) {
            String sorted = csSubTopics.stream().sorted().collect(Collectors.joining(","));
            return base + ":" + sorted;
        }
        return base;
    }
}
