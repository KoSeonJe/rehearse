package com.rehearse.api.infra.google;

import com.google.cloud.texttospeech.v1.stub.GrpcTextToSpeechStub;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThatCode;

/**
 * protobuf-java 버전 충돌(예: GeneratedMessageV3 누락)로 GrpcTextToSpeechStub의
 * <clinit>가 깨지는 회귀를 사전에 탐지하는 경량 smoke test.
 *
 * 실제 Google Cloud 호출이 아니라 클래스 초기화만 트리거하므로
 * 자격 증명 없이 통과해야 한다. 실패한다면 protobuf 4.x 중에서도
 * GeneratedMessageV3 compat이 없는 버전이 다시 클래스패스에 침투했다는 뜻.
 *
 * 2026-04-11 #293 이후 배포 실패 회귀 방지.
 */
@DisplayName("GoogleTTS - protobuf 클래스 로딩 smoke test")
class GoogleTtsClassLoadingTest {

    @Test
    @DisplayName("GrpcTextToSpeechStub 클래스 초기화 시 protobuf 버전 충돌이 발생하지 않는다")
    void classForName_grpcStubInit_doesNotThrow() {
        // when & then
        assertThatCode(() ->
                Class.forName(
                        GrpcTextToSpeechStub.class.getName(),
                        true,
                        GrpcTextToSpeechStub.class.getClassLoader()
                )
        ).doesNotThrowAnyException();
    }
}
