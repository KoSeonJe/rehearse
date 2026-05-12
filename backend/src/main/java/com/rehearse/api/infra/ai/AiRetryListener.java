package com.rehearse.api.infra.ai;

import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class AiRetryListener implements RetryListener {

    private static final String LABEL_ATTR = "ai.retry.label";

    @Override
    public <T, E extends Throwable> void onError(
            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String label = resolveLabel(context, callback);
        log.warn("AI 호출 재시도 label={} attempt={} cause={}",
                label, context.getRetryCount(), throwable.getClass().getSimpleName(), throwable);
    }

    @Override
    public <T, E extends Throwable> void close(
            RetryContext context, RetryCallback<T, E> callback, Throwable throwable) {
        String label = resolveLabel(context, callback);
        if (throwable == null) {
            if (context.getRetryCount() > 0) {
                log.info("AI 호출 재시도 후 성공 label={} attempts={}", label, context.getRetryCount());
            }
        } else {
            log.warn("AI 호출 재시도 최종 실패 label={} attempts={} cause={}",
                    label, context.getRetryCount(), throwable.getClass().getSimpleName());
        }
    }

    private <T, E extends Throwable> String resolveLabel(RetryContext context, RetryCallback<T, E> callback) {
        Object labelAttr = context.getAttribute(LABEL_ATTR);
        if (labelAttr != null) {
            return labelAttr.toString();
        }
        return callback.getLabel() != null ? callback.getLabel() : "unknown";
    }
}
