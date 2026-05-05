# Handoff — 00x-domain-docs-infra

> **수명**: 단명. Phase B 파일럿 + (선택) FE 정책 plan 결정 후 제거
> **작성 시점**: 2026-05-06 세션 종료 (Phase A 완료 + Phase B 보류)
> **다음 세션**: 이 파일 먼저 읽음

---

## 현재 상태

- 진행: **Phase A 완료**, Phase B (interview 파일럿) 미시작
- 브랜치: `chore/claude-meta-setup` (마지막 commit `5659c4d`)
- 커밋: `docs(domain): 도메인 정책 문서화 인프라 + 템플릿 3종 추가` (5659c4d)
- 빌드 / 테스트: 해당 없음 (문서만)
- PR: 미생성

### Phase A 산출물 (커밋됨)

- `docs/domain/AGENTS.md` — 폴더 구조 / 분할 룰 / 5단계 절차 / Read 강제 / 갱신 트리거 / backend agent 프롬프트 템플릿 / 메타인지 8 체크리스트 / 안티패턴
- `docs/domain/_templates/schema.md` — 테이블 목록 + (성격 / 컬럼 / 인덱스 / 불변정책 / 마이그레이션)
- `docs/domain/_templates/api-flow.md` — 입출력 / 흐름 / 분기 / 외부호출 / 저장 / 조건엣지 / 상태전이 / 관찰성
- `docs/domain/_templates/glossary.md` — 한↔영 매핑 + 약어

---

## 다음 세션 시작점

**1순위**: 서브에이전트 등록 이슈 해결 (Phase B 비용 영향).

### Blocker — 서브에이전트 미노출

`.claude/agents/` 10개 파일 중 2개만 노출 (`prompt-engineer`, `ui-ux-designer`). 나머지 8개 (`backend`, `frontend`, `git-manager`, `docs-manager`, `debugger-backend`, `debugger-frontend`, `code-reviewer-backend`, `code-reviewer-frontend`) 미노출.

**가설** (확신 70%):
- 노출되는 2개 = `description: "..."` quoted / 단일줄 + `tools:` 필드 명시
- 미노출 8개 = `description: |` YAML block scalar + `tools:` 필드 부재
- harness 가 `description: |` block + `<example>` XML 태그 조합 파싱 실패 OR `tools:` 부재 시 거부

**검증 / 수정 옵션**:
1. (옵션 A) `tools:` 필드 추가 후 재시작
2. (옵션 B) `description: |` → `description: "...\n<example>...\n</example>"` quoted 로 변경
3. (옵션 C) 한 파일만 변경 → 재시작 → 노출 확인 → 나머지 일괄 적용

**다음 세션 첫 명령**:
```bash
# 한 agent 만 수정해 검증 (예: git-manager)
# tools 필드 추가 시도
```

### Phase B (서브에이전트 등록 후 진행)

`.claude/rules/` plan 본문 §B1~B4. interview 도메인 파일럿:

- B1. backend agent 호출 (`docs/domain/AGENTS.md` §6 프롬프트 복붙)
  - 분석: `backend/src/main/java/com/rehearse/api/domain/interview/` (controller / service / entity / repository / dto / event / exception)
  - Flyway: `V*.sql` 중 interview / interview_session / interview_turn 관련 (V10, V17, V25, V29, V31, V40, V41, V43 등)
  - 출력: schema.md + api/{action}.md 다수 (endpoint 9 → 액션 단위 5-7개로 묶음 가능)
  - ❓TODO 마킹 = 코드 추론 불가 항목

- B2. 메타인지 보완 (`docs/domain/AGENTS.md` §7 체크리스트 8항목)
  - intent 분기 룰 / 모호도 임계값 / AI fallback / runtimeState / session.status 전이 / 동시 세션 제약 / resume 삭제 fallback

- B3. preview → 사용자 승인 → Write

- B4. 검증 (Flyway 컬럼 일치 / endpoint 9 100% 커버 / ❓TODO 0건 / 조건엣지 빈 row 0)

---

## 미해결 질문 / Blocker

| 항목 | 옵션 | 추천 |
|------|------|------|
| 서브에이전트 미노출 원인 | A) tools 필드 추가 / B) description quoted 변환 / C) 한 파일 검증 후 일괄 | C — 1개로 가설 검증 후 일괄 |
| Phase B 진행 시점 | A) agent 등록 수정 후 / B) general-purpose 로 우회 / C) main 세션 직접 | A — agent 비용 / 컨벤션 Read 강제 보장 |
| FE 비즈니스 정책 문서화 | 결정됨: BE 파일럿 후 별도 plan | — |

---

## 컨텍스트 메모

- **결정**: FE 정책은 별도 plan 으로 분리. interview BE 파일럿 결과 보고 진행 (사용자 답변 2026-05-06)
- **결정**: 루트 `AGENTS.md` / `CLAUDE.md` 의 `docs/domain/` 참조 룰 추가는 별도 plan (파일럿 검증 후)
- **결정**: 도메인별 작성은 1회 1 도메인. 메타인지 분산 방지
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

업데이트: 2026-05-06 (Phase A 완료 / Phase B 미시작 / agent 등록 blocker)
