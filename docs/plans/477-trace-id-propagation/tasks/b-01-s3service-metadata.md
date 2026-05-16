# Task B1 — S3Service.generatePutPresignedUrl 시그니처 변경 + metadata 주입

> **PR**: PR-B
> **영역**: BE
> **선행 Task**: PR-A 머지 후

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/infra/aws/S3Service.java` (port interface) — **변경**. 두 시그니처 모두 갱신:
  - `generatePutPresignedUrl(String s3Key, String contentType)` → `generatePutPresignedUrl(String s3Key, String contentType, String traceId)`
  - `retriggerUploadEvent(String s3Key)` → `retriggerUploadEvent(String s3Key, String traceId)`
- `backend/src/main/java/com/rehearse/api/infra/aws/AwsS3Service.java` — **변경**. 두 메서드 모두:
  - `generatePutPresignedUrl`: `PutObjectRequest.builder()...metadata(Map.of("trace-id", traceId)).build()`
  - `retriggerUploadEvent`: 기존 `CopyObjectRequest` 의 `metadataDirective("REPLACE")` **유지** + `.metadata(Map.of("trace-id", traceId))` **추가**. REPLACE 모드에서 신규 metadata 명시 안 하면 원본 metadata 손실 → 추적 단절 방지를 위해 필수
- `backend/src/main/java/com/rehearse/api/infra/aws/MockS3Service.java` (테스트 / local 용) — **변경**. 두 시그니처 추가만 (동작 무변경).
- `backend/src/main/java/com/rehearse/api/domain/question/service/InternalQuestionSetService.java:142` — **변경**. `s3Service.retriggerUploadEvent(s3Key)` 호출 site 에서 `MDC.get("traceId")` 캡처 후 전달. AFTER_COMMIT 안이므로 MDC snapshot 을 outer 변수로 캡처 (스코프 보장)
- `backend/src/test/java/com/rehearse/api/infra/aws/AwsS3ServiceIntegrationTest.java` — **신규 또는 확장** (Infra Integration).

---

## 핵심 로직

```java
// AwsS3Service.java (변경부 1 — generatePutPresignedUrl)
public String generatePutPresignedUrl(String s3Key, String contentType, String traceId) {
    PutObjectRequest putRequest = PutObjectRequest.builder()
            .bucket(bucket)
            .key(s3Key)
            .contentType(contentType)
            .metadata(Map.of("trace-id", traceId))
            .build();

    PutObjectPresignRequest presignRequest = PutObjectPresignRequest.builder()
            .signatureDuration(PUT_URL_EXPIRATION)
            .putObjectRequest(putRequest)
            .build();

    return s3Presigner.presignPutObject(presignRequest).url().toString();
}

// AwsS3Service.java (변경부 2 — retriggerUploadEvent)
@Override
public void retriggerUploadEvent(String s3Key, String traceId) {
    CopyObjectRequest copyRequest = CopyObjectRequest.builder()
            .sourceBucket(bucket)
            .sourceKey(s3Key)
            .destinationBucket(bucket)
            .destinationKey(s3Key)
            .metadataDirective("REPLACE")            // 기존 유지
            .metadata(Map.of("trace-id", traceId))   // 신규 — REPLACE 시 신규 metadata 명시 필수
            .contentType("video/webm")
            .build();

    s3Client.copyObject(copyRequest);
    log.info("S3 객체 재복사로 EventBridge 재트리거: s3Key={}, traceId={}", s3Key, traceId);
}
```

```java
// InternalQuestionSetService.java:142 호출부 (변경부)
final String traceIdSnapshot = MDC.get("traceId");  // 트랜잭션 안 캡처
TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
    @Override
    public void afterCommit() {
        try {
            s3Service.retriggerUploadEvent(s3Key, traceIdSnapshot);
            // ...
        }
    }
});
```

> AWS SDK v2 동작: `PutObjectRequest.metadata(Map)` 의 키는 자동으로 `x-amz-meta-{key}` 형태 헤더로 변환되며, presigned URL 생성 시 SignedHeaders 에 포함된다. FE 가 PUT 시 동일 헤더로 송신해야 SignatureDoesNotMatch 회피.
> `CopyObjectRequest.metadataDirective("REPLACE")` 는 destination 객체의 user metadata 를 신규 metadata 로 **완전 교체**. metadata 미명시 시 trace-id 손실 → Lambda head_object 가 fallback UUID 로 처리 (추적 단절). 명시 필수.

---

## 테스트

**카테고리**: Infra Integration (`InfraIntegrationSupport` — `S3Presigner` 단독, 외부 호출 없음)

```java
class AwsS3ServiceIntegrationTest extends InfraIntegrationSupport {
    @Test
    @DisplayName("presigned URL signed headers 에 x-amz-meta-trace-id 가 포함된다")
    void generatePutPresignedUrl_includesTraceMetadata_inSignedHeader() {
        String url = service.generatePutPresignedUrl("interviews/raw/.../raw.webm", "video/webm", "abc12345");

        // URL 의 X-Amz-SignedHeaders 쿼리 파라미터를 URL-decode 후 검증.
        // raw url.contains 매칭은 encoded ('%3B') 를 우회할 위험 → 명시적 decode.
        String signedHeaders = extractQueryParam(url, "X-Amz-SignedHeaders");
        String decoded = URLDecoder.decode(signedHeaders, StandardCharsets.UTF_8);
        assertThat(decoded).contains("x-amz-meta-trace-id");
    }

    private static String extractQueryParam(String url, String key) {
        // URI.create + UriComponentsBuilder 또는 직접 split 로 구현. testing.md TestFixtures 우회 X
        return URI.create(url).getQuery().lines()
                .flatMap(q -> Arrays.stream(q.split("&")))
                .filter(p -> p.startsWith(key + "="))
                .map(p -> p.substring(key.length() + 1))
                .findFirst().orElseThrow();
    }
}
```

> `S3Presigner` 는 실제 AWS 호출 없이 URL 생성. WireMock 불필요.
> raw `url.contains("x-amz-meta-trace-id")` 단언 금지 — `X-Amz-SignedHeaders` 값은 `;` 구분자라 URL encoding 이슈 + 다른 쿼리 파라미터 (`X-Amz-Credential` 등) 안에 같은 문자열 포함 가능성 0 이 아니므로, **명시적으로 `X-Amz-SignedHeaders` 파라미터만 추출 후 decode → 검증** 필수.

---

## 의존

- 선행: PR-A 머지 (헤더명 통일된 후)
- 외부: AWS SDK v2 `S3Presigner` (기존)

---

## Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.infra.aws.AwsS3ServiceIntegrationTest"`
- 통과 기준: signed header 검증 케이스 green
- 관찰 가능 동작: dev 환경 presigned URL 응답 → 쿼리 파라미터 `X-Amz-SignedHeaders` 에 `x-amz-meta-trace-id` 포함 확인

---

## 커밋 메시지 (예상)

```
feat(BE): presigned PUT URL 에 trace-id metadata 주입
```
