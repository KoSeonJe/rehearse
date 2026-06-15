# Task B2 — UploadUrlResponse.traceId 필드 추가 + QuestionSetService 전달

> **PR**: PR-B
> **영역**: BE
> **선행 Task**: B1

---

## 변경 파일

- `backend/src/main/java/com/rehearse/api/domain/question/dto/response/UploadUrlResponse.java` — **변경**. `traceId` 필드 추가 (`@Builder` 패턴 유지).
- `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionSetService.java` (또는 `generateUploadUrl` 보유 서비스) — **변경**. `MDC.get("traceId")` 읽어 `AwsS3Service.generatePutPresignedUrl(s3Key, contentType, traceId)` 에 전달 + Response 에 동봉.
- `backend/src/test/java/com/rehearse/api/domain/question/service/QuestionSetServiceIntegrationTest.java` — **신규 또는 확장** (Service Integration).

---

## 핵심 로직

```java
// UploadUrlResponse.java
@Getter
@Builder
public class UploadUrlResponse {
    private final String uploadUrl;
    private final String s3Key;
    private final Long fileMetadataId;
    private final String traceId;  // 신규

    public static UploadUrlResponse from(String uploadUrl, String s3Key, Long fileMetadataId, String traceId) {
        return UploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .s3Key(s3Key)
                .fileMetadataId(fileMetadataId)
                .traceId(traceId)
                .build();
    }
}
```

```java
// QuestionSetService.java (변경부)
public UploadUrlResponse generateUploadUrl(Long interviewId, Long questionSetId, String contentType) {
    String traceId = MDC.get("traceId");
    if (traceId == null) {
        // TraceIdFilter 선행 보장. 누락 시 운영 결함 — 명시 실패
        throw new IllegalStateException("traceId MDC not set; TraceIdFilter must run before upload URL issuance");
    }
    String s3Key = buildS3Key(interviewId, questionSetId);
    String uploadUrl = s3Service.generatePutPresignedUrl(s3Key, contentType, traceId);
    FileMetadata saved = fileMetadataRepository.save(...);
    return UploadUrlResponse.from(uploadUrl, s3Key, saved.getId(), traceId);
}
```

---

## 테스트

**카테고리**: Service Integration (`ServiceIntegrationSupport` — Spring 컨텍스트, 외부 S3 Mock)

```java
@DisplayName("UploadUrl 발급 시 MDC traceId 가 Response 와 S3 metadata 에 전파된다")
class QuestionSetServiceIntegrationTest extends ServiceIntegrationSupport {

    @Test
    void generateUploadUrl_traceId_propagation() {
        MDC.put("traceId", "abc12345");
        try {
            UploadUrlResponse response = questionSetService.generateUploadUrl(
                interviewId, questionSetId, "video/webm");

            assertThat(response.getTraceId()).isEqualTo("abc12345");
            // Mock S3Service 가 받은 인자 검증 (S3Service 는 외부 어댑터로 Mock 허용)
            verify(s3Service).generatePutPresignedUrl(any(), eq("video/webm"), eq("abc12345"));
        } finally {
            MDC.remove("traceId");
        }
    }

    @Test
    @DisplayName("MDC traceId 미적재 상태에서 호출 시 IllegalStateException")
    void throws_when_mdc_traceId_missing() { ... }
}
```

---

## 의존

- 선행: B1 (S3Service 시그니처)
- 외부: SLF4J MDC

---

## Verification Hook

- 명령: `./gradlew test --tests "com.rehearse.api.domain.question.service.QuestionSetServiceIntegrationTest"`
- 통과 기준: 2 케이스 green
- 관찰 가능 동작: dev 환경 `/upload-url` 응답 body 에 `traceId` 필드 등장

---

## 커밋 메시지 (예상)

```
feat(BE): UploadUrlResponse 에 traceId 노출 + 발급 시 MDC 캡처 전달
```
