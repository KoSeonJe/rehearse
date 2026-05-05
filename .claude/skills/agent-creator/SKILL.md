---
name: agent-creator
description: Claude Code 공식 sub-agent 포맷에 맞춰 새 agent 파일을 대화형으로 생성. 사용자가 "agent 만들어줘", "서브에이전트 생성", "/agent-creator", "에이전트 추가해줘" 등으로 요청할 때 트리거. 모호한 요구사항은 단계별 질문(AskUserQuestion)으로 드릴다운하여 채운 뒤 `.claude/agents/<name>.md` 또는 `~/.claude/agents/<name>.md`에 기록한다.
---

# agent-creator — 대화형 sub-agent 빌더

## 목적

Claude Code 공식 [sub-agents 스펙](https://code.claude.com/docs/en/sub-agents)을 따라 새 agent 파일을 만든다. 사용자 요구가 모호할 때 **추측 금지** — `AskUserQuestion`으로 단계적 드릴다운(plan mode 스타일).

## 핵심 원칙

1. **공식 frontmatter만 사용**. 필수: `name`, `description`. 선택: `tools`, `disallowedTools`, `model`, `permissionMode`, `maxTurns`, `skills`, `mcpServers`, `hooks`, `memory`, `background`, `effort`, `isolation`, `color`.
2. **티키타카 방식**: 한 번에 다 묻지 않는다. 1–2개씩, 답에 따라 분기.
3. **추측 금지**: "잘 모르겠어"엔 추천안 제시 (옵션 라벨 첫 항목에 "(추천)").
4. **저장 전 dry-run 확인**: 최종 파일 미리보기 → 승인 후 `Write`.

---

## 실행 절차 (단계적 드릴다운)

### Step 0 — 진입 멘트

> "새 sub-agent 만들자. 단계별로 질문할 테니 답해줘. 모르는 건 '추천' 골라도 됨."

### Step 1 — 핵심 정체성

`AskUserQuestion`:

- Q1: "이 agent가 **무슨 일**을 하길 원해? 한 줄로." (자유 입력)

답 받으면 → `name` 후보 자동 제안 (kebab-case). 예: "API 설계 리뷰" → `api-design-reviewer`. 사용자 확인 후 확정.

### Step 2 — 트리거 조건 (description 드릴다운)

`description`은 자동 위임 핵심. 차례로:

1. "**언제** 이 agent 호출해? 키워드/상황 알려줘."
2. "**언제 호출하면 안 되는지** 명시할 게 있어?"
3. (선택) "예시 시나리오 1–2개 줄래? 자동 매칭 정확도 ↑" → `<example>...<commentary>...</commentary></example>` 블록 삽입.

### Step 3 — 권한/도구

> ⚠️ **중요**: `tools` 필드 **명시 권장**. 생략 시 일부 harness 환경에서 agent 등록 실패 사례 발견 (특히 `description: |` block scalar + `<example>` 태그 조합).

**용도별 추천 셋** (Step 1–2 답변 기반 자동 매칭):

| 용도 | 추천 tools |
|------|-----------|
| 구현 / 리팩 / 테스트 작성 | `Read, Write, Edit, Bash, Glob, Grep` |
| 코드 리뷰 (수정 X) | `Read, Glob, Grep, Bash` |
| 디버깅 (minimal fix) | `Read, Edit, Bash, Glob, Grep` |
| 문서 편집 | `Read, Write, Edit, Glob, Grep` |
| Git / PR 운영 | `Bash, Read` |
| 검색 / 분류 | `Read, Glob, Grep` |
| 디자인 / UI 리뷰 | `Read, Grep, Glob` |

`AskUserQuestion`:

- Q: "도구 접근 정책?"
  - **추천 셋 ({위 매칭 결과}) (추천)** — 용도 자동 매칭
  - **읽기 전용** — `tools: Read, Grep, Glob, Bash`
  - **모두 상속** — 필드 생략 (⚠️ 등록 실패 위험)
  - **커스텀** — 직접 나열

커스텀 → 후속: "Bash 필요? MCP 도구 필요? Write 필요?"

### Step 4 — 모델 선택 (자동 추천 로직)

**4-1. 용도 분류 자동 판정**: Step 1–2 답변 키워드로 카테고리 매칭.

| 카테고리 | 키워드 신호 | 추천 model | 근거 |
|---|---|---|---|
| **Heavy reasoning** | "리뷰", "설계", "아키텍처", "보안 감사", "디버깅(복잡)", "리팩토링 전략" | `opus` | 추론 깊이 필요 |
| **Standard implementation** | "구현", "API 작성", "테스트 작성", "리팩토링 실행", "문서 갱신" | `sonnet` | 비용/품질 균형 |
| **Mechanical / repetitive** | "progress.md 갱신", "PR 생성", "배포", "환경변수 적용", "포맷 변환" | `sonnet` (정형) 또는 `haiku` (초경량) | 정형 절차, 추론 적음 |
| **Lookup / classification** | "검색", "분류", "태깅", "간단 추출" | `haiku` | 속도/비용 우선 |
| **Unclear / mixed** | 위에 안 맞음 | `inherit` | 부모 세션 모델 따름 — **안전 폴백** |

**4-2. 추천 제시 + 확인**:

> "용도 분석 결과 **{카테고리}**로 보임. **`{recommended}`** 추천 (이유: {근거}).  
> 다른 모델 쓸래?"

`AskUserQuestion` 옵션 (추천값 첫 자리, 라벨에 "(추천)"):

- `{recommended}` (추천) — {1줄 근거}
- `inherit` — 부모 세션 모델 상속 (가장 안전)
- `sonnet` — 정형 작업 균형
- `haiku` — 속도/비용 우선
- `opus` — 최고 추론

> 카테고리 판정 자체가 애매하면 **추천을 `inherit`으로** 강등하고 사용자에게 "용도가 섞여 보이는데 어느 쪽 비중이 커?" 재질문.

**4-3. 비용 경고 (조건부)**:

- 사용자가 `opus` 선택 + 카테고리 = mechanical/lookup → "정형 작업에 opus는 비용 과다. sonnet 권장. 그래도 opus?" 1회 재확인.
- 사용자가 `haiku` 선택 + 카테고리 = heavy reasoning → "복잡 추론에 haiku는 품질 저하 위험. sonnet/opus 권장. 그래도 haiku?" 1회 재확인.

### Step 5 — 추가 옵션 (조건부)

명시적 필요 시에만:

- `permissionMode`: 자동 승인 정책 변경?
- `isolation: worktree`: repo 격리 사본?
- `memory`: 세션 간 학습 누적? (`user`/`project`/`local`)
- `effort`: 모델 effort 강제?
- `maxTurns`: 무한 루프 상한?
- `color`: UI 식별 색?
- `skills`: 부팅 시 자동 로드 skill?

"기본값 가자" → 전부 스킵.

### Step 6 — System Prompt 본문

frontmatter 아래 markdown = system prompt. 4섹션 템플릿:

```markdown
# {Agent Name}

## 역할
{한 문장 정체성}

## 작업 시작 전 필독
{프로젝트 컨벤션 파일 경로 등 — 필요시}

## 절차
1. ...
2. ...
3. ...

## 출력 형식
{보고 포맷, 코드 블록 규칙 등}

## 금지사항
- ...
```

각 섹션 한 번씩 묻기. "skip" → 섹션 제거.

### Step 7 — 저장 위치

`AskUserQuestion`:

- **프로젝트 (`.claude/agents/<name>.md`) (추천)** — 팀 공유, 버전 관리
- **글로벌 (`~/.claude/agents/<name>.md`)** — 개인용, 전 프로젝트

### Step 8 — Dry-run & 확인

완성된 파일 코드 블록으로 출력 후:

> "이대로 저장할까? (수정/저장/취소)"

- 수정 → 어느 섹션 고칠지 묻고 해당 단계로 복귀
- 저장 → `Write` 실행
- 취소 → 종료

### Step 9 — 저장 후 안내

```
✅ 저장: <path>
재시작 또는 /agents 로 즉시 로드.
테스트: "Use the <name> agent to ..."
```

---

## 출력 파일 템플릿

```markdown
---
name: <kebab-case-name>
description: <자동 위임 트리거 + 사용 사례. 필요시 <example> 블록>
tools: <명시 권장 — 생략 시 일부 harness 에서 등록 실패>
model: <sonnet|opus|haiku|inherit, 기본 inherit>
---

# <Agent Display Name>

## 역할
...

## 절차
...

## 출력 형식
...

## 금지사항
...
```

---

## 검증 체크리스트 (저장 직전)

- [ ] `name` kebab-case + lowercase + 충돌 없음 (`ls .claude/agents/`, `ls ~/.claude/agents/`)
- [ ] `description` 위임 트리거 명확 — "언제 사용/미사용" 둘 다
- [ ] **`tools` 필드 존재** — 공식 도구명 정확 (Read/Edit/Write/Grep/Glob/Bash/WebFetch/WebSearch/NotebookEdit/TaskCreate 등). 생략 시 등록 실패 위험
- [ ] `model` 값 유효 (`sonnet`/`opus`/`haiku`/`inherit` 또는 full ID 예: `claude-opus-4-7`)
- [ ] system prompt 본문 비어있지 않음
- [ ] BE/FE 작업 agent면 프로젝트 컨벤션 참조 안내 포함

---

## 안티패턴

- ❌ 한 번에 6+ 질문 폭탄
- ❌ "잘 모르겠어"에 임의 채우기 — 추천안 + 동의 필수
- ❌ 공식 스펙 외 frontmatter 필드 추가
- ❌ `description` 한 단어 ("코드 리뷰") — 자동 위임 실패
- ❌ Dry-run 스킵하고 바로 `Write`
- ❌ 용도 mechanical인데 opus 추천, 또는 heavy reasoning에 haiku 추천
- ❌ **`tools` 필드 생략** — `description: |` block scalar + `<example>` 태그 조합 시 harness 등록 실패 사례 (관찰 2026-05-06)

---

## 참고

- 공식 docs: https://code.claude.com/docs/en/sub-agents
- 프로젝트 기존 agent: `.claude/agents/release-ops.md` (Sonnet, mechanical), `prompt-engineer.md`, `ui-ux-designer.md`
- `/agents` 슬래시 커맨드 — GUI 빌더 (본 스킬은 대화형 CLI 대안)
