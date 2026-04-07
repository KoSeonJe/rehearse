# 피드백 코멘트 구조화 (feedback-v3) — 진행 상황

## 태스크 상태

| # | 태스크 | 담당 (Implement / Review) | 상태 | 비고 |
|---|---|---|---|---|
| 1 | Lambda 프롬프트·핸들러 정형 JSON 전환 | `executor` / `code-reviewer` | Completed | Gemini/Vision 정형 JSON + 레거시 폴백 `_legacy_string_to_block` 래핑 |
| 2 | BE DTO 직렬화 규약 변경 | `backend` / `architect-reviewer` | Completed | `CommentBlock` 5종 + `overallComment` 응답 노출 + `parseCommentBlock` 단위 테스트 |
| 3 | FE 타입·StructuredComment·매퍼·탭 | `frontend` / `code-reviewer`, `designer` | Completed | `CommentBlock` 타입, `isCommentBlockEmpty` 헬퍼, `formatExpressionLabel` |

## 머지 순서 (엄수)

`[BE]` Plan 02 → `[Lambda]` Plan 01 → `[FE]` Plan 03 → 검증

> BE 응답 형식이 바뀌므로 FE는 BE 머지 직후 잠시 깨질 수 있음. dev 단일 사용자 환경이라 같은 작업 윈도우에 세 PR을 연속 머지·배포하면 갭은 무시 가능.

## 진행 로그

### 2026-04-07 (코드리뷰 반영)
- 🔴 Critical 픽스: 레거시 Whisper+GPT-4o 폴백 경로(`_build_timestamp_feedbacks`)가 `verbal.get("comment")` string을 그대로 보내 BE Jackson 역직렬화 실패하던 버그 수정 → `_legacy_string_to_block` 헬퍼로 `{positive: raw, negative: null, suggestion: null}` 객체 래핑
- 🟡 `overallComment` 응답 누락 보강: `TimestampFeedbackResponse`에 `overallComment` 필드 + `from()` 매핑 + FE 타입 추가
- 🟡 빈 CommentBlock 가드 보강: `isCommentBlockEmpty` 헬퍼를 `interview.ts`에 추가, `content-tab.tsx`/`delivery-tab.tsx`의 `hasVerbalComment`/`hasAttitude` 가드를 객체 단순 null 체크 → 필드 단위 trim 검사로 강화
- 🟢 `_comment_block` 람다 헬퍼: 빈 문자열을 None으로 정규화 + 세 필드 모두 None이면 블록 자체 None 반환
- 🟢 `StructuredComment`: `block === null` → `if (!block)` 으로 undefined 방어 추가

### 2026-04-07
- 사용자 보고: 피드백 페이지에서 `△`/`→` 이모지가 본문에 노출, 표정 배지가 `NEUTRAL` 영어로 표시
- 람다 프롬프트(`vision_analyzer.py`, `gemini_analyzer.py`)와 FE 파서(`structured-comment.tsx`) 양쪽 분석
- 근본 원인 확인: "LLM 자유서술 + FE prefix 파싱"이라는 취약 계약. LLM이 줄바꿈 없이 한 줄로 응답하면 파서 무력화
- 결정: 정형 JSON `{positive,negative,suggestion}` 객체로 응답 스키마 전면 교체. DB 스키마/마이그레이션 없이 기존 컬럼에 JSON 문자열 저장 (`accuracy_issues` 패턴 차용)
- 산출 파일: `requirements.md`, `plan-01-lambda-prompt.md`, `plan-02-be-dto.md`, `plan-03-fe-component.md`, `progress.md`
