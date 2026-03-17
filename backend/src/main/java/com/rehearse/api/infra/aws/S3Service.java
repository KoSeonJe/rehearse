package com.rehearse.api.infra.aws;

public interface S3Service {

    String generatePutPresignedUrl(String s3Key, String contentType);

    String generateGetPresignedUrl(String s3Key);

    String getBucket();
}
