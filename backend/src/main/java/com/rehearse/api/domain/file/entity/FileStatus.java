package com.rehearse.api.domain.file.entity;

public enum FileStatus {
    PENDING,
    UPLOADED,
    FAILED;

    public boolean canTransitionTo(FileStatus target) {
        return switch (this) {
            case PENDING -> target == UPLOADED || target == FAILED;
            case UPLOADED -> target == FAILED;
            case FAILED -> target == UPLOADED;
        };
    }
}
