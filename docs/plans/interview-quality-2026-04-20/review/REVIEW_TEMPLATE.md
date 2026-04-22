---
id: <category>-<plan>-<severity>-<slug>
severity: critical | major | minor
category: architecture | database | security | performance
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer | database-optimization | code-reviewer | ...
raised_at: YYYY-MM-DD
resolved_at: YYYY-MM-DD | null
status: open | in_progress | resolved | deferred
---

# <한 줄 제목>

## 문제

한두 문장으로 **무엇이** 잘못됐는지. 증상 레벨이 아니라 설계/코드 레벨 사실로.

## 원인

근본 원인. "왜 그렇게 구현됐나" — 설계 누락, 규약 부재, 라이브러리 오용 등. 파일:라인 레퍼런스 포함.

## 발생 상황

구체적 시나리오:
- **언제** 트리거되는가 (스레드 경합, 특정 호출 순서, 특정 데이터 분포 등)
- **누가** 영향을 받는가 (호출자 / 후속 plan / 운영 팀 / 사용자)
- **파장** — 데이터 유실? 성능 저하? 보안? 단순 혼란?

## 해결 방법

채택한 수정안. 코드/SQL 스니펫 포함.

```java
// Before
// ...

// After
// ...
```

대안 고려 + 기각 이유 1줄 (선택).

## 결과

- 변경 파일 목록 (`path/to/file:line`)
- 추가/수정된 테스트
- 검증 명령 (`./gradlew test`, `grep ...` 등)
- 후속 plan 에 미치는 영향 1-2줄
