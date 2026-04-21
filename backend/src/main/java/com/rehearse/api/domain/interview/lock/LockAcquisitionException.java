package com.rehearse.api.domain.interview.lock;

import java.time.Duration;

public class LockAcquisitionException extends RuntimeException {

    public LockAcquisitionException(Long interviewId, Duration timeout) {
        super("Lock acquisition timed out for interviewId=" + interviewId
                + " after " + timeout.toMillis() + "ms");
    }

    public LockAcquisitionException(Long interviewId, InterruptedException cause) {
        super("Lock acquisition interrupted for interviewId=" + interviewId, cause);
    }
}
