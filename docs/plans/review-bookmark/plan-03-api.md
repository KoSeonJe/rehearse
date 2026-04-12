# Plan 03: Service + Controller + DTO (API 엔드포인트)

> 상태: Draft
> 작성일: 2026-04-12
> 선행: Plan 02 완료

## Why

클라이언트(피드백 페이지, 전역 `/review-list` 페이지)가 북마크를 생성/조회/삭제/상태 전환할 수 있는 HTTP 인터페이스를 제공한다. 목록 조회 시 프론트가 추가 조인 없이 즉시 렌더할 수 있도록 DTO에 질문 · 내 답변 · 모범 답변 · 면접 메타데이터를 모두 포함한다.

## 생성/수정 파일

| 파일 | 작업 |
|------|------|
| `backend/.../reviewbookmark/service/ReviewBookmarkService.java` | 비즈니스 로직 |
| `backend/.../reviewbookmark/service/ReviewBookmarkQueryService.java` | 목록/존재 조회 전용(읽기 분리) |
| `backend/.../reviewbookmark/controller/ReviewBookmarkController.java` | REST 엔드포인트 5개 |
| `backend/.../reviewbookmark/dto/CreateReviewBookmarkRequest.java` | POST body |
| `backend/.../reviewbookmark/dto/UpdateBookmarkStatusRequest.java` | PATCH body (`resolved: boolean`) |
| `backend/.../reviewbookmark/dto/ReviewBookmarkResponse.java` | 단건 응답 |
| `backend/.../reviewbookmark/dto/ReviewBookmarkListItem.java` | 목록 항목(질문/답변/모범답변/면접 메타 포함) |
| `backend/.../reviewbookmark/dto/ReviewBookmarkListResponse.java` | 목록 응답 래퍼 `{items, total}` record |
| `backend/.../reviewbookmark/dto/BookmarkExistsResponse.java` | `{items: [{timestampFeedbackId, bookmarkId}]}` — DELETE에 필요한 bookmarkId까지 쌍으로 반환 |
| `backend/.../reviewbookmark/dto/BookmarkIdPair.java` | `GET /exists` JPQL constructor projection record (wire contract 일부) |
| `backend/.../reviewbookmark/dto/BookmarkStatusFilter.java` | `status` 쿼리파라미터 enum (ALL / IN_PROGRESS / RESOLVED), 미확인 값은 400 |
| `backend/.../reviewbookmark/exception/ReviewBookmarkException.java` | 도메인 예외 |

## 상세

### 엔드포인트 시그니처

```
POST   /api/review-bookmarks
  body:  { timestampFeedbackId: number }
  res:   201 ReviewBookmarkResponse
  error: 404 TIMESTAMP_FEEDBACK_NOT_FOUND
         409 BOOKMARK_ALREADY_EXISTS

DELETE /api/review-bookmarks/{id}
  res:   204
  error: 404 BOOKMARK_NOT_FOUND
         403 FORBIDDEN_ACCESS (타인 소유)

GET    /api/review-bookmarks
  query: status=all|in_progress|resolved (기본: all)
  res:   200 { items: ReviewBookmarkListItem[], total: number }

PATCH  /api/review-bookmarks/{id}/status
  body:  { resolved: boolean }
  res:   200 ReviewBookmarkResponse
  error: 404, 403

GET    /api/review-bookmarks/exists
  query: timestampFeedbackIds=1,2,3
  res:   200 { items: [{ timestampFeedbackId: number, bookmarkId: number }] }
```

**주의**: 응답에 `bookmarkId`를 포함해야 프론트가 DELETE 호출 시 별도 조회 없이 바로 삭제 가능. 단순 boolean 배열은 사용 금지.

### 서비스 책임 분리

- **`ReviewBookmarkService`**: `create`, `delete`, `updateStatus` (쓰기/트랜잭션)
- **`ReviewBookmarkQueryService`**: `listByUser`, `findBookmarkPairs` (`@Transactional(readOnly = true)`)
- 인증: `@AuthenticationPrincipal Long userId` 패턴 사용 (기존 프로젝트 컨벤션과 일치 — 확인 완료: `QuestionSetController` 등)
- **소유권 검사**: `ReviewBookmarkRepository.findOwnerIdById(bookmarkId)`로 `user_id` 만 직접 조회한 뒤 `userId`와 비교. `bookmark.getUser().getId()` 경로는 User 엔티티 프록시 초기화로 추가 쿼리가 발생하므로 금지.

### 중복 처리 전략

POST 시:
1. `existsByUserIdAndTimestampFeedbackId` 선검사 → 존재 시 409 반환
2. 동시 요청으로 `DataIntegrityViolationException` 발생 시 catch → 409 반환 (idempotent)

클라이언트는 409를 "이미 담긴 상태"로 동기화하고 사용자에게 에러 토스트를 노출하지 않는다.

### 목록 DTO 구조

```java
public record ReviewBookmarkListItem(
    Long id,                      // bookmark id
    Long timestampFeedbackId,
    String questionText,
    String modelAnswer,           // nullable
    String transcript,            // nullable (분석 전일 수 있음)
    String coachingImprovement,   // AI 피드백 요약
    String interviewType,         // e.g. "SYSTEM_DESIGN"
    String interviewTitle,        // e.g. "시스템 디자인 모의면접"
    LocalDateTime interviewDate,
    LocalDateTime createdAt,      // 북마크 담은 시점
    LocalDateTime resolvedAt      // nullable
) {
    public boolean isResolved() { return resolvedAt != null; }
}
```

카테고리 그룹핑은 **프론트**가 `interviewType` 기준으로 수행 (상위 그룹 매핑 테이블은 프론트 상수).

### 상태 필터 쿼리

JPQL:
```java
@Query("""
    SELECT rb FROM ReviewBookmark rb
    WHERE rb.user.id = :userId
      AND (:resolved IS NULL
           OR (:resolved = true  AND rb.resolvedAt IS NOT NULL)
           OR (:resolved = false AND rb.resolvedAt IS NULL))
    ORDER BY rb.createdAt DESC
""")
```

또는 `all` 시 분기 호출. 가독성 우선으로 Service에서 3-way 분기 권장.

## 담당 에이전트

- Implement: `backend` — 서비스, 컨트롤러, DTO, 예외
- Review: `code-reviewer` — 보안(소유권 검사, 인증), idempotent 로직, DTO 설계
- Review: `architect-reviewer` — Service/QueryService 분리, 레이어링

## 검증

- `./gradlew :backend:test` 통과
- `MockMvc` 기반 컨트롤러 통합 테스트로 5개 엔드포인트 각각의 happy path + 에러 경로 검증
- 동시 POST race condition 시뮬레이션 (병렬 테스트) → 409 반환 확인
- 타인 소유 북마크 DELETE/PATCH 시 403 확인
- `progress.md` 상태 업데이트 (Task 3 → Completed)
