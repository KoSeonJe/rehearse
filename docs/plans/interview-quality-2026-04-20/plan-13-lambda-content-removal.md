# Plan 13: Lambda Content Removal (Delivery/Content 책임 경계 확정)

> 상태: Draft
> 작성일: 2026-04-22
> 주차: W7 후반 (plan-08 + plan-09 ECR cut-over와 동시)
> 선행 `[blocking]`: plan-08 (Rubric Scorer), plan-09 (Feedback Synthesizer) — 둘 다 프로덕션 배포 완료 + STAGING G1~G3 자동 게이트 + MANUAL_AB_PROTOCOL.md 3~5건 수동 비교 통과
> 후행: 없음 (plan-12 Feature Flag Cleanup 은 2026-04-23 폐기)

## Why

현재 Lambda Gemini analyzer는 **5개 블록(transcript, vocal, verbal, technical, attitude, overall)을 단일 프롬프트로 생성**하고, FE `content-tab.tsx`가 `verbal`+`technical`을 **가공 없이 그대로 렌더**한다. 이 구조에서 plan-08 Rubric Scorer가 도입되면 **같은 답변을 두 시스템(Gemini + Rubric)이 이중 LLM 호출로 평가**하는 구조가 굳어진다.

### 구조적 문제
1. **컨텍스트 결손**: Gemini는 `questionSetCategory`, `intentType`, `resumeMode`, `currentChainLevel`, resume 체인 컨텍스트를 받지 않음 → 기술 정확성을 "레벨/의도" 기준으로 판정 불가. 감각적 "맞아 보임"만 판단.
2. **Rubric D1~D10 중 D2/D3/D4/D6 4차원이 Lambda `verbal`+`technical`과 중복**. 나머지 D1/D5/D7/D8/D9/D10은 컨텍스트 필요 → Lambda가 원천적으로 할 수 없음.
3. **Lambda `verbal` 블록 6개 축** (용어 정확, 수치 구체, 논리 구조, 주제 이탈, 분량, 전달 명확성) **전부**가 Rubric D3/D4/D6로 흡수됨. 고유 가치 없음.
4. **Plan-09 Synthesizer가 중복 adjudication 로직을 구현해야 함** → 프롬프트 복잡도 ↑, 품질 일관성 ↓, 운영 비용 ↑.

### 의도된 결과
- **Lambda = Delivery Analyzer** (AV-grounded only): `transcript` + `vocal` + `attitude` + `vision` + `overall_delivery`
- **Rubric Scorer (plan-08) = Content Analyzer** (text+context-grounded): D1~D10 차원 스코어링 단독 담당
- **Feedback Synthesizer (plan-09)**: 두 출처를 **섞지 않고** 합성 — Content 섹션은 `turn_scores`만, Delivery 섹션은 `delivery_analysis` + `vision_analysis`만
- Lambda 프롬프트 단순화로 토큰·지연 감소, Gemini 품질은 delivery에 집중, Rubric 품질 독립 검증 가능

### 사용자 결정 (2026-04-22 세션, 2026-04-23 갱신)
- **전환 방식**: 신규 ECR 이미지 배포 + Lambda 함수 버전 업데이트로 단일 cut-over. Feature Flag runtime toggle은 사용하지 않는다.
- **`verbal` 블록**: 완전 제거 (D3가 흡수)
- **DB 컬럼**: 바로 drop (V29, 과거 인터뷰 content 탭은 "데이터 없음" 표시 허용)
- 근거: dual-read 단계 없이도 스테이징에서 MANUAL_AB_PROTOCOL.md 프로토콜(3~5건 수동 diff)로 Rubric 품질 사전 검증 가능. ECR 롤백이 Feature Flag보다 단순하고 일관성 있음.

## 전제 (Phase 0 선행 필수)

- **`[blocking]` plan-08 완료**: Rubric Scorer가 7개 카테고리 rubric + 10 차원에 대해 `./gradlew test --tests "Rubric*Test"` 전부 통과. 단일 경로로 프로덕션 배포 완료 (runtime toggle 없음).
- **`[blocking]` plan-09 완료**: Feedback Synthesizer가 `turn_scores` + `delivery_analysis` 입력으로 5섹션 출력 생성. 단일 경로로 프로덕션 배포 완료.
- **내부 품질 검수 통과**: `./STAGING_QUALITY_CHECKLIST.md` G1~G3 자동 게이트 전부 pass + `./MANUAL_AB_PROTOCOL.md` 3~5건 수동 diff 과반 우세.
- **FE 리팩터 준비**: `content-tab.tsx`가 Rubric/Synthesizer 기반 렌더링 경로 구현 완료.

## Goal

| 지표 | 현재 | 목표 |
|------|------|------|
| Lambda Gemini 프롬프트 토큰 수 | baseline | −30% 이상 (verbal/technical 섹션 제거분) |
| Lambda 응답 latency p95 | baseline | −500ms 이상 |
| 같은 답변에 대한 LLM 호출 중복 | 2회 (Gemini + Rubric) | 1회 (Rubric 단독) |
| FE `content-tab.tsx` 렌더링 소스 | Lambda `technical`+`verbal` | plan-08 Rubric + plan-09 Synthesizer |
| DB `timestamp_feedback` content 컬럼 | 4개 (verbal_comment, accuracy_issues, coaching_structure, coaching_improvement) | 0개 (V29 drop) |

## Scope

### In
- Lambda Gemini 프롬프트/스키마에서 `verbal`/`technical` 블록 제거
- Lambda `overall` → `overall_delivery` rescope (기술 내용 언급 금지 가드레일)
- Legacy `verbal_analyzer.py` 경로도 동일 정리 (또는 완전 제거 검토)
- Backend DTO/Entity/Mapper에서 대응 필드 제거
- FE types/component에서 대응 필드 제거, Rubric/Synthesizer 기반 렌더로 전환
- DB migration V29: `timestamp_feedback` 컬럼 4개 drop (plan-11의 V28 `nonverbal_score` 생성과 충돌 회피)
- plan-08 / plan-09 는 단일 경로로 배포된 상태 (runtime toggle 없음). plan-13 cut-over 는 ECR 신규 태그 배포로 일원 진행

### Out
- Rubric Scorer 자체 구현 (plan-08)
- Feedback Synthesizer 자체 구현 (plan-09)
- Lambda Nonverbal 수치 필드 추가 (plan-11a)
- Lambda Vision 블록 스키마 변경 (유지)
- 과거 인터뷰 데이터 마이그레이션 (과거 인터뷰 content 섹션은 "데이터 없음" 노출 허용 — 사용자 결정)
- Dynamic Pacing 연계 로직 (별도)

## 생성/수정 파일

### Lambda (수정)

| 파일 | 작업 |
|------|------|
| `lambda/analysis/analyzers/gemini_analyzer.py` (L102-202) | 프롬프트에서 "### 3. verbal — 답변 내용 전달력", "### 4. technical — 직무 정확성" 섹션 **전부 삭제**. JSON 스키마(L177-194, L201-202)에서 `verbal`, `technical` 필드 제거. "### 6. overall" → "### 5. overall_delivery" 로 번호 조정 + "기술 내용 언급 금지 — accuracyIssues/coaching 관련 내용 포함 시 재생성" 가드레일 추가. "섹션 관점 분리" 설명에서 verbal/technical 항목 제거 |
| `lambda/analysis/handler.py` (L254-267) | output assembly에서 `accuracyIssues`, `coachingStructure`, `coachingImprovement`, `verbalComment` 매핑 제거. `overall` → `overall_delivery` 필드명 변경 |
| `lambda/analysis/analyzers/verbal_analyzer.py` | legacy fallback 경로. Gemini 주력 전환 후 제거 검토 (단독 PR) 또는 content 관련 필드 (accuracy_issues, coaching_*) 제거만 먼저 |
| `lambda/analysis/tests/test_gemini_analyzer.py` | 수정. verbal/technical 검증 테스트 삭제 또는 반대 검증(출력 JSON에 해당 키 부재)으로 전환 |

### Backend (제거)

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/feedback/dto/SaveFeedbackRequest.java` (L37, L57-59) | `verbalComment`, `accuracyIssues`, `coachingStructure`, `coachingImprovement` 필드 제거. Lambda 호출 계약에서 해당 필드 더 이상 수신 안 함 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/entity/TimestampFeedback.java` (L38, L76-82) | 대응 엔티티 필드 + 빌더 파라미터 제거. `overall_comment` → `overall_delivery_comment`로 rename (DB 컬럼 일관성) 또는 유지 결정 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/dto/TimestampFeedbackResponse.java` (L28, L45-49, L93-166) | `content` 필드 제거, `ContentFeedback` nested class 제거, `parseAccuracyIssues()` 메서드 제거 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/service/TimestampFeedbackMapper.java` (L27, L40-42) | accuracy/coaching 매핑 로직 제거 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/service/DevController.java` (L173-204) | mock feedback builder에서 Lambda content 필드 제거 |

### Backend (추가)

| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/controller/QuestionSetController.java` | `getFeedback()` 응답 DTO에 `content` 섹션을 Rubric+Synthesizer 출력으로 채움. 기존 timestamp별 content 제거, 질문셋 전체 집계 content로 변경 |
| `backend/src/main/java/com/rehearse/api/domain/feedback/session/SessionFeedbackService.java` (plan-09 산출) | `getContentFeedback(questionSetId)` 메서드 추가 — Rubric turn_scores + Synthesizer 조합 반환 |

### Frontend (수정)

| 파일 | 작업 |
|------|------|
| `frontend/src/types/interview.ts` (L132-180) | `ContentFeedback` 타입 재정의: `{dimensionScores: DimensionScore[], synthesis: SynthesizedContent, observations: Observation[], levelFlag: UserLevel}` 구조로 교체. timestamp별 content 필드 제거 |
| `frontend/src/components/feedback/content-tab.tsx` | `verbalComment` 섹션 제거, `AccuracyIssues`/`CoachingCard` 컴포넌트를 Rubric 디멘션 스코어 + observation 렌더링 컴포넌트로 교체. **질문셋 레벨**에서 한 번만 렌더 (timestamp별이 아님) |
| `frontend/src/components/feedback/feedback-panel.tsx` (L49-50) | ContentTab에 Rubric/Synthesizer 응답 전달. Delivery tab은 기존 경로 유지 |
| `frontend/src/components/feedback/delivery-tab.tsx` | **변경 없음** — vocal/attitude/vision 계속 렌더 |
| `frontend/src/hooks/useQuestionSetFeedback.ts` | 응답 구조 변경 반영. `content`는 질문셋 단위, `delivery`는 timestamp 단위 |

### Database (수정)

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/migration/V29__drop_lambda_content_columns.sql` | 신규. `ALTER TABLE timestamp_feedback DROP COLUMN verbal_comment, DROP COLUMN accuracy_issues, DROP COLUMN coaching_structure, DROP COLUMN coaching_improvement;` (plan-11의 V28 `nonverbal_score` 생성 이후 순서) |
| `backend/src/main/resources/db/migration/rollback/V29__rollback.sql` | 신규. 컬럼 재생성 SQL (응급 롤백용, 데이터 복구는 불가 명시) |

## 상세

### Cut-over 순서 (단일 PR 또는 짧은 PR chain)

1. **PR-1 (Lambda)**: Gemini 프롬프트/handler 정리. Lambda 단독 배포 → Lambda 출력 JSON에 `verbal`/`technical` 없음 확인.
2. **PR-2 (Backend)**: `SaveFeedbackRequest` + `TimestampFeedback` + `TimestampFeedbackMapper` + `TimestampFeedbackResponse`에서 content 필드 제거. Rubric/Synthesizer 응답 경로 연결. V29 migration. ECR 신규 태그 배포와 동시에 cut-over (runtime toggle 없음).
3. **PR-3 (Frontend)**: types + content-tab + feedback-panel 수정. 배포.
4. **PR 순서**: Lambda → BE → FE (CLAUDE.md의 BE/FE 분리 규칙 준수).

### 과거 인터뷰 처리

- V29 drop 이후 과거 인터뷰의 `timestamp_feedback` 레코드는 해당 컬럼이 없음 → FE는 `content` 필드 null로 수신
- FE 렌더: "이 인터뷰는 이전 포맷으로 저장되어 내용 피드백을 다시 볼 수 없습니다" 메시지 표시 (delivery 탭은 정상)
- 사용자 결정(2026-04-22): 이 UX 저하 수용

### Rubric 실패 시 Fallback 정책

- **Lambda content fallback 없음** — plan-13 cut-over 후 Lambda는 content를 생성하지 않음. 존재하지 않는 fallback을 가정 금지.
- Rubric 실패 turn: `rubricStatus=FAILED` 저장, Synthesizer는 해당 turn을 점수 집계에서 제외
- FE Content 탭: 일부 turn이 FAILED여도 나머지 turn 기반으로 Synthesis 렌더. 전체 turn FAILED 시 "내용 피드백 생성 중 일시 오류. 관리자에게 문의" 배너 (plan-09 Admin 재시도 엔드포인트로 복구 가능)

### 응급 롤백 계획

본 plan은 **단방향 변경** (Gemini 프롬프트 + DB drop). 완전 롤백은 불가능하지만 위기 시:

1. ECR 이미지 이전 태그 재배포 → Content 탭 공백 (임시, Rubric 미실행 상태)
2. Lambda 함수는 이전 버전 alias 로 복구 (신규 인터뷰만 영향)
3. V29 rollback migration 실행 — 컬럼 재생성 (신규 인터뷰 대상으로만 저장 가능, 과거 데이터는 복구 불가)
4. Backend DTO/Entity 이전 커밋으로 revert

**의사 결정**: 본 cut-over는 "Rubric 품질이 production-ready" 판정 후 진행. 롤백 발생 시 사용자 노출 최소화 (하루 내 re-flag-off + Lambda re-deploy).

### 검증 가능한 Gemini 프롬프트 구조 (After)

```
섹션 (총 4개, 기존 5개 → 4개 축소):

### 1. transcript
모든 발화 전사. (기존 유지)

### 2. vocal — 음성 특성
fillerWords, speechPace, toneConfidenceLevel, emotionLabel, p/n/s. (기존 유지)

### 3. attitude — 태도·인상
p/n/s. 시각 언급 금지. vocal과 중복 금지. (기존 유지)

### 4. overall_delivery — 종합 (delivery 전용)
음성 + 태도 + (비언어)를 종합 관점에서.
🚫 기술 내용(정확성/코칭) 언급 금지 — 위반 시 재생성.
🚫 accuracyIssues/coaching 관련 어휘 금지.

## 응답 형식 (JSON만)
{
  "transcript": "...",
  "vocal": { ... },
  "attitude": { "positive": "", "negative": "", "suggestion": "" },
  "overall_delivery": { "positive": "", "negative": "", "suggestion": "" }
}
```

## 담당 에이전트

- Implement (Lambda): `backend` + `ai-media-specialist` — Gemini 프롬프트 수정, handler 정리, Python 테스트
- Implement (Backend): `backend` — DTO/Entity/Mapper 정리, Rubric/Synthesizer 응답 경로 연결, V29 migration
- Implement (Frontend): `frontend` — types/content-tab 재설계, Rubric 스코어 시각화
- Review: `architect-reviewer` — Lambda/BE/FE 계약 일관성, content/delivery 경계 누수 없음
- Review: `code-reviewer` — 응급 롤백 경로 검증, DB drop 안전성, DTO 분리 (Entity 직접 반환 금지)
- Review: `designer` — content-tab UI 변경 (Rubric 디멘션 점수 + observation 시각화가 기존 UX 연속성 유지하는지)
- Review: `qa` — 신규 인터뷰 E2E + 과거 인터뷰 회귀 검증

## 검증

### Cut-over 전 (스테이징)

1. **Gemini 프롬프트 출력 검증**: 10개 샘플 인터뷰를 수정된 프롬프트로 처리 → 출력 JSON에 `verbal`/`technical` 키 부재 (100%), `overall_delivery`에 기술 내용 어휘 미포함 (정규식 검증)
2. **Lambda 응답 latency 감소**: p95 latency −500ms 이상 (baseline 대비)
3. **Lambda 프롬프트 토큰 수 감소**: −30% 이상
4. **Backend contract test**: `SaveFeedbackRequest`에 제거된 필드 포함된 요청 → 200 OK (무시) + 로그 WARN 남기는지 확인 (Lambda 배포 타이밍 차이 방어)
5. **FE 렌더링**: 스테이징 DB 샘플로 Content 탭이 Rubric 디멘션 점수 + observation 렌더, Delivery 탭은 기존대로 vocal/attitude/vision 렌더

### Cut-over 후 (프로덕션)

6. **신규 인터뷰 E2E**: 실제 인터뷰 1건 종료 → Lambda callback에서 content 필드 0개 확인, DB `timestamp_feedback`에 해당 컬럼 없음, `turn_rubric_scores` 레코드 생성, 피드백 페이지 정상 렌더
7. **과거 인터뷰 회귀**: 과거 인터뷰 페이지 로드 → Content 탭 "이전 포맷 안내" 배너, Delivery 탭 정상 렌더, 500 에러 0건
8. **Rubric 실패 시나리오**: 의도적으로 Rubric LLM 실패 유발 → Content 탭 "일시 오류" 배너 + 관리자 재시도 가능, Delivery 탭 정상
9. **프로덕션 Rubric 품질 SLO**: Cut-over 후 1주간 Rubric 실패율 <1%, 사용자 피드백 페이지 500 에러 <0.1%
10. **응급 롤백 리허설**: 스테이징에서 ECR 이전 태그 재배포 → Content 탭 공백 배너 확인 → 신규 태그 재배포 복구. MTTR <10분.
11. `progress.md` 13 → Completed

## Out of Scope (재확인)

- Rubric Scorer 자체 구현 — plan-08
- Feedback Synthesizer 자체 구현 — plan-09
- Lambda Nonverbal 수치 필드 (speed_variance 등) — plan-11a
- Lambda Vision 블록 (eyeContact/posture/expression) — 변경 없음
- 과거 인터뷰 Rubric 소급 적용 — 별도 검토 (운영 비용 높음)
- `verbal_analyzer.py` legacy 경로 **완전 제거** — 별도 PR (본 plan은 content 필드만 정리)

## Exit Criteria

- Rubric/Synthesizer는 단일 경로로 항상 실행 (runtime toggle 없음, 배포 단위로 롤백)
- V29 rollback migration 파일 보관 (archival 목적, 실행 금지)
