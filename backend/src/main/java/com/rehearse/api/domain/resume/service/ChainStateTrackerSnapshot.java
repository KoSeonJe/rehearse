package com.rehearse.api.domain.resume.service;

record ChainStateTrackerSnapshot(
        String chainTopic,
        int currentLevel,
        int consecutiveStay,
        String currentProjectId,
        int orderIndex,
        int answerQuality
) {}
