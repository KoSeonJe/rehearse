# Plan 01: QuestionCategory → InterviewType 전환

> 상태: Draft
> 작성일: 2026-04-12
> 선행 태스크 (blocking): 다른 모든 plan은 이 태스크 완료 후 진행
>
> **PR 분할 규칙**: 프로젝트 컨벤션상 BE/FE PR이 분리되어야 하므로 이 태스크는 두 개의 PR로 나누어 진행한다.
> - **Plan 01-BE**: backend 변경 + Flyway V21 마이그레이션 (먼저 머지)
> - **Plan 01-FE**: frontend `category` 값 한글 라벨 치환 (BE 머지 후 진행)

## Why

`QuestionCategory(RESUME, CS)`는 `InterviewType`(12개)의 다운그레이드 버전이다. `SYSTEM_DESIGN`, `LANGUAGE_FRAMEWORK`, `INFRA_CICD`, `CLOUD`, `DATA_PIPELINE` 등 세분화된 타입이 전부 `QuestionCategory.CS` 하나로 뭉개져 의미가 손실된다. 또한 `FeedbackPerspective(TECHNICAL/BEHAVIORAL/EXPERIENCE)`와도 개념이 부분 중복되어 모델 일관성을 해친다.

복습 북마크 기능은 `InterviewType` 기반 카테고리 그룹핑을 전제로 설계되었으므로, 이 리팩토링이 선행되어야 본 기능 구현에서 category 조회 경로가 단순해진다.

## 전략: 컬럼 유지 + 값 전환 (DROP이 아님)

**제약 사항**: `Interview`는 `Set<InterviewType>` (복수)를 가지므로, `questionSet.getInterview().getType()` 같은 단일 메서드로 대체할 수 없다. 각 QuestionSet은 질문 생성 시 특정 하나의 InterviewType에 대응하므로, **컬럼을 유지하고 값을 InterviewType 문자열로 전환**한다.

**현재 데이터 흐름**:
1. AI 프롬프트가 `"RESUME"` 또는 `"CS"` 문자열을 반환 (question-generation.txt:32)
2. QuestionPool(캐시)의 category는 `"자료구조"`, `"운영체제"` 등 한글 문자열
3. `parseQuestionCategory()`가 위를 `QuestionCategory.RESUME`/`CS` enum으로 변환 (실패 시 CS 폴백)
4. DB에는 `"RESUME"` 또는 `"CS"`만 저장됨

**변경 후 흐름**:
1. cacheable 경로: 루프 변수 `InterviewType type`을 직접 `type.name()`으로 저장
2. fresh 경로: AI 응답의 `questionCategory`를 InterviewType으로 매핑 시도, 실패 시 호환 폴백
3. DB에 `"CS_FUNDAMENTAL"`, `"SYSTEM_DESIGN"`, `"RESUME_BASED"` 등 저장
4. 기존 데이터: `UPDATE` 문으로 `RESUME` → `RESUME_BASED`, `CS` → `CS_FUNDAMENTAL` 변환

## 생성/수정 파일

### Plan 01-BE (Backend PR)

| 파일 | 작업 |
|------|------|
| `backend/src/main/resources/db/migration/V21__convert_question_set_category_to_interview_type.sql` | 기존 데이터 변환 + 컬럼 길이 확장 |
| `backend/src/main/java/com/rehearse/api/domain/questionset/entity/QuestionSet.java` | `@Enumerated(EnumType.STRING) QuestionCategory category` → `@Column(length = 50) String category` |
| `backend/src/main/java/com/rehearse/api/domain/questionset/entity/QuestionCategory.java` | **파일 삭제** |
| `backend/src/main/java/com/rehearse/api/domain/interview/service/QuestionGenerationService.java` | `parseQuestionCategory()` 삭제. cacheable: `.category(type.name())`, fresh: `.category(resolveInterviewType(gq.getQuestionCategory()))` |
| `backend/src/main/java/com/rehearse/api/domain/questionset/dto/QuestionSetResponse.java` | `QuestionCategory category` → `String category`, 매핑은 `questionSet.getCategory()` 그대로 (이미 String) |
| `backend/src/main/java/com/rehearse/api/infra/ai/MockAiClient.java` | `QuestionCategory` import 참조 확인 및 제거 |
| `backend/src/main/resources/prompts/template/question-generation.txt` | AI 프롬프트의 questionCategory 설명을 InterviewType 값으로 변경 |
| 테스트 7개 | `.category(QuestionCategory.CS)` → `.category("CS_FUNDAMENTAL")` 등으로 변경 |

**안전 확인 (수정 불필요)**:
- `QuestionPool.category`: String 타입 — QuestionCategory enum 미사용
- `CsSubTopic`: QuestionCategory 무관
- `QuestionCacheKeyGenerator`: InterviewType/Position/Level 기반 — 무관
- `parseFeedbackPerspective()`: String `"RESUME"` 비교 — enum 미사용, 변경 불필요

### Plan 01-FE (Frontend PR, BE 머지 후)

| 파일 | 작업 |
|------|------|
| `frontend/src/constants/interview-type-labels.ts` | **신규 생성** — InterviewType → 한글 라벨 매핑 상수 + `getInterviewTypeLabel()` 함수 |
| `frontend/src/types/interview.ts` | `category: string` 필드 유지 (값이 InterviewType 문자열로 변경될 뿐) |
| `frontend/src/pages/interview-feedback-page.tsx` | 섹션 제목에 `getInterviewTypeLabel(category)` 적용 |
| `frontend/src/pages/interview-analysis-page.tsx` | 분석 진행 라벨에 `getInterviewTypeLabel(category)` 적용 |
| `frontend/src/hooks/use-interview-session.ts` | `category: qSet.category` 유지 (값만 변경됨) |
| `frontend/src/components/interview/question-display.tsx` | 배지에 `getInterviewTypeLabel()` 적용 (이미 falsy 체크 있음) |
| `frontend/src/components/interview/question-card.tsx` | falsy 체크 추가 + `getInterviewTypeLabel()` 적용 |
| `frontend/src/components/home/key-features-section.tsx` | **수정 불필요** — 자체 로컬 category (시선/자세/표정)로 QuestionCategory 무관 |

## 상세

### Plan 01-BE 단계 (반드시 순서대로)

1. **[완료] 사전 조사**: QuestionPool, CsSubTopic, QuestionCacheKeyGenerator — 모두 영향 없음 확인
2. `QuestionSet` 엔티티: `@Enumerated` 제거, `QuestionCategory category` → `String category` (길이 50)
3. `QuestionGenerationService`:
   - `parseQuestionCategory()` 메서드 삭제
   - cacheable 경로 (line 115): `.category(type.name())` — 루프 변수 `InterviewType type` 직접 사용
   - fresh 경로 (line 157): `.category(resolveInterviewType(gq.getQuestionCategory()))` — 새 메서드
   - `resolveInterviewType()` 신규: InterviewType 파싱 시도 → 실패 시 "RESUME"→"RESUME_BASED", 기타→"CS_FUNDAMENTAL" 폴백
4. DTO `QuestionSetResponse`: `QuestionCategory` → `String`, import 제거
5. `QuestionCategory.java` 파일 삭제
6. AI 프롬프트 `question-generation.txt`: questionCategory 설명을 InterviewType 값 목록으로 변경
7. Flyway V21 마이그레이션:
   ```sql
   UPDATE question_set SET category = 'RESUME_BASED' WHERE category = 'RESUME';
   UPDATE question_set SET category = 'CS_FUNDAMENTAL' WHERE category = 'CS';
   ALTER TABLE question_set MODIFY COLUMN category VARCHAR(50) NOT NULL;
   ```
8. 전체 backend 테스트 재실행 및 수정
9. H2 환경에서 면접 생성 E2E 스모크 테스트

### Plan 01-FE 단계

1. **BE PR 머지 완료 확인** (API 응답에서 `category` 값이 InterviewType 문자열로 변경)
2. `interview-type-labels.ts` 신규 생성 (12개 InterviewType → 한글 라벨 매핑)
3. 피드백/분석 페이지, 질문 카드/디스플레이에서 `getInterviewTypeLabel()` 적용
4. `npm run lint` + `npm run build` + `npm run test` 통과
5. 영향받는 화면(홈, 면접 분석, 피드백, 질문 표시/카드) 수동 확인

### 리스크
- **기존 데이터 변환**: `RESUME`/`CS` 외 다른 값이 DB에 있을 가능성 → V21 마이그레이션에서 fallback 처리 (`CS` 이외 값은 `CS_FUNDAMENTAL`로 변환)
- **AI 프롬프트 변경**: fresh 질문 생성 시 AI가 새 InterviewType 값을 올바르게 반환하는지 확인 필요
- **BE/FE 사이 짧은 불일치 구간**: BE 머지 후 FE 반영 전까지 영문 enum 값(`CS_FUNDAMENTAL`)이 섹션 제목에 그대로 노출. 시간 갭 최소화 권장.

## 담당 에이전트

### Plan 01-BE
- Implement: `backend` — 엔티티/서비스/DTO/마이그레이션/프롬프트 수정
- Review: `architect-reviewer` — 레이어링, bounded context, 전환 전략 완전성
- Review: `database-architect` — 마이그레이션 안전성, 데이터 변환 검증

### Plan 01-FE
- Implement: `frontend` — 한글 라벨 매핑, 컴포넌트/페이지 적용
- Review: `code-reviewer` — 타입 안전성, UI 회귀

## 검증

### Plan 01-BE
- `./gradlew :backend:test` 전체 통과
- `rg 'QuestionCategory' backend/src/` 결과 0건
- H2 로컬 실행 후 면접 생성 플로우 E2E 정상 작동
- API 응답에서 `questionSets[].category`가 InterviewType 문자열로 반환
- `progress.md` 상태 업데이트

### Plan 01-FE
- `npm run lint` / `npm run build` / `npm run test` 통과
- 피드백 페이지 섹션 제목이 한글 라벨로 정상 표시 (예: "시스템 설계")
- 분석 페이지 진행 라벨이 한글로 정상 표시
- 질문 카드 배지가 한글 라벨로 표시
- 홈 페이지 key-features-section 정상 (영향 없음 확인)
