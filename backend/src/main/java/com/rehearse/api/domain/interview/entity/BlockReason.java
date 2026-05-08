package com.rehearse.api.domain.interview.entity;

public enum BlockReason {
    PUBLISH_SKIP("publish-skip"),
    QUESTION_ID_MISSING("questionId-missing"),
    RESPONSE_QUESTION_ID_MISSING("response-questionid-missing"),
    RESPONSE_QUESTION_ID_MISMATCH("response-questionid-mismatch"),
    ANALYZER_SKIP("analyzer-skip"),
    STEP_B_SKIP("step-b-skip");

    private final String value;

    BlockReason(String value) {
        this.value = value;
    }

    public String logValue() {
        return value;
    }
}
