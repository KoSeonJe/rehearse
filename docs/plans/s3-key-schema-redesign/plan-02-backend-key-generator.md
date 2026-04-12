# Plan 02: 백엔드 `S3KeyGenerator` 유틸 + `QuestionSetService` 수정

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 01 (SSOT 규격 확정)

## Why

현재 `QuestionSetService.java:74`에서 `String.format("videos/%d/qs_%d.webm", ...)` 한 줄로 키를 생성하고 있다. 이 로직을 그대로 수정하면 (1) 테스트 어려움, (2) 여러 곳에서 호출될 때 일관성 보장 불가, (3) UTC 파티션 계산이 서비스 레이어에 섞임.

**별도 유틸 클래스**(`S3KeyGenerator`)로 분리하면 SSOT 규격을 코드로 고정하고, 단위 테스트로 계약을 검증할 수 있다.

## 생성/수정 파일

| 파일 | 작업 |
|---|---|
| `backend/src/main/java/com/rehearse/api/infra/aws/S3KeyGenerator.java` | 신규 생성 |
| `backend/src/main/java/com/rehearse/api/domain/questionset/service/QuestionSetService.java` | 키 생성 로직 유틸 호출로 교체 (`:74`) |
| `backend/src/test/java/com/rehearse/api/infra/aws/S3KeyGeneratorTest.java` | 신규 단위 테스트 |
| `backend/src/test/java/com/rehearse/api/domain/questionset/service/QuestionSetServiceTest.java` | 신규 키 포맷 assertion 수정 |

## 상세

### `S3KeyGenerator.java` 구현

```java
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

    S3KeyGenerator(Clock clock) {  // 테스트용 주입
        this.clock = clock;
    }

    /**
     * 원본 녹화 키 생성.
     * 포맷: interviews/raw/YYYY/MM/DD/{interviewId}/{questionSetId}/{uuid}.webm
     */
    public String generateRawVideoKey(long interviewId, long questionSetId) {
        validatePositive(interviewId, "interviewId");
        validatePositive(questionSetId, "questionSetId");

        String date = PARTITION_FMT.format(Instant.now(clock));
        String uuid = generateShortUuid();
        return String.format(
            "interviews/raw/%s/%d/%d/%s.webm",
            date, interviewId, questionSetId, uuid
        );
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
```

### `QuestionSetService.java` 수정 (`:74` 라인 교체)

**Before**:
```java
String s3Key = String.format("videos/%d/qs_%d.webm", interviewId, questionSetId);
```

**After**:
```java
String s3Key = s3KeyGenerator.generateRawVideoKey(interviewId, questionSetId);
```

**주입**:
```java
private final S3KeyGenerator s3KeyGenerator;
// @RequiredArgsConstructor 또는 생성자에 추가
```

### 단위 테스트 `S3KeyGeneratorTest.java`

```java
@Test
@DisplayName("raw 키는 interviews/raw/ prefix + UTC 날짜 + entity path + uuid + .webm 포맷")
void generateRawVideoKey_format() {
    Clock fixed = Clock.fixed(Instant.parse("2026-04-12T14:30:00Z"), ZoneOffset.UTC);
    S3KeyGenerator generator = new S3KeyGenerator(fixed);

    String key = generator.generateRawVideoKey(123L, 456L);

    assertThat(key).matches(
        "^interviews/raw/2026/04/12/123/456/[a-f0-9]{12}\\.webm$"
    );
}

@Test
@DisplayName("날짜 자정 경계: UTC 기준으로 파티션 결정")
void generateRawVideoKey_utcMidnight() {
    // 한국 시간 09:00 = UTC 00:00 → 2026/04/12
    Clock fixed = Clock.fixed(Instant.parse("2026-04-12T00:00:00Z"), ZoneOffset.UTC);
    S3KeyGenerator generator = new S3KeyGenerator(fixed);

    String key = generator.generateRawVideoKey(1L, 1L);

    assertThat(key).startsWith("interviews/raw/2026/04/12/");
}

@Test
@DisplayName("같은 입력으로 여러 번 호출해도 uuid가 다르다")
void generateRawVideoKey_uniqueUuid() {
    S3KeyGenerator generator = new S3KeyGenerator();
    Set<String> keys = IntStream.range(0, 100)
        .mapToObj(i -> generator.generateRawVideoKey(1L, 1L))
        .collect(Collectors.toSet());

    assertThat(keys).hasSize(100);
}

@Test
@DisplayName("음수 interviewId는 IllegalArgumentException")
void generateRawVideoKey_negative_throws() {
    S3KeyGenerator generator = new S3KeyGenerator();
    assertThatThrownBy(() -> generator.generateRawVideoKey(-1L, 1L))
        .isInstanceOf(IllegalArgumentException.class);
}
```

### `QuestionSetServiceTest.java` 수정

기존 테스트에 `videos/%d/qs_%d.webm` 포맷을 가정한 assertion이 있으면 정규식 매칭으로 교체:

```java
// Before
assertThat(response.getS3Key()).isEqualTo("videos/1/qs_1.webm");

// After
assertThat(response.getS3Key()).matches(
    "^interviews/raw/\\d{4}/\\d{2}/\\d{2}/1/1/[a-f0-9]{12}\\.webm$"
);
```

Mock 기반 테스트라면 `S3KeyGenerator`를 Mock 또는 Spy로 치환해 결정론적 키 반환하도록 구성.

### FE·DB·기타 영향

- **FE**: 영향 없음. `uploadUrl` + `s3Key`를 backend 응답으로 받으며 내부 포맷을 알 필요 없음
- **DB 길이**: `file_metadata.s3_key VARCHAR(500)` 그대로 사용. 신규 키 실측 길이 ~55자(`interviews/raw/2026/04/12/12345/67890/abcdef012345.webm`). 여유 충분, 마이그레이션 불필요
- **DB UNIQUE 제약 — 중요**: `V4__add_question_set_and_file_metadata.sql:21`에 `CREATE UNIQUE INDEX idx_file_metadata_s3_key ON file_metadata(s3_key)` 존재. 현행 레거시 포맷(`videos/{iid}/qs_{qsid}.webm`)은 (interviewId, questionSetId) 쌍에 대해 결정론적 → 같은 질문셋 재녹화 시 이 UNIQUE 제약이 충돌 가능성. **신규 uuid 포맷은 매 호출마다 고유 키를 발급 → UNIQUE 제약과 자연스럽게 정합**. 재녹화는 새 `FileMetadata` 행을 독립적으로 생성할 수 있어 히스토리 보존이라는 요구사항(Plan 01 §3)과 정합. DB 마이그레이션은 불필요. 다만 재녹화 구현 로직(`QuestionSet.assignFileMetadata` 재호출 시 기존 행 orphan 여부)은 본 플랜 범위 밖이며, 필요 시 별도 티켓으로 분리
- **InternalFileService / retriggerUploadEvent**: s3Key 문자열만 다루므로 포맷 불가지론. 영향 없음
- **`update_convert_status(streaming_s3_key=...)` 경로**: Lambda convert는 변환 완료 시 `api_client.update_convert_status`를 통해 `file_metadata.streaming_s3_key`를 `interviews/mp4/...`로 업데이트. 백엔드 internal API는 s3Key 문자열을 그대로 저장하므로 포맷 변경에 무영향 (검증만)
- **Mockito 기반 기존 테스트**: 하드코딩된 키 문자열이 있는지 grep 필수 (`grep -rn "videos/" backend/src/test`)

## 담당 에이전트

- Implement: `backend` — 유틸 클래스, 서비스 수정, 테스트 추가
- Review: `code-reviewer` — SOLID, DI 주입, 테스트 품질
- Review: `architect-reviewer` — 레이어 경계(infra 패키지 위치 적절성)

## 검증

- `./gradlew test` 전체 통과
- `S3KeyGeneratorTest` 4개 테스트 통과
- `QuestionSetServiceTest` 신규 키 포맷 assertion 통과
- `grep -rn "videos/" backend/src/main/java` → `QuestionSetService` 하드코딩 제거 확인
- 로컬 실행 (`./gradlew bootRun --args='--spring.profiles.active=local'`) 후 `POST /api/question-sets/{id}/upload-url` 호출 → 응답 `s3Key` 필드가 `interviews/raw/...` 포맷 확인
- `progress.md` Task 2 → Completed
