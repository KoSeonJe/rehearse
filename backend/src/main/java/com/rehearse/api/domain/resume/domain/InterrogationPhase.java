package com.rehearse.api.domain.resume.domain;

import java.util.List;

public record InterrogationPhase(
        List<ChainReference> primaryChains,
        List<ChainReference> backupChains
) {

    public InterrogationPhase {
        if (primaryChains == null) {
            throw new IllegalArgumentException("primaryChains 는 필수입니다.");
        }
        if (primaryChains.isEmpty()) {
            throw new IllegalArgumentException("primaryChains 는 최소 1개 이상이어야 합니다.");
        }
        if (backupChains == null) {
            throw new IllegalArgumentException("backupChains 는 필수입니다.");
        }
        validateAscendingPriority(primaryChains, "primaryChains");
        validateAscendingPriority(backupChains, "backupChains");
        primaryChains = List.copyOf(primaryChains);
        backupChains = List.copyOf(backupChains);
    }

    private static void validateAscendingPriority(List<ChainReference> chains, String fieldName) {
        for (int i = 1; i < chains.size(); i++) {
            if (chains.get(i).priority() <= chains.get(i - 1).priority()) {
                throw new IllegalArgumentException(
                        fieldName + " 의 priority 는 중복 없이 오름차순이어야 합니다."
                );
            }
        }
    }
}
