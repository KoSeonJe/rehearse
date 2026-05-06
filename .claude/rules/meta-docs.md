# AGENTS.md / CLAUDE.md 관리 룰

**대상 파일**: `AGENTS.md`, `CLAUDE.md`, 하위 영역 동등 파일 (`backend/AGENTS.md`, `backend/CLAUDE.md`, `frontend/AGENTS.md`, `frontend/CLAUDE.md`, `lambda/AGENTS.md`, `lambda/CLAUDE.md`).

이 파일들 **수정 / 추가 / 점검** 작업 시 `claude-md-management` 플러그인 도구 사용 필수. 직접 Edit 자제 — 도구가 일관성 / 간결성 / 정합성 보장.

## 도구 매핑

| 상황 | 도구 | 호출 방법 |
|------|------|----------|
| 정기 audit / 큰 코드 변경 후 정합성 점검 / 디렉토리·명령어 stale 의심 | `claude-md-improver` (skill) | "audit my CLAUDE.md files" / "AGENTS.md 점검해줘" |
| 세션 학습 반영 / 새 컨벤션 합의 후 / 발견한 gotcha 기록 | `/revise-claude-md` (command) | `/revise-claude-md` 명시 호출 |

## 트리거 판단 (자율)

상황 보고 적절히 트리거. 사용자가 명시 안 해도 다음 신호 시 제안 / 호출:

- **improver 신호**:
  - 빌드/테스트 명령 변경 / 디렉토리 개편 / 의존 추가·제거 후
  - 큰 규모 PR 머지 후 (10+ 파일 변경 / 구조 변경)
  - "AGENTS / CLAUDE 오래된 것 같다" 사용자 멘션
  - 한 영역 룰 변경 → 다른 영역 동기화 필요 의심

- **revise 신호**:
  - 의미 있는 합의 / 결정 (이번 세션 같이 새 시스템 도입)
  - 미래 세션 도움 될 함정 / 패턴 발견
  - 세션 종료 직전 (사용자 "끝" / "종료" / handoff 작성 시점)

## 운영 룰

- 도구 호출 후 제안 diff 사용자 승인 필수 (Blocking).
- 한 도구 호출 = 한 책임. revise + improver 동시 호출 X. 하나 끝나고 다음.
- 단순 1줄 오타 / typo 수정은 직접 Edit 가능 (도구 과함).
- 다층 구조 (root + sub-AGENTS) 의식. 한 곳 변경 시 다른 층 영향 같이 검토.
