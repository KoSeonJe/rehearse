package com.rehearse.api.domain.file.entity;

public enum FileStatus {
    PENDING,
    UPLOADED,
    CONVERTING,
    CONVERTED,
    FAILED;

    public boolean canTransitionTo(FileStatus target) {
        return switch (this) {
            case PENDING -> target == UPLOADED || target == FAILED;
            case UPLOADED -> target == CONVERTING || target == FAILED;
            case CONVERTING -> target == CONVERTED || target == FAILED;
            case CONVERTED -> target == FAILED;
            case FAILED -> false;
        };
    }
}
