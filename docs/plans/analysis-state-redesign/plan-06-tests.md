# Plan 06: 테스트 보강

> 상태: Draft
> 작성일: 2026-03-24

## Why

상태 모델 전체가 변경되므로 기존 테스트가 대부분 깨진다. 깨진 테스트를 수정하고, 신규 로직(completeAnalysis, PARTIAL, convertStatus)에 대한 테스트를 추가한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/test/.../questionset/entity/QuestionSetAnalysisTest.java` | **신규** — completeAnalysis(), isFullyReady(), 상태 전이 테스트 |
| `backend/src/test/.../questionset/entity/AnalysisStatusTest.java` | **신규** — canTransitionTo() 전 케이스 (PARTIAL 포함) |
| `backend/src/test/.../questionset/entity/ConvertStatusTest.java` | **신규** — canTransitionTo() 전 케이스 |
| `backend/src/test/.../questionset/service/InternalQuestionSetServiceTest.java` | AnalysisProgress 참조 제거. saveFeedback 테스트에 isVerbalCompleted/isNonverbalCompleted 검증 추가. retryAnalysis PARTIAL 케이스 추가 |
| `backend/src/test/.../questionset/service/AnalysisSchedulerTest.java` | detectFileConvertingZombies 테스트 삭제 → detectConvertZombies 테스트 추가 |
| `backend/src/test/.../questionset/controller/InternalQuestionSetControllerTest.java` | updateProgress 테스트 수정 (AnalysisStatus 기반). updateConvertStatus 테스트 추가 |
| `backend/src/test/.../file/service/InternalFileServiceTest.java` | CONVERTING/CONVERTED 관련 테스트 제거 |

## 상세

### QuestionSetAnalysisTest 핵심 케이스

```java
@Test
void completeAnalysis_둘다성공_COMPLETED() {
    analysis.updateAnalysisStatus(AnalysisStatus.EXTRACTING);
    analysis.updateAnalysisStatus(AnalysisStatus.ANALYZING);
    analysis.updateAnalysisStatus(AnalysisStatus.FINALIZING);
    analysis.completeAnalysis(true, true);
    assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.COMPLETED);
}

@Test
void completeAnalysis_언어만성공_PARTIAL() {
    // ... FINALIZING까지 전이 ...
    analysis.completeAnalysis(true, false);
    assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.PARTIAL);
    assertThat(analysis.isVerbalCompleted()).isTrue();
    assertThat(analysis.isNonverbalCompleted()).isFalse();
}

@Test
void completeAnalysis_둘다실패_FAILED() {
    // ...
    analysis.completeAnalysis(false, false);
    assertThat(analysis.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
}

@Test
void isFullyReady_분석완료_변환완료() {
    // ... COMPLETED + ConvertStatus.COMPLETED
    assertThat(analysis.isFullyReady()).isTrue();
}

@Test
void isFullyReady_분석완료_변환미완료() {
    // ... COMPLETED + ConvertStatus.PROCESSING
    assertThat(analysis.isFullyReady()).isFalse();
}

@Test
void isFullyReady_PARTIAL_변환완료() {
    // ... PARTIAL + ConvertStatus.COMPLETED
    assertThat(analysis.isFullyReady()).isTrue();
}
```

## 담당 에이전트

- Implement: `test-engineer` — 테스트 작성 및 수정
- Review: `code-reviewer` — 테스트 커버리지, 엣지 케이스

## 검증

- 전체 테스트 통과 (`./gradlew test`)
- 신규 테스트: QuestionSetAnalysis 관련 최소 10개
- 기존 깨진 테스트 0개
- `progress.md` 상태 업데이트 (Task 6 → Completed)
