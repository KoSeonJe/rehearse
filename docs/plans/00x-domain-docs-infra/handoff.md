# Handoff — 00x-domain-docs-infra

> **수명**: 단명. Phase B 파일럿 + (선택) FE 정책 plan 결정 후 제거
> **작성 시점**: 2026-05-06 세션 종료 (Phase A 완료 + Phase B 보류)
> **다음 세션**: 이 파일 먼저 읽음

---

## 현재 상태

- 진행: **Phase A 완료**, **Phase B 완료** (interview 도메인 정책 작성)
- 브랜치: `chore/claude-meta-setup` (마지막 commit `5659c4d`)
- 커밋: `docs(domain): 도메인 정책 문서화 인프라 + 템플릿 3종 추가` (5659c4d)
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

---

## 다음 세션 시작점

**1순위**: Phase C — question 도메인 정책 작성.

### Phase C (question 도메인)

interview 파일럿 완료 기준으로 동일 절차 적용:

- C1. backend agent 호출 (`docs/domain/AGENTS.md` §6 프롬프트 복붙)
  - 분석: `backend/src/main/java/com/rehearse/api/domain/question/` (controller / service / entity / repository / dto / event / exception)
  - Flyway: `V*.sql` 중 question / interview_turn / answer 관련
  - 출력: schema.md + api/{action}.md + (옵션) glossary.md
  - ❓TODO 마킹 = 코드 추론 불가 항목 (interview 파일럿에서 정정된 가설 참조)

- C2. 메타인지 보완 (`docs/domain/AGENTS.md` §7 체크리스트 8항목)
  - 턴 데이터가 question 도메인에 있음 재확인 / runtimeState 연동 지점 명시

- C3. preview → 사용자 승인 → Write

- C4. 검증 (Flyway 컬럼 일치 / endpoint 100% 커버 / ❓TODO 보류 5개 제외 나머지 0건 / 조건엣지 빈 row 0)

**참고**: Phase B 정정 가설 (`interview_session` / `interview_turn` 부재) 이 question 도메인 분석에 영향. 시작 전 `docs/domain/interview/schema.md` 재확인 권장.

---

## 미해결 질문 / Blocker

| 항목 | 상태 | 결과 |
|------|------|------|
| 서브에이전트 미노출 원인 | **Resolved** | `tools:` 필드 8개 agent 추가로 해결 (commit c93c803) |
| Phase B 진행 시점 | **Completed** | interview 도메인 정책 작성 완료 (2026-05-06) |
| FE 비즈니스 정책 문서화 | 미결 | BE 파일럿 완료 → 별도 plan 으로 분리 예정 |

---

## 컨텍스트 메모

- **결정**: FE 정책은 별도 plan 으로 분리. interview BE 파일럿 결과 보고 진행 (사용자 답변 2026-05-06)
- **결정**: 루트 `AGENTS.md` / `CLAUDE.md` 의 `docs/domain/` 참조 룰 추가는 별도 plan (파일럿 검증 후)
- **결정**: 도메인별 작성은 1회 1 도메인. 메타인지 분산 방지
- **결정 (Phase B)**: Issue #404 발행 — interview 도메인 분석 중 발견된 보안/안정성/cleanup 8건 통합 Epic
- **정정 (Phase B)**: `interview_session` / `interview_turn` 테이블 실재하지 않음. 답변·턴 = question 도메인, runtimeState = Caffeine in-memory. 기존 handoff 가설 수정
- **함정**: backend agent 가 코드 추론으로 ❓TODO 채우면 안 됨. 모르면 ❓TODO 마킹 후 사용자에게
- **함정**: api 파일은 endpoint 1:1 X. 비즈니스 액션 단위 묶음
- **재사용**: `docs/plans/AGENTS.md` 톤 / 섹션 구성 차용. `docs/plans/_templates/` 7종 구조 참조
- **재사용**: `.claude/skills/create-tech-spec/SKILL.md` Step 6 (preview → confirm → write Blocking) 워크플로우

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

업데이트: 2026-05-06 (Phase A 완료 / Phase B 완료 / Phase C = question 도메인 예정)
