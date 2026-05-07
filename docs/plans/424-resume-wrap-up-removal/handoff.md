# Handoff — 424-resume-wrap-up-removal

> **수명**: 단명 (plan 종료 시 제거)
> **작성 시점**: 세션 종료 (2026-05-07)
> **다음 세션**: plan 폴더 진입 시 이 파일 먼저 읽음

---

## 현재 상태

- **spec 작성**: 완료 (product-spec / tech-spec / implement-be.md / implement-fe.md 4개)
- **리뷰 이력**:
  - product spec: spec-reviewer-product 1차 (P1 6건) → 반영 → PASS
  - tech spec: spec-reviewer-tech 1차 (P0 5 / P1 7 / P2 3) → 사용자 결정 + 전면 재작성 → 2차 (P1 4건) → 반영 → 3차 PASS
  - implement: 작성 완료 (사용자 명시 승인 후 /create-implement-plan 진입)
- **코드 변경**: 없음 — 구현 미시작
- **브랜치**: 미생성 (423 머지 후 `feat/424-resume-wrap-up-removal` 생성 예정)
- **빌드 / 테스트**: 해당 없음

## 다음 세션 시작점 (Blocking)

### 1순위 — plan 423 선행 머지 확인

```bash
gh pr view --repo KoSeonJe/devlens   # 423 관련 PR 상태 확인
```

- 423 PR 미머지 → STOP. 머지 대기.
- 423 머지 확인 → `feat/424-resume-wrap-up-removal` 브랜치 생성 + develop rebase.

### 2순위 — BE 구현 (423 머지 후 즉시)

```
Agent(subagent_type=backend, prompt=
  "docs/plans/424-resume-wrap-up-removal/implement-be.md 의 Phase 1~4 를 순서대로 구현.
   착수 전 아래 파일 Read 필수:
   - backend/.claude/rules/conventions.md
   - backend/.claude/rules/testing.md
   - docs/plans/424-resume-wrap-up-removal/implement-be.md (전체)
   - docs/plans/424-resume-wrap-up-removal/tech-spec.md (API contract + Pre/Post State + Verification)
   Phase 3 시작 전 ResumeTurnEventPublisher listener 를 grep + Read 로 점검하여
   terminate=true 시 turnEventPublisher.publish 호출 여부 결정 후
   implement-be.md ## 결정 로그 섹션에 1줄 기록할 것.")
```

### 3순위 — FE 구현 (BE PR 머지 + dev 배포 검증 후)

```
Agent(subagent_type=frontend, prompt=
  "docs/plans/424-resume-wrap-up-removal/implement-fe.md 의 Phase 1~4 를 순서대로 구현.
   착수 전 아래 파일 Read 필수:
   - frontend/.claude/rules/conventions.md
   - frontend/.claude/rules/architecture.md
   - docs/plans/424-resume-wrap-up-removal/implement-fe.md (전체)
   - docs/plans/424-resume-wrap-up-removal/tech-spec.md
   BE dev 배포 확인 후 진입할 것. mock 진행 없음.")
```

## 미해결 질문 / Blocker

| 항목 | 상태 |
|------|------|
| plan 423 (intent classifier removal) 선행 머지 | **Blocking** — 423 미머지 시 424 BE 진입 불가 |
| turnEventPublisher.publish 호출 여부 (terminate=true 분기) | **결정 완료 (Decision B — skip)**. implement-be.md `## 결정 로그` 참조. listener IllegalStateException + 운영 알림 오인 회피 |

## 핵심 컨텍스트

### 강결합 정책
- 423 머지 → 424 BE rebase → 424 BE 구현 → BE PR 머지 + dev 검증 → FE 구현 순서 고정.
- FE mock 진행 없음 (tech-spec 강결합 BE 선행 명시).

### FollowUpRequest.terminate 직렬화
- `private boolean terminate` + `@Getter @NoArgsConstructor` 패턴 — Jackson field reflection deserialize.
- `isTerminate()` getter 자동 생성 (Lombok). 명시적 `@JsonProperty` 불필요.

### Flyway 버전 관리 (Amendment 2026-05-07)
- **신규 Flyway 마이그레이션 0건**. V46 / V47 폐기.
- 사유: V42 (`drop_question_resume_meta`) 가 이미 `chk_question_track_meta_v2` constraint + `chain_id` / `chain_step_type` / `project_id` 컬럼 일괄 DROP. RESUME_WRAP_UP row-pattern 차단 대상 부재. application enum 차단으로 충분.
- 운영 SQL (`ops/424-resume-wrap-up-cleanup.sql`) 은 별도 ops PR. constraint prerequisite 가 아닌 단순 데이터 위생 작업으로 격하 — 본 BE PR 머지 후 임의 시점 적용 가능.
- 롤백: 코드 revert 만으로 충분. constraint 복원 절차 부재.

### terminateResponse() 반환 스펙
- `followUpExhausted=true / skip=true / presentToUser=false` — 기존 hard timeout 응답 재사용.
- FE 종료 페이즈 분기: `followUpExhausted=true` 기존 분기 그대로 활용 (신규 UI 불필요).

### BE 구현 주의 사항 (implement-be.md Phase 3)
- `processUserTurn` 내 WRAP_UP 분기 제거 + 시퀀스 7번 위치에 terminate 분기 삽입.
- `dispatchByMode` switch: PLAYGROUND / INTERROGATION 2종만 잔존.
- 로그 레벨 분리: `FE-signaled terminate` → `log.info` / `hard timeout backstop` → `log.warn`.

## 관련 파일

```
docs/plans/424-resume-wrap-up-removal/
├── product-spec.md          # 기획 배경 + 수용 기준
├── tech-spec.md             # API contract / Pre-Post State / Verification / 운영 SQL (Flyway 신규 0건 — Amendment)
├── implement-be.md          # BE Phase 1~4 (결정 로그 섹션 implement 진입 후 갱신)
└── implement-fe.md          # FE Phase 1~4 (BE 머지 후 진입)
```

## 참고 명령

```bash
# 423 PR 상태 확인
gh pr list --state open

# 424 브랜치 생성 (423 머지 후)
git checkout develop && git pull
git checkout -b feat/424-resume-wrap-up-removal

# 로컬 부팅
cd backend
docker compose -f docker-compose.local.yml up -d
./gradlew bootRun --args='--spring.profiles.active=local'

# WRAP_UP 잔존 grep (Phase 4 green 기준)
grep -rEn "RESUME_WRAP_UP|WrapUp|wrap-up-threshold-min|wrapUpThresholdMin|ResumeWrapUp" backend/src
grep -rEn "RESUME_WRAP_UP|WRAP_UP|wrap-up-threshold-min" docs/domain
```

---

업데이트: 2026-05-07 (세션 종료)
