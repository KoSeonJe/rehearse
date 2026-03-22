package com.rehearse.api.infra.aws;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;
import software.amazon.awssdk.services.s3.S3Client;
import software.amazon.awssdk.services.s3.presigner.S3Presigner;
import software.amazon.awssdk.services.s3.presigner.model.PresignedGetObjectRequest;
import software.amazon.awssdk.services.s3.presigner.model.PresignedPutObjectRequest;

import java.net.MalformedURLException;
import java.net.URL;

import software.amazon.awssdk.services.s3.presigner.model.GetObjectPresignRequest;
import software.amazon.awssdk.services.s3.presigner.model.PutObjectPresignRequest;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class AwsS3ServiceTest {

    @Mock
    private S3Client s3Client;

    @Mock
    private S3Presigner s3Presigner;

    @Mock
    private PresignedPutObjectRequest presignedPutObjectRequest;

    @Mock
    private PresignedGetObjectRequest presignedGetObjectRequest;

    private AwsS3Service awsS3Service;

    @BeforeEach
    void setUp() {
        awsS3Service = new AwsS3Service(s3Client, s3Presigner);
        ReflectionTestUtils.setField(awsS3Service, "bucket", "test-bucket");
    }

    @Test
    @DisplayName("generatePutPresignedUrl 호출 시 S3Presigner로 PUT URL을 생성하여 반환한다")
    void generatePutPresignedUrl_success() throws MalformedURLException {
        // given
        String s3Key = "recordings/interview-1/video.webm";
        String contentType = "video/webm";
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/recordings/interview-1/video.webm?X-Amz-Signature=abc");

        given(s3Presigner.presignPutObject(any(PutObjectPresignRequest.class))).willReturn(presignedPutObjectRequest);
        given(presignedPutObjectRequest.url()).willReturn(expectedUrl);

        // when
        String result = awsS3Service.generatePutPresignedUrl(s3Key, contentType);

        // then
        assertThat(result).isEqualTo(expectedUrl.toString());
        then(s3Presigner).should().presignPutObject(any(PutObjectPresignRequest.class));
    }

    @Test
    @DisplayName("generateGetPresignedUrl 호출 시 S3Presigner로 GET URL을 생성하여 반환한다")
    void generateGetPresignedUrl_success() throws MalformedURLException {
        // given
        String s3Key = "recordings/interview-1/video.mp4";
        URL expectedUrl = new URL("https://test-bucket.s3.amazonaws.com/recordings/interview-1/video.mp4?X-Amz-Signature=xyz");

        given(s3Presigner.presignGetObject(any(GetObjectPresignRequest.class))).willReturn(presignedGetObjectRequest);
        given(presignedGetObjectRequest.url()).willReturn(expectedUrl);

        // when
        String result = awsS3Service.generateGetPresignedUrl(s3Key);

        // then
        assertThat(result).isEqualTo(expectedUrl.toString());
        then(s3Presigner).should().presignGetObject(any(GetObjectPresignRequest.class));
    }

    @Test
    @DisplayName("getBucket 호출 시 설정된 버킷 이름을 반환한다")
    void getBucket_returnsBucketName() {
        // when
        String bucket = awsS3Service.getBucket();

        // then
        assertThat(bucket).isEqualTo("test-bucket");
    }
}
