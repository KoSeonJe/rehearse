# 복습 북마크 (Review Bookmark) — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-12

## Why

### 1. Why? — 어떤 문제를 해결하는가
사용자가 모의면접 피드백을 받은 뒤 "이 답변은 다시 연습해야겠다"고 느껴도, 지금은 그 질문을 다시 꺼내 볼 수단이 없다. 면접이 끝나면 해당 피드백은 특정 면접 세션 내부에 묻히고, 여러 면접에 걸쳐 쌓인 약점을 한곳에서 볼 경로가 없다. 그 결과 "복습"이라는 학습 루프가 닫히지 않는다.

### 2. Goal — 구체적인 결과물과 성공 기준
- 사용자가 피드백 페이지에서 개별 답변을 **한 번의 클릭으로 복습 북마크에 담기** 가능
- 전역 사이드바의 "복습 북마크" 메뉴에서 담긴 답변을 **카테고리별**(InterviewType 기반)로 모아 보기 가능
- 각 항목은 **질문 + 내 답변(transcript) + 모범 답변(modelAnswer)**을 함께 비교 가능
- **해결 상태(연습 중 / 해결됨)** 토글로 사용자 자체 진도 추적 가능
- 최대 수백 건 목록 조회 응답 500ms 이내
- 최초 사용자의 북마크 발견을 돕는 **1회 코치마크** 제공

### 3. Evidence — 근거
- 현재 피드백 페이지(`frontend/src/components/feedback/feedback-panel.tsx`)는 면접 단위 단일 뷰로만 설계되어 있어 면접 간 횡단 조회 경로가 없음
- `TimestampFeedback`에는 `transcript`, `coachingImprovement`, `accuracyIssues` 등 약점 신호가 이미 존재
- `Question.modelAnswer` 필드가 이미 존재 → 추가 데이터 생성 없이 기존 자산 재활용으로 구현 가능
- "못한 답변"을 AI로 자동 판정하는 대안이 있으나, 현재 스키마에 숫자형 점수가 없어 신규 프롬프트/Lambda/스키마 변경이 필요해 MVP 비용이 과다

### 4. Trade-offs — 포기하는 것
- **AI 자동 판정 포기, 수동 북마크 선택** — 사용자 의도 100% 반영 + 구현 경량화. 배포 후 행동 데이터가 쌓이면 v2에서 AI 추천 후보를 병행 가능
- **시도(attempt) 단위 저장, 질문 단위 머지 포기** — 구현 단순성 확보. 같은 질문을 여러 면접에서 만나면 별도 항목으로 쌓임 (YAGNI)
- **메모/태그/알림/공유/통계 전부 미포함** — MVP는 "담기 + 꺼내 보기 + 해결 체크"만 담당

---

## 목표

1. `ReviewBookmark` 도메인 + 데이터 모델 신설
2. 피드백 페이지에 "복습 북마크에 담기" 토글 + 최초 사용자 코치마크 + 토스트
3. 전역 `/review-list` 풀페이지 — 상태 필터, 카테고리 그룹, 내 답변 vs 모범답변 비교
4. 선행 리팩토링: `QuestionCategory` enum 삭제 → `QuestionSet.category`를 InterviewType String으로 전환
5. 구현 이전 HTML 프로토타입으로 UI/UX 확정 후 착수

## 아키텍처 / 설계

### 도메인 배치

신규 패키지: `com.rehearse.api.domain.reviewbookmark`

기존 `questionset/`(면접 생성물)과 bounded context를 분리한다. 북마크는 **사용자 학습 행위** aggregate이고 `questionset`은 **면접 생성물** aggregate로, 책임이 다르다. `user/`는 인증/프로필 책임이라 여기에 두지 않는다.

### 데이터 모델

```sql
-- V21__create_review_bookmark.sql (정확한 버전은 구현 시 확인)
CREATE TABLE review_bookmark (
    id                     BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id                BIGINT   NOT NULL,
    timestamp_feedback_id  BIGINT   NOT NULL,
    resolved_at            DATETIME NULL,
    created_at             DATETIME NOT NULL,
    CONSTRAINT fk_rb_user FOREIGN KEY (user_id)
        REFERENCES users(id) ON DELETE CASCADE,
    CONSTRAINT fk_rb_tsf  FOREIGN KEY (timestamp_feedback_id)
        REFERENCES timestamp_feedback(id) ON DELETE CASCADE,
    CONSTRAINT uk_rb_user_tsf UNIQUE (user_id, timestamp_feedback_id)
);

CREATE INDEX idx_rb_user_created ON review_bookmark (user_id, created_at DESC);
```

**설계 결정**:
- **`resolved_at` NULL 방식**으로 해결 상태 표현 — enum status 컬럼 불필요, 해결 시점을 공짜로 획득. 3번째 상태가 필요해지면 추후 enum 컬럼 추가 마이그레이션 가능 (무손실 전환)
- **`UNIQUE(user_id, timestamp_feedback_id)`** — 동시 요청(더블 클릭) race condition 방지. 애플리케이션 레벨 체크는 원자성 보장 불가
- **`ON DELETE CASCADE`** — 면접/피드백 삭제 시 북마크도 함께 삭제. Dangling 항목으로 인한 UI NPE 방지. 면접 삭제는 드물고 의도적이므로 유실 수용 가능
- **카테고리 denormalize 금지** — `QuestionSet.category`(리팩토링 후 InterviewType 문자열 저장)가 source of truth. 조인 경로: `ReviewBookmark → TimestampFeedback → QuestionSetFeedback → QuestionSet.category`. Interview까지 조인할 필요 없이 QuestionSet에서 직접 조회 가능. MVP 규모에서 조인 비용 무시 가능

### 선행 리팩토링: QuestionCategory → InterviewType 전환

**문제**: `QuestionCategory(RESUME, CS)`는 `InterviewType`(12개)의 다운그레이드 버전이다. `InterviewType.CS_FUNDAMENTAL`, `SYSTEM_DESIGN`, `LANGUAGE_FRAMEWORK`, `INFRA_CICD`, `CLOUD`, `DATA_PIPELINE` 등이 전부 `QuestionCategory.CS` 하나로 뭉개져 의미가 손실된다. `FeedbackPerspective(TECHNICAL/BEHAVIORAL/EXPERIENCE)`와도 의미가 부분 중복된다.

**제약 사항**: `Interview`는 `Set<InterviewType>` (복수)를 가지므로 `questionSet.getInterview().getType()` 같은 단일 메서드로 대체 불가. 각 QuestionSet은 질문 생성 시 특정 하나의 InterviewType에 대응하므로, **컬럼을 유지하고 값을 InterviewType 문자열로 전환**해야 한다.

**조치**: `QuestionCategory` enum 삭제 + `QuestionSet.category` 컬럼을 `String` 타입으로 변환하여 `InterviewType.name()` 값 저장. 기존 데이터는 `RESUME` → `RESUME_BASED`, `CS` → `CS_FUNDAMENTAL`로 마이그레이션.

**영향 파일 (사전 조사)**:
- `backend/src/main/java/com/rehearse/api/domain/questionset/entity/QuestionSet.java` — `@Enumerated` → `String`
- `backend/src/main/java/com/rehearse/api/domain/questionset/entity/QuestionCategory.java` — 삭제
- `backend/src/main/java/com/rehearse/api/domain/interview/service/QuestionGenerationService.java` — `parseQuestionCategory()` 삭제, InterviewType.name() 직접 사용
- `backend/src/main/java/com/rehearse/api/domain/questionset/dto/QuestionSetResponse.java` — `QuestionCategory` → `String`
- `backend/src/main/resources/prompts/template/question-generation.txt` — AI 프롬프트 questionCategory 설명 변경
- QuestionPool, CsSubTopic, QuestionCacheKeyGenerator — **영향 없음** (String 기반 / 무관)
- 관련 테스트 7개

### API 엔드포인트

| Method | Path | 용도 |
|--------|------|------|
| POST | `/api/review-bookmarks` | 북마크 추가 (body: `{timestampFeedbackId}`) |
| DELETE | `/api/review-bookmarks/{id}` | 북마크 삭제 |
| GET | `/api/review-bookmarks` | 내 북마크 목록 (query: `status=all\|in_progress\|resolved`) |
| PATCH | `/api/review-bookmarks/{id}/status` | 해결 상태 토글 (body: `{resolved}`) |
| GET | `/api/review-bookmarks/exists` | 다중 TimestampFeedback 북마크 여부 배치 조회 |

- 목록 응답에는 `question`, `transcript`, `modelAnswer`, `interviewType`, `interviewDate`, `resolvedAt`, `coachingImprovement` 포함 → 프론트 조인 없이 바로 렌더
- `GET /exists`는 피드백 페이지 로드 시 한 번만 호출해 카드 상태 일괄 동기화 (N+1 방지)
- 중복 북마크 POST는 **409 Conflict**로 응답, 프론트에서 "이미 담김" 상태로 동기화

### UI/UX 요구사항 (요약)

**피드백 페이지 북마크 버튼** — `FeedbackCard` 헤더 우측:
- 아이콘: `ListPlus` (기본) / `ListChecks` (토글 후)
- 레이블: "복습 북마크에 담기" / "복습 북마크에 담김"
- 색상: `#FFF1EE` 배경 + `#D94A3A` 텍스트 (WCAG AA 대비 확보, `#FF6B5B`는 대비 부족)
- 1회 코치마크 popover + 1회 추가 토스트 + 3초 Undo
- 접근성: `aria-pressed`, 동적 `aria-label`, `focus-visible:ring`

**전역 `/review-list` 풀페이지**:
- 헤더: 제목 + 총 개수 + 한 줄 설명
- 필터 바: 전체/연습 중/해결됨 + 카테고리 드롭다운
- `InterviewType` 기반 UI 상위 그룹(5~6개)별 섹션
- 카드: 질문 + 면접 날짜 + 해결 상태 배지 + 확장 시 내 답변 vs 모범답변 좌우 비교
- 빈 상태: 첫 북마크 유도 안내

**세부 디자인은 Plan 04 HTML 프로토타입 단계에서 `designer` 에이전트가 확정한다.**

## Scope

### In
- `review_bookmark` 테이블 + `ReviewBookmark` 엔티티/Repository/Service/Controller/DTO
- 5개 API 엔드포인트
- 피드백 페이지 북마크 버튼 + 코치마크 + 토스트
- 전역 `/review-list` 페이지
- 전역 사이드바 "복습 북마크" 메뉴 항목
- `QuestionCategory` 삭제 선행 리팩토링
- 단위/통합/주요 E2E 테스트

### Out
- AI 자동 약점 판정/스코어링
- 메모 / 태그 / 알림 / 리마인더
- 같은 질문 시도 병합 / 히스토리 뷰
- 북마크 공유 / 팀 기능
- 복습 진행률 통계 대시보드
- 모바일 전용 네이티브 레이아웃(기본 반응형만 보장)

## 제약조건 / 환경

- Spring Boot 3.x + JPA / MySQL 8 (prod) / H2 (dev) / Flyway
- React 18 + TS strict / Tailwind / TanStack Query / Zustand
- Lucide React 아이콘 이미 포함
- 인증: 기존 `@AuthenticationPrincipal` 패턴 유지
- Flyway 버전은 구현 시 현재 최신 버전 확인 후 +1/+2로 할당

## 열린 질문 (배포 후 계측/결정)

1. `InterviewType` 12개를 UI에서 어떤 상위 그룹(5~6개)으로 묶을지 — Plan 06에서 `designer` 에이전트가 확정
2. 코치마크 효과 측정 지표 설정 (최초 사용자 북마크 전환율)
3. v2 AI 추천 후보 기능 도입 시 스키마 확장 여부
