package com.rehearse.api.domain.question.service;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.entity.TechStack;

import java.util.Set;

public final class QuestionCacheKeyGenerator {

    /**
     * 포지션/스택에 무관한 공통 유형.
     * 캐시 키에서 position, techStack을 제외하여 데이터 1벌로 전 포지션 커버.
     */
    private static final Set<InterviewType> POSITION_AGNOSTIC_TYPES = Set.of(
            InterviewType.CS_FUNDAMENTAL,
            InterviewType.BEHAVIORAL,
            InterviewType.SYSTEM_DESIGN
    );

    private QuestionCacheKeyGenerator() {
    }

    public static String generate(Position position, InterviewLevel level,
                                  TechStack techStack, InterviewType type) {

        if (POSITION_AGNOSTIC_TYPES.contains(type)) {
            // 공통 유형: 레벨 + 유형만으로 키 생성
            // CS 세부 주제는 캐시 키에 포함하지 않고, 조회 시 category 필터링으로 처리
            return level.name() + ":" + type.name();
        }
        // 포지션 특화 유형: 포지션 + 레벨 + 스택 + 유형
        return position.name() + ":" + level.name() + ":"
                + techStack.name() + ":" + type.name();
    }
}
