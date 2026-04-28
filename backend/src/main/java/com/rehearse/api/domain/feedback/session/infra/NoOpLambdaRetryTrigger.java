package com.rehearse.api.domain.feedback.session.infra;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class NoOpLambdaRetryTrigger implements LambdaRetryTrigger {

    @Override
    public void trigger(Long interviewId) {
        // Lambda 재호출 구현은 별도 PR에서 실 구현 예정
        log.info("Lambda retry triggered (no-op): interviewId={}", interviewId);
    }
}
