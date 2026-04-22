# Plan 12: Feature Flag Cleanup `[post-rollout]`

> 상태: Draft
> 작성일: 2026-04-21 (2026-04-22 plan-13 플래그 추가)
> 주차: W8+ (스프린트 후, 각 flag 의 Exit Criteria 충족 후 트리거)
> 의존성: 01, 03, 04, 07, **13** 전부 전면 롤아웃 + 2주 안정화

## Why

`AiFeatureProperties` 의 7개 flag(`intent-classifier / answer-analyzer / followup-v3 / resume-track / context-engineering / feedback-rubric / feedback-synthesizer`)는 모두 **release flag** — 점진 배포 안전망으로 도입됐고 영구적 on/off 스위치가 아니다. 스프린트 종료 후 제거하지 않으면:

- 구버전(v2) 코드 경로가 "살아있지만 안 쓰이는" 상태로 영원히 남음
- 분기문이 2배 → 유지보수 비용/리뷰 부담 증가
- 6개월 뒤 "이 flag 켜도 되나?" 를 아무도 모름 (Flag Debt)
- `@RefreshScope` + `spring-cloud-context` 의존성도 flag 전부 사라지면 필요 없음

**청소 계획을 사전에 문서화**하여 스프린트 종료 직후 바로 실행 가능하게 한다.

## 각 Flag 의 Exit Criteria

| Flag | 플랜 | 청소 트리거 조건 | 청소 대상 |
|------|------|------------------|-----------|
| `intent-classifier` | plan-01 | 프로덕션 100% 2주 / 분류 정확도 ≥ 90% / OFF_TOPIC 오분류율 ≤ 3% | flag + "없이 돌아가는 폴백 경로" |
| `answer-analyzer` + `followup-v3` | plan-02, 03 | 프로덕션 100% 2주 / v3 꼬리질문 human eval 점수 ≥ v2 / 레이턴시 p95 ≤ v2+200ms | **v2 꼬리질문 코드 통째 삭제** + 두 flag 동시 제거 |
| `resume-track` | plan-05~07 | 이력서 업로드 유저 100% 2주 / 에러율 ≤ 일반 트랙 + 0.5%p | flag 제거 (feature 자체는 유지) |
| `context-engineering` | plan-04 | 프로덕션 100% 2주 / 캐시 히트율 ≥ 70% / 레이턴시 회귀 없음 | flag 제거, 4-layer 빌더 단일 경로 |
| `feedback-rubric` + `feedback-synthesizer` | plan-08, plan-09, plan-13 | plan-13 cut-over 후 2주 안정 / Rubric 실패율 <1% / content 품질 SLO 충족 / 사용자 피드백 페이지 500 에러 <0.1% | 두 flag 동시 제거 (기본 상시 on), plan-13 Exit Criteria 에 따라 V29 rollback migration 은 archival 보관 (실행 금지) |

**측정 지표 출처**: `AiCallMetrics` (`rehearse.ai.call.duration_seconds{call.type, fallback, cache.hit, outcome}`) + plan-10 Eval Harness.

## 청소 PR 구조 (각 flag 독립 PR)

각 flag 제거는 **별도 PR 1개** 로 진행. 병합 후 문제 발견 시 단일 revert 로 즉시 복구.

### PR 1: `chore(ai): remove intent-classifier feature flag`
- `AiFeatureProperties.intentClassifier` 필드 + getter/setter 제거
- `application.yml` 의 `rehearse.features.intent-classifier` 블록 제거
- flag 분기 호출측(`FollowUpService` 등)에서 `if (features.getIntentClassifier().isEnabled())` 분기 삭제
- v1 경로 코드(classifier 없이 폴백) 삭제
- 관련 테스트 정리

### PR 2: `chore(ai): remove followup v2 + answer-analyzer/followup-v3 feature flags`
- **가장 범위가 큼** — v2 꼬리질문 구현(기존 `FollowUpService.generateFollowUp` 내 single-shot 경로) 통째 삭제
- `answerAnalyzer`, `followupV3` 두 필드 동시 제거
- v2 관련 프롬프트 파일(`backend/src/main/resources/prompts/follow-up-*.txt` 중 구버전) 삭제
- v2 전용 테스트 삭제

### PR 3: `chore(ai): remove resume-track feature flag`
- `resumeTrack` 필드 제거
- `resume_track_enabled = false` 일 때 타던 "일반 트랙 폴백" 분기 삭제 (이력서 업로드 시 항상 resume 트랙)
- resume 트랙이 default 가 되도록 서비스 진입점 정리

### PR 4: `chore(ai): remove context-engineering feature flag`
- `contextEngineering` 필드 제거
- 4-layer 빌더를 단일 default 경로로 승격
- 구버전 플랫 프롬프트 생성 로직 삭제

### PR 5: `chore(ai): remove feedback-rubric + feedback-synthesizer feature flags`
- plan-13 cut-over 후 2주 안정화 + content 품질 SLO 충족 시 실행
- `feedbackRubric`, `feedbackSynthesizer` 두 필드 동시 제거
- `rehearse.features.feedback-rubric` / `feedback-synthesizer` 블록 제거
- flag off 분기(없음 — plan-13 cut-over 시점에 Lambda content 블록이 이미 제거돼 fallback 경로 부재. flag off 는 "Content 탭 공백" 상태로만 정의됨)
- V29 `drop_lambda_content_columns.sql` rollback SQL 은 archival 보관 (실행 금지) — 과거 데이터 복구 불가로 rollback 실익 없음

### PR 6 (optional): `chore(ai): remove @RefreshScope + spring-cloud-context`
- PR 1~5 완료 시 `AiFeatureProperties` 가 비어있거나 영구 flag 만 남음
- 남는 flag 이 없으면 `spring-cloud-context:4.1.4` 의존성 제거 + `/actuator/refresh` 엔드포인트 노출 제거
- `AiFeatureProperties` 자체 삭제 가능하면 삭제

## 생성/수정 파일 (예상)

### 제거 대상
| 파일 | 작업 |
|------|------|
| `backend/src/main/java/com/rehearse/api/config/AiFeatureProperties.java` | flag 필드 순차 제거 → 최종 삭제 |
| `backend/src/main/resources/application.yml` | `rehearse.features.*` 블록 순차 제거 |
| `backend/build.gradle.kts` | `spring-cloud-context` 의존성 제거 (PR 5) |
| `backend/src/test/java/com/rehearse/api/config/ActuatorRefreshIntegrationTest.java` | PR 5 에서 삭제 |

### 수정 대상
- 각 domain service 의 flag 분기문 제거 (`FollowUpService`, `QuestionGenerationService`, Resume 관련 서비스 등)
- v2 구현 코드 + 관련 테스트 파일 삭제

## 담당 에이전트

- Implement: `backend` — 각 PR 단독 실행, 스코프 협소하므로 Opus 불필요
- Review: `code-reviewer` — dead code 완전 제거 여부, v2 경로가 어디선가 참조되지 않는지
- Review: `test-engineer` — v2 전용 테스트만 정확히 삭제되고 핵심 계약 테스트는 보존되는지

## 검증

1. 각 PR 머지 전: `./gradlew test` 전체 통과
2. 각 PR 머지 후: 프로덕션 `AiCallMetrics` 대시보드에서 해당 call.type 의 레이턴시/에러율 회귀 없음 (1주 관찰)
3. PR 5 머지 후: `curl /actuator/refresh` 가 404 반환 (엔드포인트 제거 확인)
4. 모든 PR 완료 후: `grep -rn "FeatureProperties\|RefreshScope" backend/src` → 0 결과

## 트리거 조건 재확인 (취소/연기 시나리오)

- **v3 꼬리질문 품질이 v2 에 못 미치면**: PR 2 취소, v2 를 default 로 roll-forward. `followupV3 = false` 영구 유지하거나 flag 제거 + v3 코드 삭제 (역방향).
- **캐시 히트율 목표 미달**: PR 4 연기, `context-engineering` flag 유지 상태로 plan-04 빌더 튜닝.
- **resume-track 에러율 문제**: PR 3 연기, rollout-percentage 조정 가능하도록 `FeatureFlag` 에 `rolloutPercentage` 필드 추가 고려.

## Out of Scope

- 새 flag 추가 (이 plan 은 청소 전용)
- `AiFeatureProperties` 영구 flag(향후 추가될 유료/무료 티어 구분 등)의 설계
