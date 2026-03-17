package com.rehearse.api.infra.aws;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.stereotype.Service;

@Slf4j
@Service
@ConditionalOnMissingBean(AwsS3Service.class)
public class MockS3Service implements S3Service {

    @PostConstruct
    void init() {
        log.warn("=== MockS3Service 활성화: AWS 없이 Mock URL로 동작합니다 ===");
    }

    @Override
    public String generatePutPresignedUrl(String s3Key, String contentType) {
        log.info("[Mock S3] PUT Presigned URL 생성: s3Key={}", s3Key);
        return "https://mock-s3.example.com/put/" + s3Key;
    }

    @Override
    public String generateGetPresignedUrl(String s3Key) {
        log.info("[Mock S3] GET Presigned URL 생성: s3Key={}", s3Key);
        return "https://mock-s3.example.com/get/" + s3Key;
    }

    @Override
    public String getBucket() {
        return "mock-bucket";
    }
}
