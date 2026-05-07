# docs/domain — 도메인 정책 / 스키마 / API 흐름 운영 룰

이 폴더는 devlens 도메인별 **영구 정책 저장소**. "어떤 테이블이 무슨 단위인가 / 어떤 API 가 어떤 분기로 흐르는가 / 어떤 룰이 코드 밖에 산재하는가" 가 여기에 산다.

> 루트 `AGENTS.md`, `CLAUDE.md` 와 함께 적용. 충돌 시 이 파일 우선 (이 폴더 한정).

대상 도메인 10개: `interview`, `feedback`, `reviewbookmark`, `file`, `resume`, `question`, `user`, `auth`, `admin`, `servicefeedback`.

---

## 1. 폴더 구조

```
docs/domain/
├── AGENTS.md                  # 이 파일
├── _templates/
│   ├── schema.md
│   ├── api-flow.md
│   └── glossary.md
└── {도메인명}/
    ├── schema.md              # 테이블 4개 이하
    ├── schema/                # 또는 5개+ 분할
    │   └── {table}.md
    ├── api/
    │   └── {action}.md        # 액션 단위 (kebab-case)
    └── glossary.md            # 옵션 — 도메인 용어 모호 시
```

---

## 2. 분할 룰

### Schema
- 테이블 ≤ 4: 단일 `schema.md`
- 테이블 ≥ 5: `schema/` 폴더 + 테이블별 `{table}.md`

### API
- 비즈니스 **액션** 단위. REST endpoint 1:1 매핑 X.
- 한 액션 안에서 여러 endpoint 가 같은 분기 / state 전이를 공유하면 묶는다.
- 파일명 kebab-case (`start-session.md`, `submit-answer.md`).

### Glossary
- 한글 ↔ 영문 / 코드 식별자 매핑 모호 시 작성.
- 명확하면 생략 가능 (강제 X).

---

## 3. 작성 절차 (5단계)

### Step 1. 대상 도메인 결정
- **1회 1 도메인**. 여러 도메인 동시 작성 금지 (메타인지 분산).

### Step 2. backend agent 호출 → 초안 자동 추출
§6 프롬프트 템플릿 복붙. 코드 + Flyway 마이그레이션 분석 후 초안 생성.

### Step 3. 사용자 메타인지 검토
§7 체크리스트 기반. 코드에서 추론 불가능한 정책 (state 전이 룰 / 임계값 / fallback / 동시성) 보완.

### Step 4. preview → 사용자 승인 (Blocking)
파일 작성 전 전체 본문 preview. 사용자 명시 승인 후 Step 5.

### Step 5. `docs/domain/{name}/` 작성 + 커밋
`Write` 다중 파일. 커밋 메시지: `docs(domain): {name} 도메인 정책 / 스키마 / API 흐름 문서화`.

---

## 4. 참조 시점 (Read 강제 룰)

| 호출 / 상황 | Read 대상 |
|------------|---------|
| `backend` / `frontend` / `lambda` agent — 도메인 작업 | `docs/domain/{name}/` 전체 |
| `create-product-spec` / `create-tech-spec` 스킬 | 식별된 도메인 `policy` (= `api/*.md`) + `glossary.md` |
| `code-reviewer-backend` / `code-reviewer-frontend` | 정책 위반 검증 항목 — schema / api flow 와 PR diff 대조 |

도메인 작업인데 위 Read 누락 = 컨벤션 위반. 리뷰 단계에서 재작업 사유.

---

## 5. 갱신 트리거

- Flyway 마이그레이션 추가 / 변경 → `schema.md` (또는 `schema/{table}.md`) 갱신
- 신규 endpoint / 비즈니스 룰 변경 → 해당 `api/{action}.md` 갱신
- 신규 도메인 추가 → 본 절차 재실행
- PR 머지 전 갱신 누락 시 reviewer 가 차단

### 자동 동기화 (Claude Code PostToolUse hook)

Claude 세션 안에서 `git pull` / `git merge` Bash 도구 실행 직후 `.claude/hooks/post-merge-sync.sh` 가 fire. ORIG_HEAD..HEAD diff 분석 → 영향받은 BE 도메인 / Flyway / docs/architecture / progress.md 카테고리 추출 → **메인 세션에 작업 컨텍스트 inject** (`hookSpecificOutput.additionalContext`). 메인 세션이 다음 turn 에서 직접 docs 파일 Edit. 별개 백그라운드 `claude -p` 호출 X — 토큰 / 컨텍스트 메인 세션 통합.

비활성화:
- 일시: `SKIP_DOC_SYNC_HOOK=1` env (per-shell)
- 머지 커밋 메시지에 `[skip-doc-sync]` 포함
- 영구: `.claude/settings.json` 의 PostToolUse hook 항목 제거

제약:
- Claude 세션 밖 머지 (직접 터미널 / IDE / CI) = fire 안함. 워크플로우상 develop pull 은 `git-manager` agent 위임이 원칙 → 거의 모든 케이스 커버.
- docs/domain/{name}/ 미존재 = 노터치 → §3 5단계로 사용자 수동 트리거 필요.
- `git pull --rebase` 는 ORIG_HEAD 머지 시맨틱 부재 → 스킵.

PR 단위 갱신 원칙. "나중에 한꺼번에" 금지 — drift 누적.

---

## 6. backend agent 프롬프트 템플릿 (복붙용)

```
backend agent 로 docs/domain/{name}/ 초안 작성.

분석 대상:
- backend/src/main/java/com/rehearse/api/domain/{name}/
- backend/src/main/resources/db/migration/V*.sql 중 {name} 관련

템플릿: docs/domain/_templates/{schema,api-flow,glossary}.md strict 준수.
- schema.md: 테이블 4개 이하 단일 파일 / 5개+ schema/{table}.md 분할
- api/{action}.md: endpoint 1:1 X. 비즈니스 액션 단위로 묶음.

출력:
- schema.md (테이블 + 컬럼 성격 자연어)
- api/{action}.md 다수 (endpoint 별 흐름 + 분기 + 조건/엣지 + 관찰성)
- glossary.md (도메인 용어 모호 시)

비즈니스 룰 중 코드에서 추론 불가 항목 = ❓TODO(사용자 확인) 마킹.
- state 전이 조건 / 임계값 / fallback 발동 조건 / 동시성 정책 / 보존 정책 / 권한 모델 / 외부 의존 fallback / 관찰성 임계
- 추론으로 채우지 말 것. 모르면 ❓TODO.

작성 후 preview 만 출력. 파일 직접 쓰지 말 것 (사용자 검토 후 Write).
```

---

## 7. 메타인지 체크리스트 (사용자 보완 강제)

backend agent 초안 = 코드에서 보이는 만큼만 추출 가능. 코드 밖 정책은 사용자만 안다. 다음 8항목 보완:

- [ ] **state machine 전이 룰** — enum 만 있고 전이 조건은 service 산재. 누가 / 언제 / 어떤 조건에서 전이?
- [ ] **모호도 / 점수 임계값** — magic number 의미. 0.7 / 0.85 가 무엇 기준?
- [ ] **AI fallback 발동 조건** — timeout / 5xx / parse 실패 구분. 몇 회 retry?
- [ ] **동시성 충돌 처리** — 낙관락 / 비관락 / 큐 / retry 정책. 충돌 시 사용자 경험?
- [ ] **데이터 보존 정책** — soft delete / TTL / 백필 / 영구보존
- [ ] **권한 모델** — 어떤 role / userId 기준. Admin override 존재?
- [ ] **외부 의존 실패 시 fallback** — resume 삭제 시 일반 질문 모드 등. graceful degradation?
- [ ] **관찰성** — 로그 key fields / 메트릭 / 알람 임계

체크리스트 미통과 = preview 승인 금지.

---

## 8. 안티패턴

- 템플릿 섹션 / 헤더 자의적 변형 (구조 일관성 손실)
- 코드 복붙 (자연어 흐름 X, 의사코드 / 분기 표만 작성)
- endpoint 1:1 파일 (액션 단위 묶음 원칙 위반)
- 사용자 메타인지 없이 agent 출력 그대로 커밋 (❓TODO 잔존)
- 한 세션에 여러 도메인 동시 작성
- "조건 / 엣지" 표 비어있는 row 방치 (의도적 N/A 면 명시)
- PR 단위 갱신 누락 ("나중에 한꺼번에")
