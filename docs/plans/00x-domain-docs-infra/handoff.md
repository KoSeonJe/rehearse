# Handoff — 00x-domain-docs-infra

> **수명**: 단명. Phase C 완료 / 나머지 도메인 문서화 계획 확정 후 제거
> **작성 시점**: 2026-05-06 세션 종료 (Phase A 완료 + Phase B 완료 + Phase C 완료)
> **다음 세션**: 이 파일 먼저 읽음

---

## 현재 상태

- 진행: **Phase A 완료**, **Phase B 완료**, **Phase C 완료** (interview + question 도메인 정책 작성)
- 브랜치: `chore/claude-meta-setup` (마지막 commit `c794cf4`)
- 커밋: `docs(domain): question 도메인 정책 / 스키마 / API 흐름 문서화` (c794cf4)
- 빌드 / 테스트: 해당 없음 (문서만)
- PR: 미생성

### Phase A 산출물 (커밋됨)

- `docs/domain/AGENTS.md` — 폴더 구조 / 분할 룰 / 5단계 절차 / Read 강제 / 갱신 트리거 / backend agent 프롬프트 템플릿 / 메타인지 8 체크리스트 / 안티패턴
- `docs/domain/_templates/schema.md` — 테이블 목록 + (성격 / 컬럼 / 인덱스 / 불변정책 / 마이그레이션)
- `docs/domain/_templates/api-flow.md` — 입출력 / 흐름 / 분기 / 외부호출 / 저장 / 조건엣지 / 상태전이 / 관찰성
- `docs/domain/_templates/glossary.md` — 한↔영 매핑 + 약어

### Phase B 완료 (2026-05-06)

- `docs/domain/interview/` 8개 파일 작성 (schema.md / api 6개 / glossary.md, 총 1,191 lines)
- ❓TODO 17개 해결 + 5개 보류 유지 (의도적 미결)
- Issue #404 발행 — 보안/안정성/cleanup 8건 통합 Epic (https://github.com/KoSeonJe/rehearse/issues/404)
- **가설 정정**: `interview_session` / `interview_turn` 테이블 부재 확인 → 답변·턴 데이터는 question 도메인 관리, `InterviewRuntimeState` 는 Caffeine in-memory
- **템플릿 갱신**: `docs/domain/_templates/schema.md` / `api-flow.md` 에 "연관 의존성" 섹션 추가

### Phase C 완료 (2026-05-06)

- `docs/domain/question/` 6개 파일 작성 (schema.md / api 4개 / glossary.md, 총 1,101 lines)
- interview 도메인과의 의존 관계 명시 (턴 데이터 소유권 question, runtimeState 연동 지점)
- `scripts/sync-domain-docs.sh` 추가 — post-merge 자동 동기화 hook

---

## 다음 세션 시작점

**Phase A~C 완료** — resume / feedback / interview / question 4개 도메인 정책 문서화 완료.

### 문서화 현황 (2026-05-06 기준)

| 도메인 | 문서화 상태 | 위치 |
|--------|-----------|------|
| resume | 완료 | `docs/domain/resume/` |
| feedback | 완료 | `docs/domain/feedback/` |
| interview | 완료 | `docs/domain/interview/` |
| question | 완료 | `docs/domain/question/` |
| questionset | 미문서화 | `backend/.../domain/questionset/` |
| reviewbookmark | 미문서화 | `backend/.../domain/reviewbookmark/` |
| file | 미문서화 | `backend/.../domain/file/` |
| user | 미문서화 | `backend/.../domain/user/` |
| auth | 미문서화 | `backend/.../domain/auth/` |
| admin | 미문서화 | `backend/.../domain/admin/` |
| servicefeedback | 미문서화 | `backend/.../domain/servicefeedback/` |

### 다음 우선순위 (사용자 결정 필요)

1. **questionset** — QuestionSet / QuestionSetAnalysis / 분석 스케줄러 포함. interview·question·feedback 의존. 복잡도 높음.
2. **reviewbookmark** — ReviewBookmark. feedback 도메인과 연동.
3. 나머지 (file / user / auth / admin / servicefeedback) — 비즈니스 복잡도 낮음. 우선순위 후순위.

동일 절차: `docs/domain/AGENTS.md` §6 프롬프트 → backend agent → preview → 승인 → Write → 검증.

---

## 미해결 질문 / Blocker

| 항목 | 상태 | 결과 |
|------|------|------|
| 서브에이전트 미노출 원인 | **Resolved** | `tools:` 필드 8개 agent 추가로 해결 (commit c93c803) |
| Phase B 진행 시점 | **Completed** | interview 도메인 정책 작성 완료 (2026-05-06) |
| Phase C 진행 시점 | **Completed** | question 도메인 정책 작성 완료 (2026-05-06) |
| FE 비즈니스 정책 문서화 | 미결 | BE 파일럿 완료 → 별도 plan 으로 분리 예정 |
| 나머지 도메인 문서화 순서 | 미결 | questionset / reviewbookmark 우선 후보. 사용자 결정 필요 |

---

## 컨텍스트 메모

- **결정**: FE 정책은 별도 plan 으로 분리. interview BE 파일럿 결과 보고 진행 (사용자 답변 2026-05-06)
- **결정**: 루트 `AGENTS.md` / `CLAUDE.md` 의 `docs/domain/` 참조 룰 추가는 별도 plan (파일럿 검증 후)
- **결정**: 도메인별 작성은 1회 1 도메인. 메타인지 분산 방지
- **결정 (Phase B)**: Issue #404 발행 — interview 도메인 분석 중 발견된 보안/안정성/cleanup 8건 통합 Epic
- **정정 (Phase B)**: `interview_session` / `interview_turn` 테이블 실재하지 않음. 답변·턴 = question 도메인, runtimeState = Caffeine in-memory. 기존 handoff 가설 수정
- **함정**: backend agent 가 코드 추론으로 ❓TODO 채우면 안 됨. 모르면 ❓TODO 마킹 후 사용자에게
- **함정**: api 파일은 endpoint 1:1 X. 비즈니스 액션 단위 묶음
- **함정**: questionset 도메인 분석 시 AnalysisScheduler 가 interview / question / feedback 3개 도메인과 교차. 의존 방향 명시 필수
- **재사용**: `docs/plans/AGENTS.md` 톤 / 섹션 구성 차용. `docs/plans/_templates/` 7종 구조 참조
- **재사용**: `.claude/skills/create-tech-spec/SKILL.md` Step 6 (preview → confirm → write Blocking) 워크플로우
- **재사용**: `scripts/sync-domain-docs.sh` — post-merge 자동 동기화. 신규 도메인 추가 시 스크립트 내 도메인 목록 갱신 필요

---

## 참고 명령

```bash
# Phase B 시작 시 — interview 도메인 코드 / DDL 스카우트
ls backend/src/main/java/com/rehearse/api/domain/interview/
ls backend/src/main/resources/db/migration/ | grep -E "V[0-9]+__"

# 서브에이전트 frontmatter 확인
for f in .claude/agents/*.md; do echo "=== $f ==="; awk '/^---$/{c++; if(c==2)exit} c==1' "$f"; done

# Phase A 산출물 재확인
cat docs/domain/AGENTS.md
ls docs/domain/_templates/
```

### backend agent 프롬프트 (Phase B1 복붙용)

```
backend agent 로 docs/domain/interview/ 초안 작성.

분석 대상:
- backend/src/main/java/com/rehearse/api/domain/interview/
- backend/src/main/resources/db/migration/V*.sql 중 interview / interview_session / interview_turn 관련

템플릿: docs/domain/_templates/{schema,api-flow,glossary}.md strict 준수.
- schema.md: 테이블 4개 이하 단일 / 5개+ schema/{table}.md 분할
- api/{action}.md: endpoint 1:1 X. 비즈니스 액션 단위 묶음

출력: schema.md + api/{action}.md 다수 + (옵션) glossary.md.
코드 추론 불가 항목 = ❓TODO(사용자 확인) 마킹. 추론으로 채우지 말 것.

작성 후 preview 만 출력. 파일 직접 쓰지 말 것 (사용자 검토 후 Write).
```

---

업데이트: 2026-05-06 (Phase A 완료 / Phase B 완료 / Phase C 완료 — resume·feedback·interview·question 4개 도메인 정책 문서화 완결)
