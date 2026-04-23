# Plan 11a: Lambda Nonverbal Schema Prerequisite (Gemini 프롬프트 필드 확장)

> 상태: Draft
> 작성일: 2026-04-21
> 주차: W7 초반 (plan-11 착수 전 `[blocking]`)
> 선행: plan-00a INVENTORY (Lambda 현황 확인 완료)
> 후행: **plan-11 Nonverbal Rubric** (본 plan 없이는 실행 불가)

## Why

`VERIFICATION_REPORT.md` §D3 에서 확인: plan-11 이 전제하는 Lambda 출력 필드 3개가 **전부 실재하지 않음**.

| 필드 | plan-11 전제 용도 | 현재 상태 |
|------|------------------|-----------|
| `speed_variance` | D12 Confidence Tone | ❌ 없음 |
| `gaze_on_camera_ratio` | D13 Eye Contact | ❌ 없음 (`eyeContactLevel` enum 만 존재) |
| `posture_unstable_count` | D13 Posture | ❌ 없음 (`postureLevel` enum 만 존재) |

plan-11 본문(line 20)은 "Gemini 프롬프트 전면 개편은 Out-of-Scope" 로 선을 그었기에 **필드 확장을 담당하는 별도 plan 이 필요**. 이 공백 없이 plan-11 실행 시 서브에이전트가 가짜 필드를 생성하거나 결정론 threshold 매핑이 불가능해짐.

본 plan 은 **최소 범위의 Gemini 프롬프트 필드 추가**만 담당. 프롬프트 전면 개편(`prompt-improvement-2026-04` Lane 3-5)은 여전히 별건.

## Goal

Gemini `verbal_analyzer` / `vision_analyzer` 의 response_format 에 3개 수치 필드를 최소 침습으로 추가. 기존 필드(`filler_word_count`, `tone_label`, `eyeContactLevel`, `postureLevel`, `expressionLabel`) 는 **전부 불변**.

| 지표 | 현재 | 목표 |
|------|------|------|
| 새 필드 3개 출력 성공률 | — | ≥ 99% (파싱 실패율 ≤ 1%) |
| 기존 필드 회귀 | — | 0건 (기존 소비처 `TimestampFeedback` 정상) |
| Gemini 응답 latency 회귀 | baseline | p95 +300ms 이내 |

## Scope

### In
- `lambda/analysis/analyzers/verbal_prompt_factory.py` 에 `speed_variance: float (0.0~1.0)` 추가
- `lambda/analysis/analyzers/vision_analyzer.py` 에 `gaze_on_camera_ratio: float (0.0~1.0)`, `posture_unstable_count: int (0~N)` 추가
- Gemini 응답 JSON 스키마 정의 확장 (`response_schema` 또는 response text 규약)
- `lambda/analysis/handler.py` 의 응답 검증부에 신규 필드 파싱 + 누락 시 default 값 처리
- Python 테스트 추가 (신규 필드 파싱 + 값 범위 검증)

### Out
- 기존 enum 필드 변경 (`eyeContactLevel` 등) — 불변
- 프롬프트 템플릿 전면 개편 — `prompt-improvement-2026-04` 별건
- Backend 측 필드 소비 (plan-11 범위)
- 결정론 매퍼 구현 (plan-11 범위)
- **Lambda `verbal` / `technical` 블록 제거 — plan-13 범위** (본 plan은 delivery 수치 필드 추가만, content 블록 제거는 별도 plan)

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/verbal_prompt_factory.py` | 수정. 응답 스키마에 `speed_variance: float` 추가. 프롬프트 지시문에 "발화 속도의 분산을 0.0(일정)~1.0(매우 불안정) 으로 측정" 1줄 추가 |
| `lambda/analysis/analyzers/vision_analyzer.py` | 수정. 응답 스키마에 `gaze_on_camera_ratio: float`, `posture_unstable_count: int` 추가. 프롬프트 지시문에 "전체 프레임 중 카메라를 응시한 비율(0.0~1.0), 자세 불안정 감지 횟수(0~N)" 2줄 추가 |
| `lambda/analysis/handler.py` | 수정. 신규 필드 파싱 실패 시 default (`speed_variance=0.5`, `gaze_on_camera_ratio=0.5`, `posture_unstable_count=0`) 적용 + `failure_reason=SCHEMA_MISSING_FIELDS` 카운터 증가 (운영 관측용) |
| `lambda/analysis/tests/test_verbal_prompt_factory.py` | 수정 또는 신규. 필드 존재/범위 검증 |
| `lambda/analysis/tests/test_vision_analyzer.py` | 수정 또는 신규. 동일 |
| `lambda/analysis/tests/test_handler_new_fields.py` | 신규. 필드 누락 시 default 처리 + 정상 케이스 |

## 상세

### 필드 정의

| 필드 | 타입 | 범위 | Gemini 프롬프트 지시 |
|------|------|------|---------------------|
| `speed_variance` | float | 0.0~1.0 | "발화 속도의 분산 정도. 0.0=시종일관 일정, 1.0=매우 불안정(빠름/느림 변동 큼). 주저함/말더듬 포함" |
| `gaze_on_camera_ratio` | float | 0.0~1.0 | "영상 전체 프레임 중 눈동자가 카메라(화면 상단 중앙)를 향한 비율. 시선 이탈(바닥/천장/좌우)은 제외" |
| `posture_unstable_count` | int | 0~N | "몸이 흔들리거나 자세가 급격히 바뀐 순간의 개수. 기준: 상반신 각도가 >15° 변화한 지점" |

### 기존 enum 과의 관계 (하위 호환)

plan-11 결정론 매퍼는 신규 수치 필드만 사용. 기존 enum (`eyeContactLevel`, `postureLevel`) 는 기존 `TimestampFeedback` 소비처를 위해 유지.

즉 Gemini 응답은 **동일 응답에 수치 + enum 을 둘 다 출력** 하는 "지시문 확장" 방식. 이는 프롬프트 전면 개편이 아니며, Gemini 가 수치 산출을 거부하거나 누락할 경우 `handler.py` default fallback 으로 plan-11 매퍼에 안전한 값(중간값) 전달.

### 실패 처리 — admin 재시도 연계 (plan-09 §Lambda Error Handling)

- 신규 필드 누락 시: `failure_reason=SCHEMA_MISSING_FIELDS` 로 마킹 (신규 에러 타입). `failure_detail` 에 누락 필드명 기록
- Backend `SessionFeedbackService` 는 해당 턴을 **admin 재시도 가능 상태**로 저장. 사용자에게는 "비언어 분석 일시 오류, 재처리 중" 배너 (plan-09 매핑)
- admin 재시도 시 Lambda 가 갱신된 프롬프트/모델로 재실행 후 성공하면 스키마 호환

### 설정

Lambda 환경변수 `ANALYZER_SCHEMA_VERSION=v2` 로 활성화. 필드 누락 시 `handler.py` 가 아래 기본값 적용:
- `speed_variance: 0.5`
- `gaze_on_camera_ratio: 0.5`
- `posture_unstable_count: 0`

Backend feature flag runtime toggle은 사용하지 않는다. Lambda 배포 후 스모크 통과 시 환경변수로 전환.

## 담당 에이전트

- Implement: `backend` + `ai-media-specialist` — Gemini 프롬프트 필드 확장 (SSML/response_format)
- Review: `code-reviewer` — response 파싱 방어성, default fallback 안전성
- Review: `architect-reviewer` — Lambda ↔ Backend 계약 하위 호환성

## 검증

1. **신규 필드 출력률**: 100개 실제 영상 샘플에서 `speed_variance` / `gaze_on_camera_ratio` / `posture_unstable_count` 출력 성공률 ≥ 99%
2. **값 범위**: float 필드 [0.0, 1.0] 이탈 0건, int 필드 [0, 100] 이탈 0건 (clamp 적용)
3. **기존 필드 회귀**: 기존 `TimestampFeedback` 생성 경로 100% 정상 (unit test + 실제 세션 10개 E2E)
4. **Latency 회귀**: Gemini 응답 p95 ≤ baseline + 300ms
5. **default fallback**: 필드 전부 누락된 응답 10케이스에서 handler.py 가 default 값으로 정상 처리 + `SCHEMA_MISSING_FIELDS` 카운터 증가
6. **plan-11 착수 가능성**: 본 plan 머지 후 `lambda/analysis/handler.py` 출력 JSON 에 3개 필드 존재 확인
7. `progress.md` 11a → Completed 후 plan-11 착수

## Exit Criteria

- 없음 (프롬프트 필드 확장은 영구 변경). Lambda 환경변수 `ANALYZER_SCHEMA_VERSION=v2` 는 영구 유지.

## Out of Scope (재확인)

- Gemini 프롬프트 전면 재작성 / 새 모델 도입 / Whisper 교체 — `prompt-improvement-2026-04`
- Backend `NonverbalRubricScorer` 구현 — plan-11
- 결정론 매퍼 `nonverbal_rubric_mapper.py` — plan-11
- context_weights 적용 — plan-11
