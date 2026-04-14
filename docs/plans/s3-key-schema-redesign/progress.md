# S3 Key Schema 재설계 — 진행 상황

> 최종 업데이트: 2026-04-12
> 선행 문서: [`requirements.md`](./requirements.md)
> **후행 프로젝트**: [`../prod-environment-setup/`](../prod-environment-setup/requirements.md)

## 태스크 상태

| # | 태스크 | 플랜 문서 | 태그 | 의존 | 상태 | 담당 |
|---|---|---|---|---|---|---|
| 1 | 키 스키마·Lifecycle·EventBridge 규격 확정 (SSOT `docs/architecture/s3-key-schema.md`) | [plan-01](./plan-01-key-schema-definition.md) | `[blocking]` | — | **Completed** | documentation-expert / architect-reviewer |
| 2 | 백엔드 `S3KeyGenerator` 유틸 + `QuestionSetService` 수정 + 단위 테스트 | [plan-02](./plan-02-backend-key-generator.md) | `[parallel]` | 1 | **Completed** | backend / code-reviewer + architect-reviewer |
| 3 | Lambda convert/analysis 신규 전용 파서 + `backup_to_s3` 경로 + Skipped 처리 | [plan-03](./plan-03-lambda-handlers-update.md) | `[parallel]` | 1 | **Completed** | devops-engineer / code-reviewer |
| 4 | dev freeze·drain·레거시 삭제·원샷 컷오버·E2E·24h 모니터링 + prod 문서 업데이트 | [plan-04](./plan-04-dev-rollout-validation.md) | `[blocking]` | 2, 3 | **Completed** (2026-04-14) | devops-engineer + qa / architect-reviewer + code-reviewer |

### 의존 관계

```
plan-01 (SSOT 문서)
   ├─→ plan-02 (백엔드)    ─┐
   └─→ plan-03 (Lambda)     ─┼─→ plan-04 (컷오버)
                             │
                        (병렬 진행 가능)
```

- **병렬 가능**: Task 2·3 (BE/Lambda 독립)
- **블로커**: Task 4는 2·3 둘 다 완료 후 단일 메인터넌스 윈도우에서 원샷 집행

## 정책 요약

- **Lambda 파서**: 신규 스키마 단일 지원, 레거시 코드 경로 미존재
- **미매칭 키**: `{"statusCode":200,"body":"Skipped: not a v1 raw key"}` 즉시 반환
- **전환 방식**: dev freeze + in-flight drain + 레거시 S3/DB 일괄 삭제 + Lambda·BE 동시 배포 + E2E
- **다운타임**: dev 메인터넌스 윈도우 ~90분 (공지 24h 전 필수)
- **"오래 걸리더라도" 수용**: 총 예상 소요 3~5일

## 후행 영향 (prod-environment-setup 업데이트 대상)

Task 4 컷오버 완료 + 24h 안정화 후 다음 문서를 갱신한다:

- [ ] `docs/plans/prod-environment-setup/requirements.md` — 선행 프로젝트 Completed 명시
- [ ] `docs/plans/prod-environment-setup/plan-03-s3-eventbridge-mediaconvert.md` — SSOT 링크 + prefix 검증
- [ ] `docs/plans/prod-environment-setup/progress.md` — 선행 작업 완료 반영
- [ ] `docs/architecture/s3-key-schema.md` Changelog — dev 적용 완료 append

## 진행 로그

### 2026-04-12
- 플랜 문서 세트 Draft 작성 완료
- 생성 파일:
  - `docs/plans/s3-key-schema-redesign/requirements.md`
  - `docs/plans/s3-key-schema-redesign/plan-01-key-schema-definition.md`
  - `docs/plans/s3-key-schema-redesign/plan-02-backend-key-generator.md`
  - `docs/plans/s3-key-schema-redesign/plan-03-lambda-handlers-update.md`
  - `docs/plans/s3-key-schema-redesign/plan-04-dev-rollout-validation.md`
  - `docs/plans/s3-key-schema-redesign/progress.md`
- **정책 결정**: Lambda 구/신 병행 파서 기각 → 신규 전용 + 드레인 컷오버 (사용자 지시)
- **본 프로젝트는 `prod-environment-setup`의 선행 작업**임을 명시

### 2026-04-12 (review pass — 장애/누락 결함 점검)

플랜 세트 코드베이스 대조 리뷰 수행. 실제 코드(`QuestionSetService.java`, `lambda/{convert,analysis}/handler.py`, `lambda/analysis/api_client.py`, `file_metadata` 엔티티, `V1~V20` migration)와 플랜 기술 내용을 전량 교차 검증. 발견된 결함 10건 수정 완료.

**Critical 결함 (수정)**:
1. **Plan 01 §9 ↔ Plan 03 직접 충돌** — "Lambda 호환 파서: 레거시 키 도착 시 기존 동작 유지" 문구가 Plan 03의 "신규 전용, 레거시 코드 경로 미존재" 결정과 정면 충돌. Plan 01을 결정에 정합하도록 재작성.
2. **Plan 04 Scenario 2 검증 불가** — 신규 EventBridge 규칙이 `prefix: interviews/raw/`로 필터링되어 `videos/999/...` 레거시 prefix 업로드는 Lambda를 **트리거조차 하지 않는다**. "Skipped 로그 확인"이 구조적으로 실패. S3 업로드 방식을 제거하고 `aws lambda invoke --qualifier live` 직접 주입으로 교체. 추가 케이스(MP4/feedback/garbage) 4종 포함.
3. **Plan 02 UNIQUE 제약 누락** — `V4:21 CREATE UNIQUE INDEX idx_file_metadata_s3_key`의 존재가 신규 uuid 스키마의 재녹화 호환성 근거인데 플랜에 언급 없음. 명시 추가.

**Important 결함 (수정)**:
4. **Plan 04 TRUNCATE 목록 불완전** — `question_set_feedback`, `question_set_answer`, `question` 누락. V4 FK 체계상 FK 순서 잘못하면 `SET FOREIGN_KEY_CHECKS=0`이어도 잔여 행이 orphan 상태로 남음. 실측 FK 기반으로 순서 재작성 + `users` 유지 명시.
5. **Plan 03 테스트 디렉토리 미존재** — `lambda/analysis/tests/`, `lambda/convert/tests/` 리포에 존재하지 않음. 디렉토리·`__init__.py` 생성 단계를 태스크에 포함.
6. **Plan 03 `lambda/shared/` 모호성** — Option A(각 Lambda 사본) 채택했으나 파일 목록·구현 섹션에 `lambda/shared/s3_keys.py`가 여전히 등장. 미사용 명시 + 구현 섹션 제목을 `lambda/{analysis,convert}/s3_keys.py`로 교체.
7. **Plan 03 grep 명령 버그** — `grep -n "..." lambda/analysis/ lambda/convert/`가 디렉토리를 대상으로 하면서 `-r` 미포함. 재귀 검색 불가. `grep -rn` + `--exclude-dir=tests`로 수정 (테스트 파일에는 legacy 문자열이 rejection 케이스로 포함됨).
8. **Plan 01 I1 문구 혼동** — "같은 QuestionSet raw/mp4/feedback 동일 uuid"가 §7의 "재녹화 시 새 uuid"와 충돌처럼 읽힘. "특정 raw 1건에서 유도된 파생의 uuid 공유, 재녹화는 독립 객체 집합"으로 명확화.
9. **Plan 04 Rollback 구체성 부족** — DB orphan 정리 SQL 미제공. 실패 시점별 의사결정 매트릭스(4단계) + A~D 구체 액션 블록으로 재구성. Lambda 이전 버전 조회 명령(`list-versions-by-function`) 포함. cutover runbook에 prev/new 버전 기록 체크리스트 추가.
10. **Plan 02 streaming_s3_key 경로 확인 누락** — 변환 완료 후 `update_convert_status(streaming_s3_key=...)`가 `file_metadata.streaming_s3_key`를 업데이트하는 경로가 신규 포맷에도 무영향인 점 명시.

**검증 근거**:
- 코드: `QuestionSetService.java:74`, `convert/handler.py:19,23,44,77-89`, `analysis/handler.py:35,39,155,511-523`, `analysis/api_client.py:78-87`, `FileMetadata.java:33-37`
- 스키마: `V1__init_schema.sql`, `V4__add_question_set_and_file_metadata.sql`(FK + UNIQUE index), `V14__analysis_state_redesign.sql`(CASCADE), `V17__add_user_id_to_interview.sql`
- Lambda 테스트 디렉토리: `ls lambda/analysis/tests lambda/convert/tests` → `No such file or directory`
- EventBridge 필터: Plan 01 §5 JSON `"prefix": "interviews/raw/"` (Scenario 2 불가 원인)

**남은 리스크 (수정 미대상, 모니터링 필요)**:
- UUID 12-hex(48-bit) 충돌 확률 — "일일 수만 개"에서 무시 가능이나 규모 증가 시 16자 확장 필요 (SSOT §7에 이미 명시)
- `QuestionSet.assignFileMetadata` 재호출 시 old `file_metadata` 행 orphan 처리 — 본 플랜 범위 밖, 별도 티켓 권장 (Plan 02에 명시)
- Lifecycle 정책 실제 적용은 `prod-environment-setup/plan-03`에서 집행되며 dev에도 동일 적용할지 별도 결정 필요

## 다음 단계

1. 팀 리뷰 (architect-reviewer / code-reviewer)
2. Plan 01 착수 → SSOT 문서 머지
3. Plan 02·3 병렬 실행 (2~3일)
4. dev 컷오버 윈도우 잡고 Plan 04 집행
5. 24h 안정화 후 prod-environment-setup 착수
