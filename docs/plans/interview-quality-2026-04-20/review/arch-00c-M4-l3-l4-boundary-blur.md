---
id: arch-00c-M4-l3-l4-boundary-blur
severity: major
category: architecture
plan: plan-00c-session-state-persistence
reviewer: architect-reviewer
raised_at: 2026-04-21
resolved_at: 2026-04-21
status: resolved
---

# L3 / L4 경계 결정 미문서화

## 문제

`STATE_DESIGN.md` 는 L3 (런타임 상태) 와 L4 (계산 캐시) 를 별도 계층으로 정의했으나, 구현은 L4 에 속한 `resumeSkeletonCache` 를 L3 POJO (`InterviewRuntimeState`) 의 필드로 인라인. 설계 문서와 코드 사이 경계가 흐려짐.

## 원인

L3, L4 가 동일 TTL (2h) + 동일 저장소 (Caffeine) 를 공유하므로 코드에서는 자연스럽게 합쳐졌지만, 그 결정이 STATE_DESIGN 에 명시되지 않음. 향후 독자가 "왜 L4 전용 캐시가 없지?" 라고 의문을 갖게 됨.

## 발생 상황

- **언제**: 새 개발자 온보딩, plan-05 구현자가 ResumeSkeleton 저장 경로 탐색 시
- **누가**: 후속 plan 구현자, 신규 온보딩
- **파장**: 구현 시행착오, 설계 의도와 어긋난 별도 캐시 인스턴스 중복 생성 위험

## 해결 방법

`STATE_DESIGN.md` 에 Decision 추가:

```markdown
### D6. L4 계산 캐시는 L3 POJO 인라인 필드로 관리

- 독립 Caffeine 인스턴스를 추가로 생성하지 않는다
- `InterviewRuntimeState` 의 필드 (`resumeSkeletonCache` 등) 에 인라인
- 사용자 저장 동의 `false` 일 때만 L4 활성화 (true 일 때는 L2 영속 저장)
- **이유**: L3/L4 가 동일 TTL (2h idle) + 동일 세션 scope + 동일 저장소(Caffeine) 를 공유. 별도 캐시는 메모리 + 관리 오버헤드만 증가
- **제약**: multi-node 확장 시 L3/L4 동시 이관 (동일 lifecycle)
```

## 결과

- 수정: `docs/plans/interview-quality-2026-04-20/STATE_DESIGN.md` — D6 추가
- 후속 plan 이 "별도 L4 캐시 만들기" 시행착오 없이 `state.setResumeSkeletonCache(...)` 로 직행
- 구현 변경은 없음 (코드는 이미 이 방향)
