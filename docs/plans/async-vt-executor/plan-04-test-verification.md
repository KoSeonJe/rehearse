# Plan 04: 테스트 및 검증

> 상태: Draft
> 작성일: 2026-03-27

## Why

글로벌 VT → 선택적 VT 전환 후, 기존 기능의 정상 동작을 검증한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| 기존 테스트 파일들 | 변경 없음 (통과 확인만) |

## 상세

### 검증 항목

1. **전체 테스트 통과**
   ```bash
   cd backend && ./gradlew test
   ```

2. **스레드 모델 확인**
   - Tomcat 요청: `Thread.currentThread().isVirtual() == false`
   - `@Async("vtExecutor")` 이벤트 핸들러: `Thread.currentThread().isVirtual() == true`

3. **E2E 흐름 확인** (수동)
   - 면접 생성 → 201 즉시 반환 → 질문 생성 백그라운드 완료
   - 후속질문 생성 → 동기 응답 (기존과 동일)

4. **pinning 모니터링**
   - `bootRun` JVM args `-Djdk.tracePinnedThreads=short` 유지
   - `@Async` VT → `startGeneration()`/`saveResults()` (`@Transactional`) 경로에서 pinning 없는지 확인
   - HikariCP 6.2.1이 `ReentrantLock` 사용하므로 pinning 발생하지 않아야 함

## 담당 에이전트

- Implement: `qa` — 테스트 실행 + 결과 리포트
- Review: `architect-reviewer` — 스레드 모델 전환 완전성 검증

## 검증

- `./gradlew test` 전체 통과
- pinning 경고 로그 없음
- `progress.md` 상태 업데이트 (Task 4 → Completed)
