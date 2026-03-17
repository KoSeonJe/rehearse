# Sprint 0: 녹화-분석-피드백 파이프라인 전환 — Progress

## 상태 요약

| Phase | 설명 | 상태 |
|-------|------|------|
| Phase A | Legacy 제거 + DB 스키마 | Completed |
| Phase B | BE 서비스 레이어 | Completed |
| Phase C~F | Infra + Lambda + FE | In Progress |

## Task 진행 상태

| Task | 설명 | 상태 | PR | 이슈 |
|------|------|------|-----|------|
| Task 0 FE | Legacy 클라이언트 분석 코드 제거 | Completed | #89 | #77 |
| Task 0 BE | feedback 도메인 제거 | Completed | #90 | #77 |
| Task 1 | Flyway V4 + 6엔티티 + 7Enum + 6Repo | Completed | #91 | #78 |
| Task 2-1 | AWS 인프라 (S3, EventBridge, Lambda) | Completed | — | #79 |
| Task 2-2 | AWS S3 SDK + Presigned URL + 조건부 빈 | Completed | #92 → #97 | #79 (부분) |
| Task 3-1 | 면접생성 리팩토링 + QuestionSet + 모범답변 | Completed | #93 → #98 | #80 |
| Task 3-2 | 클라이언트 API 5개 + S3Service 인터페이스 | Completed | #94 → #99 | #81 |
| Task 4 | Lambda용 내부 API 7개 + InternalApiKeyFilter | Completed | #95 → #100 | #82 |
| Task 5 | 좀비 스케줄러 + 면접 완료 자동 집계 | Completed | #96 → #101 | #83 |
| Task 6 | 분석 Lambda (Python, FFmpeg+Whisper+Vision+LLM) | Not Started | — | — |
| Task 7 | 변환 Lambda (WebM→MP4, MediaConvert) | Not Started | — | — |
| Task 8 | [FE] 질문세트 단위 녹화 + S3 업로드 | Completed | — | — |
| Task 9 | [FE] 분석 대기 UI + 상태 폴링 | Completed | — | — |
| Task 10 | [FE] 피드백 뷰어 (영상+타임라인+피드백 동기화) | Not Started | — | — |

## 이슈 상태

| 이슈 | 제목 | 상태 | 닫은 PR |
|------|------|------|---------|
| #77 | Legacy 파이프라인 정리 | Closed | #89 + #90 |
| #78 | DB 스키마 | Closed | #91 |
| #79 | S3 + EventBridge + Lambda + MediaConvert | Open (BE만 완료) | #97 (BE S3 SDK) |
| #80 | 면접 생성 API 리팩토링 | Closed | #98 |
| #81 | 외부 API | Closed | #99 |
| #82 | 내부 API + 인증 필터 | Closed | #100 |
| #83 | 좀비 스케줄러 + 면접 완료 집계 | Closed | #101 |

## PR 트래커

| 원본 PR | 재생성 PR | 브랜치 | 머지 일자 |
|---------|----------|--------|----------|
| #89 | — | feat/legacy-cleanup-fe | 2026-03-17 |
| #90 | — | feat/legacy-cleanup-be | 2026-03-17 |
| #91 | — | feat/db-schema-question-set | 2026-03-17 |
| #92 | #97 | feat/s3-presigned-url | 2026-03-17 |
| #93 | #98 | feat/interview-create-refactor | 2026-03-17 |
| #94 | #99 | feat/question-set-api | 2026-03-17 |
| #95 | #100 | feat/internal-api | 2026-03-17 |
| #96 | #101 | feat/zombie-scheduler | 2026-03-17 |

## 코드 품질 검증 (2026-03-17)

| PR | 설명 | 상태 |
|----|------|------|
| #102 | [BE] refactor: DTO 분리, Filter 개선, N+1 해결 | Merged |
| #103 | [BE] test: 10개 신규 클래스 테스트 추가 (1,903 LOC) | Merged |

## 비고

- PR #92~#96은 스택된 PR 체인이었으나, base 브랜치 삭제로 자동 닫힘 → develop 기반으로 재생성 (#97~#101)
- 이슈 #79는 BE S3 SDK 부분만 완료, AWS 인프라 구축(Task 2-1)은 별도 진행 예정
- 최종 develop 빌드 검증 통과 (2026-03-17)
- 코드 품질 검증 9.3/10 → 3건 개선 완료 (2026-03-17)
- 테스트 커버리지: 기존 7개 → 17개 테스트 클래스 (2026-03-17)
