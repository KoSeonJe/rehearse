package com.rehearse.api.infra.aws;

import org.springframework.stereotype.Component;

import java.time.Clock;
import java.time.Instant;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.UUID;

@Component
public class S3KeyGenerator {

    private static final DateTimeFormatter PARTITION_FMT =
            DateTimeFormatter.ofPattern("yyyy/MM/dd").withZone(ZoneOffset.UTC);
    private static final int UUID_LENGTH = 12;

    private final Clock clock;

    public S3KeyGenerator() {
        this(Clock.systemUTC());
    }

    S3KeyGenerator(Clock clock) {
        this.clock = clock;
    }

    public String generateRawVideoKey(long interviewId, long questionSetId) {
        validatePositive(interviewId, "interviewId");
        validatePositive(questionSetId, "questionSetId");
        String date = PARTITION_FMT.format(Instant.now(clock));
        String uuid = generateShortUuid();
        return String.format("interviews/raw/%s/%d/%d/%s.webm", date, interviewId, questionSetId, uuid);
    }

    private static String generateShortUuid() {
        return UUID.randomUUID().toString().replace("-", "").substring(0, UUID_LENGTH);
    }

    private static void validatePositive(long value, String field) {
        if (value <= 0) {
            throw new IllegalArgumentException(field + " must be positive: " + value);
        }
    }
}
