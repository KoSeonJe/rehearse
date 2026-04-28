package com.rehearse.api.domain.resume.entity;

import java.util.List;

public record ChainReference(
        String chainId,
        String topic,
        int priority,
        List<Integer> levelsToCover
) {

    public static final String SEPARATOR = "::";

    private static final int MIN_LEVEL = 1;
    private static final int MAX_LEVEL = 4;
    private static final int MIN_PRIORITY = 1;

    public static String synthesizeChainId(String projectId, String topic) {
        return projectId + SEPARATOR + topic;
    }

    public String projectId() {
        String[] parts = chainId.split(SEPARATOR, 2);
        return parts[0];
    }

    public ChainReference {
        if (chainId == null || chainId.isBlank()) {
            throw new IllegalArgumentException("chainId 는 필수입니다.");
        }
        if (!chainId.contains(SEPARATOR)) {
            throw new IllegalArgumentException("chainId 는 '" + SEPARATOR + "' 구분자를 포함한 합성키여야 합니다. chainId=" + chainId);
        }
        if (topic == null || topic.isBlank()) {
            throw new IllegalArgumentException("topic 은 필수입니다.");
        }
        if (priority < MIN_PRIORITY) {
            throw new IllegalArgumentException("priority 는 1 이상이어야 합니다. priority=" + priority);
        }
        if (levelsToCover == null) {
            throw new IllegalArgumentException("levelsToCover 는 필수입니다.");
        }
        if (levelsToCover.isEmpty()) {
            throw new IllegalArgumentException("levelsToCover 는 비어있을 수 없습니다.");
        }
        for (int level : levelsToCover) {
            if (level < MIN_LEVEL || level > MAX_LEVEL) {
                throw new IllegalArgumentException(
                        "levelsToCover 의 각 원소는 " + MIN_LEVEL + "~" + MAX_LEVEL + " 범위여야 합니다. level=" + level
                );
            }
        }
        levelsToCover = List.copyOf(levelsToCover);
    }
}
