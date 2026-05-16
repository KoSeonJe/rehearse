# Product Spec — Question Pool Admin

> **작성자**: 사용자 발화 + Issue #486 정리
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → 구현 agent 가 `tech-spec.md` 기준 구현 계획 승인 요청

---

## 문제 상황 (Problem)

- 현재 상태:
  - 표준 면접 질문 생성은 LLM API를 호출하기 전에 `question_pool`에서 `cache_key` 기준으로 사전 생성 질문을 먼저 조회한다.
  - 질문 풀은 `QuestionPoolService`가 선택/저장하고, 부족할 때만 LLM 생성 결과를 저장한다.
  - 운영자는 질문 풀 상태를 서비스 화면에서 확인하거나 직접 추가할 수 없다.
- 발생 증상:
  - 특정 `cache_key`에 어떤 질문/모범답안/category가 들어있는지 DB 직접 조회 없이는 알 수 없다.
  - 질문 품질 개선, 풀 부족 보강, 부실 질문 점검이 seed SQL 또는 수동 DB 작업에 의존한다.
- 사용자·운영 인지 채널:
  - 사용자 요청: "question pool에 있는 질문, 답변, 캐시키 등을 조회하고 질문을 추가할 수 있는 어드민 페이지"
  - GitHub Issue: #486

## 왜 해야 하는가 (Why)

- 사용자 임팩트:
  - 질문 풀이 부실하면 사용자는 반복적이거나 품질 낮은 질문을 받을 수 있다.
  - 풀 hit가 약하면 질문 생성 대기와 LLM 의존도가 커진다.
- 운영 / 시스템 임팩트:
  - 운영자가 질문 풀을 직접 확인하고 보강할 수 있어야 LLM 호출 비용과 질문 품질을 안정적으로 관리할 수 있다.
  - DB 직접 조작을 줄여 운영 실수를 줄인다.
- 외부 압력:
  - MVP 운영 중 질문 품질을 빠르게 조정해야 하는 요구.

## 해결 방향 (Approach)

- 핵심 접근:
  - 기존 `/admin/feedbacks`와 같은 비밀번호 기반 어드민 접근 패턴 안에서 question pool 전용 목록/생성 기능을 추가한다.
  - 기존 `question_pool` 스키마를 변경하지 않고 조회/추가만 제공한다.
- 대안 비교:
  - DB/seed SQL 직접 관리: 구현 비용은 낮지만 운영 실수와 추적성 문제가 크다.
  - 풀 조회/추가 어드민 UI: MVP에 필요한 최소 운영 기능을 제공한다.
- 단계 분리:
  - Phase 1: 목록 조회 + 필터 + 페이지네이션
  - Phase 2: 단건 질문 추가
  - Phase 3: FE 어드민 페이지 연결

## Evidence

- 도메인 문서: `docs/domain/question/schema.md`
- 흐름 문서: `docs/domain/question/api/pool-cache-management.md`
- 기존 엔티티: `backend/src/main/java/com/rehearse/api/domain/question/entity/QuestionPool.java`
- 기존 풀 서비스: `backend/src/main/java/com/rehearse/api/domain/question/service/QuestionPoolService.java`
- 기존 어드민 API 패턴: `backend/src/main/java/com/rehearse/api/domain/servicefeedback/controller/AdminFeedbackController.java`
- 기존 어드민 FE 패턴: `frontend/src/pages/admin-feedbacks-page.tsx`

## Goal

- [ ] 관리자가 `/admin/question-pool`에서 질문 풀 목록을 조회할 수 있다.
- [ ] 관리자가 `cacheKey`, category, 활성 상태, 검색어로 목록을 필터링할 수 있다.
- [ ] 관리자가 질문 본문, TTS 문구, category, 모범답안을 입력해 특정 `cacheKey`에 질문을 추가할 수 있다.
- [ ] 추가된 질문은 기존 표준 질문 생성의 pool hit 경로에서 사용할 수 있다.

## Non-Goals

- 질문 품질 자동 평가 — 운영자가 직접 보고 판단하는 기능이 목표다.
- 풀 hit ratio/메트릭 대시보드 — 관찰성 작업은 별도 Issue로 분리한다.
- AI 기반 자동 재생성 — 이번 작업은 수동 조회/추가 기능이다.

## 수용 기준 (Acceptance Criteria)

- [ ] Backend: 관리자용 question pool 목록 조회 API가 있다.
- [ ] Backend: 목록 조회는 페이지네이션과 `cacheKey`, category, `isActive`, 검색어 필터를 지원한다.
- [ ] Backend: 관리자용 question pool 생성 API가 있다.
- [ ] Backend: 생성 요청은 `cacheKey`와 질문 본문 필수값을 검증하고, `ttsContent`, category, `bestAnswer`를 저장할 수 있다.
- [ ] Backend: 어드민 인증은 기존 `/api/v1/admin/feedbacks`의 `X-Admin-Password` 운영 패턴과 일관되게 동작한다.
- [ ] Frontend: `/admin/question-pool` 어드민 페이지에서 질문 풀 목록을 조회할 수 있다.
- [ ] Frontend: 목록에서 cacheKey, category, 활성 상태, 질문, 모범답안을 확인할 수 있다.
- [ ] Frontend: 필터와 페이지네이션으로 원하는 풀 데이터를 찾을 수 있다.
- [ ] Frontend: 신규 질문 추가 폼을 제공하고 성공 후 목록을 갱신한다.
- [ ] Tests: backend controller/service/repository 레벨의 핵심 조회·생성 검증이 있다.
- [ ] Tests: frontend hook/page 또는 주요 interaction 테스트가 있다.

## 비스코프 (Don't)

- 질문 풀 자동 재학습, TTL, eviction 정책 수립
- AI 모델/프롬프트 버전 변경 시 기존 풀 invalidate 정책
- `(cache_key, content)` UNIQUE 또는 대규모 dedup 마이그레이션
- question pool bulk import/export
- 질문 수정/삭제/비활성화 UI
- 일반 사용자용 화면 노출
- DB 스키마 변경으로 `reference_type` 컬럼 추가

## 참고

- 관련 Issue: #486
- 관련 plan: `docs/plans/486-question-pool-admin/`
