# Feature Specification: 종합 리포트 API

> **문서 ID**: PLAN-008
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P1 (High Impact)

---

## Overview

### 문제 정의

면접 종료 후 개별 피드백만으로는 사용자가 전체적인 성과를 파악하기 어렵다. 모든 피드백을 종합하여 전체 점수, 요약, 강점, 개선점을 한 눈에 볼 수 있는 리포트가 필요하다.

### 솔루션 요약

백엔드에서 피드백 리스트를 Claude API로 분석하여 종합 점수(0-100), 요약문, 강점 3개, 개선 포인트 3개를 생성한다. 이 리포트는 캐싱되어 첫 조회 시만 생성하고, 이후 조회는 저장된 결과를 반환한다.

### 우선순위 근거

| 기준 | 판단 |
|------|------|
| **Impact** | High - 사용자가 성과를 정량적으로 파악하는 핵심 요소 |
| **Effort** | Medium - 기존 Claude API + JPA 패턴 활용, 신규 엔티티 1개 |
| **결론** | **P1** - 피드백 생성 후 즉시 개발 |

---

## Backend API Specification

### API-1: 종합 리포트 조회

```
GET /api/v1/interviews/{interviewId}/report
```

**Path Parameters**:
- `interviewId` (Long): 인터뷰 ID

**Response (200 OK)**:
```json
{
  "success": true,
  "data": {
    "id": 1,
    "interviewId": 1,
    "overallScore": 75,
    "summary": "전반적으로 좋은 답변이었습니다. 기술적 이해도는 우수하나, 커뮤니케이션 속도를 조절할 필요가 있습니다.",
    "strengths": [
      "자료구조에 대한 깊이 있는 이해",
      "명확한 문제 분석 및 설명",
      "면접 진행 중 긍정적인 태도 유지"
    ],
    "improvements": [
      "말하는 속도를 늦추고 또렷하게 발음",
      "예시를 통한 구체적인 설명 필요",
      "시선 처리 및 자세 개선"
    ],
    "feedbackCount": 8
  },
  "message": null
}
```

**Error Cases**:
- 404: 존재하지 않는 인터뷰 ID
- 409: 피드백이 없어 리포트를 생성할 수 없음

---

## Data Model

### InterviewReport 엔티티

| 컬럼 | 타입 | 제약 | 설명 |
|------|------|------|------|
| id | Long (PK) | AUTO_INCREMENT | 리포트 ID |
| interview_id | Long (FK) | NOT NULL, UNIQUE | 인터뷰 참조 (1:1) |
| overall_score | INT | NOT NULL, 0-100 | 종합 점수 |
| summary | TEXT | NOT NULL | 요약문 (500자 이상) |
| strengths | TEXT | NOT NULL | 강점 3개 (파이프 `\|` 구분) |
| improvements | TEXT | NOT NULL | 개선점 3개 (파이프 `\|` 구분) |
| feedback_count | INT | NOT NULL | 피드백 개수 |
| created_at | DATETIME | NOT NULL | 생성일시 |

**관계**: Interview 1:1 InterviewReport (cascade delete)

### ReportResponse DTO

```typescript
interface ReportResponse {
  id: number
  interviewId: number
  overallScore: number
  summary: string
  strengths: string[]           // 배열로 변환
  improvements: string[]        // 배열로 변환
  feedbackCount: number
}
```

---

## Service Implementation

### ReportService

**주요 메서드**:

#### `getReport(Long interviewId): ReportResponse`

```java
public ReportResponse getReport(Long interviewId) {
    InterviewReport report = reportRepository.findByInterviewId(interviewId)
            .orElseGet(() -> generateAndSaveReport(interviewId));
    return ReportResponse.from(report);
}
```

**동작**:
1. `reportRepository.findByInterviewId(interviewId)` 조회
2. 존재하면 바로 반환 (캐싱)
3. 없으면 `generateAndSaveReport()` 호출하여 생성 후 저장

#### `generateAndSaveReport(Long interviewId): InterviewReport` (private)

```java
@Transactional
InterviewReport generateAndSaveReport(Long interviewId) {
    // 1. 인터뷰 조회
    Interview interview = interviewRepository.findById(interviewId)
            .orElseThrow(...);

    // 2. 피드백 조회
    List<Feedback> feedbacks = feedbackRepository
            .findByInterviewIdOrderByTimestampSeconds(interviewId);

    if (feedbacks.isEmpty()) {
        throw new BusinessException(
            HttpStatus.CONFLICT, "REPORT_001",
            "피드백이 없어 리포트를 생성할 수 없습니다."
        );
    }

    // 3. 피드백 요약 생성
    String feedbackSummary = feedbacks.stream()
            .map(f -> String.format("[%s/%s] %s",
                f.getCategory(), f.getSeverity(), f.getContent()))
            .collect(Collectors.joining("\n"));

    // 4. Claude API 호출
    GeneratedReport generated = aiClient.generateReport(feedbackSummary);

    // 5. 엔티티 생성 및 저장
    InterviewReport report = InterviewReport.builder()
            .interview(interview)
            .overallScore(generated.getOverallScore())
            .summary(generated.getSummary())
            .strengths(String.join("|", generated.getStrengths()))
            .improvements(String.join("|", generated.getImprovements()))
            .feedbackCount(feedbacks.size())
            .build();

    InterviewReport saved = reportRepository.save(report);
    log.info("리포트 생성 완료: interviewId={}, score={}",
             interviewId, saved.getOverallScore());

    return saved;
}
```

**에러 처리**:
- 인터뷰 없음 → 404 BusinessException
- 피드백 없음 → 409 BusinessException
- Claude API 실패 → 502 BusinessException

---

## Claude API Integration

### AiClient 메서드

```java
GeneratedReport generateReport(String feedbackSummary)
```

**입력**:
```
feedbackSummary:
"[VERBAL/INFO] 말하는 속도가 조금 빨랐습니다.
[NON_VERBAL/WARNING] 시선이 화면에서 벗어났습니다.
[CONTENT/SUGGESTION] 예시를 더 구체적으로 제시해야 합니다."
```

**Claude Prompt Design**:

```
System:
당신은 IT 면접 피드백을 분석하여 종합 리포트를 작성하는 전문가입니다.
주어진 피드백을 바탕으로 다음을 생성하세요:
1. 종합 점수 (0-100): 피드백의 긍정/부정 비율로 계산
2. 요약문: 전반적 평가 (3-4문장, 500자 이상)
3. 강점: 3개 (각 1문장)
4. 개선점: 3개 (각 1문장)

응답 형식: JSON

User:
다음 피드백을 분석하여 리포트를 생성하세요:

[VERBAL/INFO] 말하는 속도가 조금 빨랐습니다.
[NON_VERBAL/WARNING] 시선이 화면에서 벗어났습니다.
...
```

**Response Format**:
```json
{
  "overallScore": 72,
  "summary": "전반적으로 좋은 답변이었습니다...",
  "strengths": [
    "자료구조에 대한 깊이 있는 이해",
    "명확한 문제 분석 및 설명",
    "면접 진행 중 긍정적인 태도 유지"
  ],
  "improvements": [
    "말하는 속도를 늦추고 또렷하게 발음",
    "예시를 통한 구체적인 설명 필요",
    "시선 처리 및 자세 개선"
  ]
}
```

---

## Controller Implementation

### ReportController

```java
@RestController
@RequestMapping("/api/v1/interviews/{interviewId}/report")
@RequiredArgsConstructor
public class ReportController {

    private final ReportService reportService;

    @GetMapping
    public ResponseEntity<ApiResponse<ReportResponse>> getReport(
            @PathVariable Long interviewId) {
        ReportResponse response = reportService.getReport(interviewId);
        return ResponseEntity.ok(ApiResponse.ok(response));
    }
}
```

---

## Repository

### ReportRepository

```java
@Repository
public interface ReportRepository extends JpaRepository<InterviewReport, Long> {
    Optional<InterviewReport> findByInterviewId(Long interviewId);
}
```

---

## Technical Constraints

1. **캐싱 전략**: 첫 조회 시 생성, 이후 DB에서 조회 (write-once)
2. **Claude API 타임아웃**: 30초 (generateReport 메서드)
3. **트랜잭션**: @Transactional (readOnly=true) for getReport, @Transactional for generateAndSaveReport
4. **에러 응답**: GlobalExceptionHandler + BusinessException 패턴
5. **로깅**: reportService.generateAndSaveReport 완료 시 INFO 레벨

---

## 데이터 형식 (DB 저장)

**strengths, improvements 저장 형식**:
```
강점1|강점2|강점3
개선점1|개선점2|개선점3
```

**ReportResponse.from()에서 Split**:
```java
.strengths(List.of(report.getStrengths().split("\\|")))
.improvements(List.of(report.getImprovements().split("\\|")))
```

---

## 주요 구현 결정사항

1. **1:1 관계 설정**
   - Interview 1 ↔ 1 InterviewReport
   - 인터뷰 삭제 시 리포트 자동 삭제 (cascade)

2. **캐싱 전략**
   - 첫 조회: 피드백 → Claude API → DB 저장 (30초 소요)
   - 이후 조회: DB에서 바로 조회 (< 100ms)
   - 비용 절감: Claude API 호출 1회만

3. **피드백 요약 포맷**
   ```
   [카테고리/심각도] 내용
   ```
   - Claude가 구조화된 데이터 처리 용이

4. **점수 계산**
   - Claude가 생성 (비즈니스 로직은 AI에 위임)
   - 0-100 범위 보장 (프롬프트에 명시)
   - 정수형 INT 저장

5. **에러 처리**
   - 피드백 없음 → 409 Conflict (리포트 생성 불가)
   - 인터뷰 없음 → 404 Not Found
   - Claude API 실패 → 502 Bad Gateway

---

## 상태 전이

**리포트 생성 플로우**:

```
[사용자가 리포트 조회]
       ↓
[ReportService.getReport()]
       ↓
[DB 조회 (findByInterviewId)]
       ├─ 존재 → ReportResponse.from() → 반환
       └─ 없음 → generateAndSaveReport() 호출
              ↓
         [인터뷰 조회]
              ↓
         [피드백 조회]
              ├─ 없음 → 409 BusinessException
              └─ 있음 → Claude API 호출
                    ↓
              [GeneratedReport 받음]
                    ↓
              [InterviewReport 저장]
                    ↓
              [ReportResponse 반환]
```

---

## API 사용 예시 (Frontend)

**TanStack Query Hook**:
```typescript
export const useReport = (interviewId: string) => {
  return useQuery({
    queryKey: ['report', interviewId],
    queryFn: () =>
      apiClient.get<ApiResponse<InterviewReport>>(
        `/api/v1/interviews/${interviewId}/report`,
      ),
    enabled: !!interviewId,
  })
}
```

**페이지 컴포넌트**:
```typescript
const { data: response, isLoading } = useReport(id ?? '')
const report = response?.data

// 렌더링
<p>{report.overallScore}</p>
<p>{report.summary}</p>
{report.strengths.map((s) => <li>{s}</li>)}
{report.improvements.map((i) => <li>{i}</li>)}
```

---

## 테스트 전략 (QA 참고)

- [x] 피드백 있는 인터뷰 → 200 + 리포트 데이터
- [x] 피드백 없는 인터뷰 → 409 Conflict
- [x] 존재하지 않는 인터뷰 → 404 Not Found
- [x] 리포트 조회 2번 → 두 번째는 캐시 (DB 조회)
- [x] 점수 범위 → 0-100 범위 내
- [x] 강점/개선점 → 정확히 3개씩
- [x] Claude API 타임아웃 → 30초 이상 소요 시 502

---

## 의존성

| 의존성 | 상태 | 설명 |
|--------|------|------|
| Interview 엔티티 | 기존 | 인터뷰 참조 |
| Feedback 엔티티 | 기존 | 피드백 조회 |
| AiClient | 기존 | Claude API 호출 |
| GlobalExceptionHandler | 기존 | 에러 처리 |
| ApiResponse | 기존 | 응답 래퍼 |

---

## 완료 기준

- [x] InterviewReport 엔티티 (1:1 관계, cascade)
- [x] ReportResponse DTO (배열 변환)
- [x] ReportRepository (findByInterviewId)
- [x] ReportService (getReport, generateAndSaveReport)
- [x] ReportController (GET endpoint)
- [x] Claude API 통합 (generateReport)
- [x] 에러 처리 (404, 409, 502)
- [x] 단위 테스트 + 통합 테스트

---

## 완료된 작업

**Backend 구현 완료 (PR #8)**:
- ReportController.java
- ReportService.java
- ReportResponse.java
- InterviewReport.java (엔티티)
- ReportRepository.java
- AiClient.generateReport() (기존)
