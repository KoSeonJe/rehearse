package com.rehearse.api.domain.feedback.session.infra;

public interface LambdaRetryTrigger {

    void trigger(Long interviewId);
}
