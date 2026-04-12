# Plan 07: 테스트 (단위 + 통합 + E2E)

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 02, 03, 05, 06 완료 후 최종 보강 (각 plan 내부 테스트는 이미 포함됨)

## Why

복습 북마크 기능은 사용자 개인 학습 자산을 저장/삭제/변경하는 기능이므로, 데이터 일관성과 권한 경계 · 동시성 · 접근성이 모두 중요하다. 각 plan에서 기본 테스트를 작성하되, 이 태스크에서는 **누락/엣지 케이스 보강 + E2E 시나리오 검증**을 수행한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/src/test/.../reviewbookmark/ReviewBookmarkServiceTest.java` | 서비스 단위 테스트 (Mockito) |
| `backend/src/test/.../reviewbookmark/ReviewBookmarkQueryServiceTest.java` | 쿼리 서비스 단위 테스트 |
| `backend/src/test/.../reviewbookmark/ReviewBookmarkControllerTest.java` | 컨트롤러 MockMvc 통합 테스트 |
| `backend/src/test/.../reviewbookmark/ReviewBookmarkRepositoryTest.java` | `@DataJpaTest` 기반 리포지터리 테스트 |
| `frontend/src/components/review/__tests__/*.test.tsx` | 주요 컴포넌트 렌더/상호작용 테스트 (Vitest + Testing Library) |
| `frontend/src/hooks/__tests__/use-review-bookmarks.test.ts` | TanStack Query 훅 테스트 (MSW) |

## 상세

### 백엔드 테스트 시나리오

**Repository (`@DataJpaTest`)**
- `existsByUserIdAndTimestampFeedbackId` 존재/미존재 케이스
- `findByUserIdOrderByCreatedAtDesc` 최신순 정렬 + `@EntityGraph` N+1 미발생 (SQL 카운트 검증)
- UNIQUE 제약 위반 시 `DataIntegrityViolationException` 발생 확인
- CASCADE: `TimestampFeedback` 삭제 시 연쇄 삭제 확인

**Service (Mockito)**
- `create` — 정상 / 이미 존재 시 409 / 대상 피드백 없음 404
- `create` — 동시 요청 시뮬레이션: 선검사 통과 후 save에서 `DataIntegrityViolationException` 발생 → 409로 변환
- `delete` — 정상 / 타인 소유 시 403
- `updateStatus` — resolved true/false 전환 / 타인 소유 403 / 미존재 404

**Controller (MockMvc + `@WithMockUser` 또는 커스텀 인증 세팅)**
- 5개 엔드포인트 각각의 2xx/4xx 경로
- 요청/응답 JSON 스키마 검증
- 인증 없이 접근 시 401 (기존 보안 설정 준수)

### 프론트 테스트 시나리오

**`BookmarkToggleButton`**
- 미북마크 상태 렌더 → `aria-pressed="false"` + `ListPlus` 아이콘
- 클릭 후 → `aria-pressed="true"` + `ListChecks` 아이콘 + 배경/텍스트 변경
- 연속 클릭 디바운스 또는 pending 상태 처리 확인

**`ReviewListPage`**
- 빈 목록 → `ReviewEmptyState` 렌더
- 다수 항목 → 그룹 섹션 개수와 항목 수 일치
- 상태 필터 변경 → 쿼리 재호출
- 해결 토글 → optimistic 업데이트 → 서버 응답 후 동기화
- 삭제 확인 모달 → 확인 클릭 시 카드 제거, 취소 클릭 시 유지

**`ReviewCoachMark`**
- localStorage 키 없음 → 렌더
- "알겠어요" 클릭 후 localStorage 설정 + 미렌더
- ESC 키 + 외부 클릭 + 8초 auto-dismiss 모두 작동

**`use-review-bookmarks` 훅 (MSW)**
- `listQuery` 상태 필터별 쿼리 키 분리 확인
- `createMutation` 성공 시 `exists` 쿼리 invalidate 확인
- 409 응답 시 UI 에러 토스트 미노출 + 상태 동기화

### 수동 E2E 체크리스트

1. **핵심 플로우**: 피드백 페이지 → 북마크 추가 → 사이드바 "복습 북마크" 클릭 → 항목 확인 → 해결 토글 → 상태 필터로 격리 → 삭제 → 빈 상태 확인
2. **중복 방지**: 같은 답변 두 번 클릭 시 2번째는 이미 상태로 동기화, 중복 생성 없음
3. **권한**: 다른 사용자 세션으로 직접 PATCH/DELETE 호출 시 403
4. **접근성**: 키보드만으로 북마크 추가/제거, 스크린 리더에서 상태 전환 읽힘
5. **반응형**: 모바일/태블릿/데스크톱 3종 뷰포트 레이아웃 정상
6. **데이터 삭제 연쇄**: 면접 삭제 시 관련 북마크도 함께 제거됨 (DB 확인)

### 성능 기준

- 내 북마크 100건 기준 `GET /api/review-bookmarks` 응답 500ms 이내
- `ReviewListPage` 렌더 LCP 1.5s 이내 (데스크톱 캐시 미사용 기준)

## 담당 에이전트

- Implement: `test-engineer` — 테스트 전략 수립, 누락 커버리지 보강, CI 통과 보장
- Implement: `qa-tester` — 수동 E2E 시나리오 실행 및 결과 리포트
- Review: `code-reviewer` — 테스트 품질, 어서션 완전성, 플래키 테스트 방지

## 검증

- `./gradlew :backend:test` 전체 통과, 핵심 패키지 커버리지 80% 이상
- `npm run test` 전체 통과
- CI(Frontend CI / Backend CI) 통과
- 수동 E2E 체크리스트 전 항목 PASS
- `progress.md` 상태 업데이트 (Task 7 → Completed)
