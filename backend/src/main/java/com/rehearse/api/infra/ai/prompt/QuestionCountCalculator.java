package com.rehearse.api.infra.ai.prompt;

public final class QuestionCountCalculator {

    private static final int MINUTES_PER_QUESTION = 3;
    private static final int MIN_QUESTION_COUNT = 2;
    private static final int MAX_QUESTION_COUNT = 24;
    private static final int SINGLE_TYPE_QUESTION_COUNT = 5;
    private static final int DOUBLE_TYPE_QUESTION_COUNT = 6;
    private static final int MULTI_TYPE_QUESTION_COUNT = 8;

    private QuestionCountCalculator() {
    }

    public static int calculate(Integer durationMinutes, int typeCount) {
        if (durationMinutes != null) {
            int count = (int) Math.round((double) durationMinutes / MINUTES_PER_QUESTION);
            return Math.max(MIN_QUESTION_COUNT, Math.min(count, MAX_QUESTION_COUNT));
        }
        return switch (typeCount) {
            case 1 -> SINGLE_TYPE_QUESTION_COUNT;
            case 2 -> DOUBLE_TYPE_QUESTION_COUNT;
            default -> MULTI_TYPE_QUESTION_COUNT;
        };
    }
}
