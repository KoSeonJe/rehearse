package com.rehearse.api.infra.aws;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import software.amazon.awssdk.regions.Region;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;

/**
 * AWS 자격증명 전략: DefaultCredentialsProvider 체인에 위임.
 * 우선순위: 환경변수 → Java 시스템 프로퍼티 → 웹 아이덴티티 토큰 → 프로필 파일 → EC2 IMDS.
 *
 * - 로컬/dev: `~/.aws/credentials` 또는 환경변수로 동작
 * - prod: EC2 Instance Profile(rehearse-prod-ec2-role)을 IMDS로 자동 주입
 *
 * Static 키 주입 경로를 제거하여 prod 컨테이너에 access key를 저장하지 않는다.
 */
@Configuration
@ConditionalOnProperty(prefix = "aws", name = "enabled", havingValue = "true")
public class AwsConfig {

    @Value("${aws.region}")
    private String region;

    @Bean
    public S3Client s3Client() {
        return S3Client.builder()
                .region(Region.of(region))
                .build();
    }

    @Bean
    public S3Presigner s3Presigner() {
        return S3Presigner.builder()
                .region(Region.of(region))
                .build();
    }
}
