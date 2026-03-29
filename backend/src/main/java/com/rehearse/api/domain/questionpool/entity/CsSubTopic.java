package com.rehearse.api.domain.questionpool.entity;

import java.util.Arrays;
import java.util.List;
import java.util.Optional;

public enum CsSubTopic {
    DATA_STRUCTURE("자료구조"),
    OS("운영체제"),
    NETWORK("네트워크"),
    DATABASE("데이터베이스");

    private final String categoryName;

    CsSubTopic(String categoryName) {
        this.categoryName = categoryName;
    }

    public String getCategoryName() {
        return categoryName;
    }

    public static Optional<String> toCategoryName(String subtopic) {
        try {
            return Optional.of(valueOf(subtopic).getCategoryName());
        } catch (IllegalArgumentException e) {
            return Optional.empty();
        }
    }

    public static List<String> allCategoryNames() {
        return Arrays.stream(values())
                .map(CsSubTopic::getCategoryName)
                .toList();
    }
}
