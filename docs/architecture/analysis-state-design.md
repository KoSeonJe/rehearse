# 분석 파이프라인 상태 관리 설계

> 작성일: 2026-03-25
> 관련 코드: `QuestionSetAnalysis`, `AnalysisStatus`, `ConvertStatus`

## 왜 상태 설계가 중요한가

면접 분석 파이프라인은 **비동기 + 분산 + 병렬** 시스템이다.

- S3 업로드 → EventBridge → Analysis Lambda + Convert Lambda **동시 실행**
- Lambda는 외부 AI API(Gemini, Vision, Whisper)에 의존 — 부분 실패 가능
- Lambda 실행 시간이 수 분 — 중간에 죽거나 타임아웃 가능
- 사용자는 FE에서 실시간 진행률을 폴링

이 환경에서 상태 관리가 부실하면:

1. **부분 실패를 모른다** — Vision만 실패해도 COMPLETED로 저장되어 비언어 피드백이 빈 채로 제공
2. **"전체 완료" 판정이 어렵다** — 분석 상태는 QuestionSet에, 변환 상태는 FileMetadata에 있어서 2개 엔티티를 조합해야 함
3. **재시도가 불가능하다** — 어디서 실패했는지 모르니 전체를 다시 돌려야 함
4. **좀비가 영구 방치된다** — 중간 상태에서 Lambda가 죽으면 감지할 수 없음

이 설계는 **"어떤 실패든 감지하고, 가능한 만큼 복구하고, 사용자에게 정확한 상태를 보여준다"** 를 목표로 한다.

---

## 핵심 구조: QuestionSetAnalysis

분석/변환 상태를 **하나의 엔티티**로 통합 관리한다.

```
QuestionSet (1:1) ── QuestionSetAnalysis
                      ├── analysisStatus        분석 진행 상태 (9값)
                      ├── convertStatus         영상 변환 상태 (4값)
                      ├── isVerbalCompleted      언어 분석 성공 여부
                      ├── isNonverbalCompleted   비언어 분석 성공 여부
                      ├── failureReason / failureDetail
                      ├── convertFailureReason
                      └── @Version              낙관적 잠금 (동시성 제어)

FileMetadata
  └── status → PENDING / UPLOADED / FAILED (순수 업로드 상태만)
```

**설계 원칙**:

- 분석과 관련된 모든 상태는 QuestionSetAnalysis에 집중
- FileMetadata는 "파일이 S3에 올라갔는가?"만 관리
- `isFullyReady()` 하나로 "피드백 뷰어에 진입 가능한가?" 판정

---

## 전체 파이프라인 흐름

```
[사용자가 답변 저장]
     │
     ▼
QuestionSetAnalysis 생성 (PENDING)
     │
     ▼  FE → S3 presigned URL 업로드
     │
  PENDING_UPLOAD
     │
     ▼  S3 PutObject → EventBridge 트리거
     │
     ├───────────────────────┐
     ▼                       ▼
[Analysis Lambda]      [Convert Lambda]
     │                       │
  EXTRACTING              PROCESSING
  (FFmpeg 추출)           (WebM → MP4)
     │                       │
  ANALYZING                  │
  (Gemini + Vision)          │
     │                       │
  FINALIZING                 │
  (피드백 생성)               │
     │                       │
  COMPLETED / PARTIAL / FAILED    COMPLETED / FAILED
     │                       │
     └───────────┬───────────┘
                 ▼
          isFullyReady()?
     (분석 OK + 변환 OK = true)
                 │
                 ▼
     InterviewCompletionService
     (모든 질문세트 완료 → 면접 COMPLETED)
```

Analysis Lambda와 Convert Lambda는 **동시에 병렬 실행**된다.
같은 QuestionSetAnalysis row를 동시에 업데이트하므로 `@Version` 낙관적 잠금으로 충돌을 처리한다.

---

## 상태 전이 테이블

### AnalysisStatus (9값)

```
PENDING ──→ PENDING_UPLOAD ──→ EXTRACTING ──→ ANALYZING ──→ FINALIZING
  │                                                            │
  ├──→ SKIPPED                                                 ├──→ COMPLETED
  │                                                            ├──→ PARTIAL
  └──→ FAILED                                                  └──→ FAILED
         │
         ├──→ EXTRACTING  (재시도)
         └──→ COMPLETED   (좀비 FAILED 후 Lambda 뒤늦은 성공)

PARTIAL ──→ EXTRACTING (재시도)
PARTIAL ──→ FAILED
COMPLETED ──→ FAILED
```

| 상태 | 설정 주체 | 의미 | 전이 가능 |
|------|-----------|------|-----------|
| PENDING | BE (생성 시) | 초기 상태 | PENDING_UPLOAD, SKIPPED, FAILED |
| PENDING_UPLOAD | BE (답변 저장 시) | S3 업로드 대기 | EXTRACTING, FAILED |
| EXTRACTING | Lambda | FFmpeg 오디오/프레임 추출 중 | ANALYZING, FAILED |
| ANALYZING | Lambda | Gemini 음성 + Vision 비언어 분석 중 | FINALIZING, FAILED |
| FINALIZING | Lambda | 종합 피드백 생성 중 | COMPLETED, PARTIAL, FAILED |
| COMPLETED | Lambda | 언어 + 비언어 모두 성공 | FAILED |
| PARTIAL | Lambda | 한쪽만 성공 | EXTRACTING, FAILED |
| FAILED | Lambda / 좀비 스케줄러 | 전체 실패 | EXTRACTING, COMPLETED |
| SKIPPED | BE | 녹화 안 한 질문세트 | (종료) |

### ConvertStatus (4값)

```
PENDING ──→ PROCESSING ──→ COMPLETED ──→ FAILED
                │                         │
                └──→ FAILED ──→ PROCESSING ┘
```

| 상태 | 의미 | 전이 가능 |
|------|------|-----------|
| PENDING | 변환 대기 | PROCESSING, FAILED |
| PROCESSING | MediaConvert 실행 중 | COMPLETED, FAILED |
| COMPLETED | 변환 완료 | FAILED |
| FAILED | 변환 실패 | PROCESSING |

### FileStatus (3값, 축소됨)

| 상태 | 의미 | 전이 가능 |
|------|------|-----------|
| PENDING | 초기 상태 | UPLOADED, FAILED |
| UPLOADED | S3 업로드 완료 | FAILED |
| FAILED | 업로드 실패 | UPLOADED |

---

## 부분 실패 감지 (PARTIAL)

### 문제

Gemini(언어)와 Vision(비언어)은 독립적인 AI 서비스다. 한쪽만 장애가 나면 기존에는 전체를 COMPLETED로 처리하고 빈 피드백을 제공했다.

### 해결

`isVerbalCompleted` / `isNonverbalCompleted` boolean 필드로 개별 결과를 추적한다.

```java
public void completeAnalysis(boolean verbalCompleted, boolean nonverbalCompleted) {
    this.isVerbalCompleted = verbalCompleted;
    this.isNonverbalCompleted = nonverbalCompleted;

    if (둘 다 성공)      → COMPLETED
    else if (둘 다 실패)  → FAILED
    else                 → PARTIAL  (한쪽만 성공)
}
```

Lambda에서의 판별:

```python
verbal_ok = any(r is not None for r in gemini_results)
nonverbal_ok = any(r is not None for r in vision_results)

save_feedback(..., isVerbalCompleted=verbal_ok, isNonverbalCompleted=nonverbal_ok)
```

### PARTIAL 시나리오

| 시나리오 | verbal | nonverbal | 결과 |
|----------|--------|-----------|------|
| 정상 | true | true | COMPLETED |
| Vision API 장애 | true | false | PARTIAL |
| Gemini + 폴백 성공, Vision 실패 | true | false | PARTIAL |
| Gemini + 폴백 실패, Vision 성공 | false | true | PARTIAL |
| 전부 실패 | false | false | FAILED |

---

## 실패 대응 전략: 3단계

### 1단계: Lambda 내부 자동 재시도

API 장애 시 **모델 단위**로 1회 재시도한다 (개별 답변이 아닌 전체 모델).

```
Gemini 전체 실패?
  → 2초 대기 → 재시도
    → 성공: 계속 진행
    → 실패: Whisper + GPT-4o 폴백 경로로 전환

Vision 전체 실패?
  → 2초 대기 → 재시도
    → 성공: COMPLETED
    → 실패: PARTIAL로 저장 (언어 피드백만 제공)
```

### 2단계: 좀비 스케줄러 (자동 감지)

Lambda가 죽거나 타임아웃되면 상태가 중간에 멈춘다. 스케줄러가 주기적으로 감지하여 FAILED로 마킹한다.

| 스케줄러 | 주기 | 대상 상태 | 타임아웃 | 처리 |
|----------|------|-----------|---------|------|
| detectAnalysisZombies | 60초 | EXTRACTING / ANALYZING / FINALIZING | 10분 | → FAILED |
| detectConvertZombies | 60초 | convertStatus = PROCESSING | 10분 | → FAILED |
| detectPendingUploadZombies | 5분 | PENDING_UPLOAD | 30분 | → FAILED |
| detectUploadZombies | 5분 | FileMetadata.PENDING | 30분 | → FAILED |

FAILED 처리 후 사용자가 FE에서 재시도 가능.

### 3단계: 사용자 수동 재시도 (retryAnalysis API)

FE에서 "재시도" 버튼을 누르면:

```java
public void retryAnalysis(Long questionSetId) {
    // FAILED/PARTIAL만 재시도 가능
    if (status != FAILED && status != PARTIAL)
        throw BusinessException;

    // 양쪽 결과 리셋 (전체 재분석)
    analysis.resetVerbalResult();
    analysis.resetNonverbalResult();
    analysis.updateAnalysisStatus(EXTRACTING);

    // TX 커밋 후 S3 재트리거 → EventBridge → Lambda 재실행
    afterCommit(() -> s3Service.retriggerUploadEvent(s3Key));
}
```

```
FAILED/PARTIAL → 리셋 → EXTRACTING → S3 재트리거 → Lambda 재실행
  → ANALYZING → FINALIZING → COMPLETED / PARTIAL / FAILED
```

---

## 동시성 제어

### 문제

Analysis Lambda와 Convert Lambda가 동시에 같은 QuestionSetAnalysis row를 업데이트한다.

```
[Analysis Lambda]                [Convert Lambda]
analysisStatus = COMPLETED       convertStatus = COMPLETED
        │                                │
        └────── 동시 UPDATE ──────────────┘
                    ↓
            한쪽이 version 충돌
```

### 해결: @Version + @Retryable

```java
@Entity
public class QuestionSetAnalysis {
    @Version
    private Long version;  // 낙관적 잠금
}

// 서비스 메서드에 재시도 적용
@Retryable(retryFor = ObjectOptimisticLockingFailureException.class,
           maxAttempts = 3, backoff = @Backoff(delay = 100))
@Transactional
public void updateProgress(...) { ... }
```

```java
// @EnableRetry 순서 보장: Retry가 TX 바깥에서 감싸야 함
@EnableRetry(order = Ordered.HIGHEST_PRECEDENCE)
```

**동작 흐름**:
```
[시도 1] TX 시작 → UPDATE → version 충돌 → TX 롤백 → 예외 전파
[시도 2] 100ms 대기 → 새 TX → UPDATE → 성공 → TX 커밋 ✅
```

### 좀비 스케줄러 vs Lambda 경합

```
1. Lambda가 느리게 실행 중 (9분 59초)
2. 좀비 스케줄러: 10분 지남 → FAILED 처리
3. Lambda: 1초 뒤 완료 → save_feedback 호출
4. completeAnalysis(true, true) → FAILED → COMPLETED 전이 (허용됨!)
5. 결과: 정상 완료 ✅
```

좀비 스케줄러도 `ObjectOptimisticLockingFailureException`을 catch하여 동시 업데이트 시 스킵한다.

---

## "전체 완료" 판정

```java
public boolean isFullyReady() {
    boolean analysisOk = analysisStatus == COMPLETED || analysisStatus == PARTIAL;
    boolean convertOk = convertStatus == ConvertStatus.COMPLETED;
    return analysisOk && convertOk;
}
```

- PARTIAL도 "분석 완료"로 취급 — 일부 피드백이라도 볼 수 있음
- 분석 + 변환 모두 완료되어야 FE에서 피드백 뷰어 진입 가능

### 면접 완료 판정 (InterviewCompletionService)

```
모든 질문세트가 COMPLETED/PARTIAL/SKIPPED
  + COMPLETED 또는 PARTIAL이 1개 이상
  → 면접 상태 = COMPLETED
```

30초 간격으로 IN_PROGRESS 면접을 스캔하여 자동 완료 처리.

---

## FE 상태 처리

```typescript
// 터미널 상태 = 폴링 중단
const isTerminal = (s) =>
  ['COMPLETED', 'PARTIAL', 'FAILED', 'SKIPPED'].includes(s?.analysisStatus)

// 재시도 가능 = FAILED, PARTIAL, 또는 convertStatus FAILED
const hasFailed = statuses.some(s =>
  s?.analysisStatus === 'FAILED' ||
  s?.analysisStatus === 'PARTIAL' ||
  s?.convertStatus === 'FAILED'
)

// 피드백 조회 가능 = COMPLETED 또는 PARTIAL
const shouldFetchFeedback =
  analysisStatus === 'COMPLETED' || analysisStatus === 'PARTIAL'
```

| 상태 | UI |
|------|-----|
| EXTRACTING / ANALYZING / FINALIZING | 프로그레스 바 + "AI가 영상을 분석 중..." |
| COMPLETED | "분석 완료!" + 피드백 보러가기 |
| PARTIAL | "일부 분석 완료" + 재시도 버튼 + 완료된 결과 보기 |
| FAILED | "일부 피드백 생성 실패" + 재시도 버튼 |

---

## 요약

| 관심사 | 설계 | 효과 |
|--------|------|------|
| 상태 통합 | QuestionSetAnalysis 단일 엔티티 | 2개 엔티티 조합 → 1개 메서드 판정 |
| 부분 실패 | isVerbalCompleted / isNonverbalCompleted | 실패 위치 식별 + PARTIAL 상태 |
| 자동 복구 | Lambda 내부 모델 단위 재시도 + 폴백 | 일시적 API 장애 자동 극복 |
| 좀비 감지 | 4개 스케줄러 (60초~5분 주기) | 중간 상태 방치 방지 |
| 수동 재시도 | retryAnalysis API + S3 재트리거 | FAILED/PARTIAL에서 전체 재분석 |
| 동시성 | @Version + @Retryable + 프록시 순서 | 병렬 Lambda 충돌 안전 처리 |
| 상태 전이 안전 | canTransitionTo() 패턴 | 잘못된 상태 변경 차단 |
