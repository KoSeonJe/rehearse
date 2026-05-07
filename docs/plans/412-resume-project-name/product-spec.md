# Product Spec — 이력서 기반 질문에 프로젝트명 포함

> **작성자**: 사용자 (PM)
> **답하는 질문**: 왜? 무엇? 수용기준?
> **다음 단계**: 사용자 승인 → backend agent 가 `tech-spec.md` 작성

---

## 문제 상황 (Problem)

- 현재 상태 (정상 동작 / 기존 흐름):
  - 이력서 (PDF) 업로드 → `ResumeExtractionService` 가 LLM 으로 `ResumeSkeleton` 추출 → DB `resume_skeleton.skeleton_json` 저장.
  - Resume 트랙 인터뷰 시작 시 planner / playground / chain-interrogator / wrap-up 4 단계가 skeleton 을 컨텍스트로 질문 생성.
- 발생 증상 (재현 절차 / 빈도 / 영향 범위):
  - 다수 프로젝트가 담긴 이력서로 인터뷰 진행 → 프로젝트 관련 질문 텍스트가 항상 "이 프로젝트", "해당 프로젝트" 등 지시 표현으로만 출력. 매번 재현. Resume 트랙 전체 영향.
- 사용자·운영 인지 채널:
  - 사용자 발화 — 다수 프로젝트 이력서에서 면접자가 어떤 프로젝트를 가리키는지 식별 불가.
  - 코드 추적 — 추출 record / 프롬프트 / DTO / 매핑 / 다운스트림 prompt builder 전 구간에서 projectName 부재.

## 왜 해야 하는가 (Why)

- **사용자 임팩트**: 면접자가 다수 프로젝트 중 어떤 것을 묻는 질문인지 즉시 인식 불가 → 부정확 / 동문서답 / 면접 몰입 저하. Resume 트랙 핵심 가치 (이력서 컨텍스트 기반 깊이 있는 질문) 훼손.
- **운영 / 시스템 임팩트**: `ProjectPlan.projectName` 이 invariant 필수 필드인데, skeleton 입력에 부재 → planner LLM 이 hallucinate 한 값으로 통과. 사실성 (factuality) 검증 우회 + 인터뷰 데이터 왜곡.
- **외부 압력**: P0 결함 (즉시 수정). MVP 핵심 차별점 (이력서 기반 컨텍스트 인터뷰) 의 가시 결함.

## 해결 방향 (Approach)

- 핵심 접근:
  - 추출 (Skeleton) 단계부터 projectName 을 추출 결과에 포함 → 다운스트림 (planner / 질문 빌더) 이 추출값을 그대로 사용 → 질문 텍스트에 자연스러운 형태로 노출.
  - 이력서에 명시 프로젝트명 없으면 추출기가 claims 요약 기반 한국어 명칭 생성 (예: "주문 API 프로젝트") — 결정성 + 자연스러움 균형.
- 대안 비교:
  - planner 단에서만 보강 — 기각 (현재 hallucinate 통과 중 + 추출 누락이 근본 원인).
  - 명칭 부재 시 "프로젝트 N" 식 placeholder — 기각 (사용자 결정: 자연스러움 우선 → claims 요약 채택).
- 단계 분리: 단일 phase. BE 한정.

## Evidence

- `backend/src/main/java/com/rehearse/api/domain/resume/entity/Project.java:5-9` — record 에 name 필드 부재.
- `backend/src/main/resources/prompts/template/resume/resume-extractor.txt:18-42` — 추출 스키마에 프로젝트명 키 없음.
- `backend/src/main/java/com/rehearse/api/infra/ai/dto/ExtractedResumeSkeleton.java:32-41` — `ExtractedProject` DTO 에 projectName 누락.
- `backend/src/main/java/com/rehearse/api/domain/resume/service/ResumeExtractionService.java:97-101` — `mapProject()` 매핑 미반영.
- `backend/src/main/java/com/rehearse/api/domain/resume/entity/ProjectPlan.java:17-19` — `projectName` invariant 필수 필드 (현재 LLM hallucinate 로 통과).
- `backend/src/main/resources/prompts/template/resume/resume-interview-planner.txt:71` — Few-shot 예시에 placeholder 명칭 ("예시 프로젝트") 존재. skeleton 입력 부재 시 LLM 이 모방·일반화할 가능성 (추정 — 실측 샘플 미수집).
- `grep projectName` 결과: `ProjectPlan` 엔티티 외 다운스트림 (Playground / Chain Interrogator / WrapUp / FocusLayer) 호출처 0건 → 컨텍스트 주입 결손.
- 사용자 발화: "다수 프로젝트 이력서 → 면접자 식별 불가".

## Goal

- [ ] 신규 인터뷰의 skeleton 데이터에서 프로젝트명 적재율 100% (명시 / 부재 케이스 모두 — 부재 시 요약 명칭 채움).
- [ ] 다수 프로젝트 이력서로 진행한 신규 인터뷰의 프로젝트 식별 질문에서 면접자가 어떤 프로젝트를 묻는지 텍스트만으로 인식 가능 (수동 샘플 검수 통과).

## Non-Goals

- 프로젝트 기간 / 역할 / 회사 / 인원수 등 메타데이터 확장 — 사유: projectName 단일 결함 우선 fix. 나머지 필드는 별도 spec.
- 질문 카드 UI 에서 projectName 강조 표시 (bold / chip) — 사유: FE 작업, 본 spec 은 BE 결함 fix.
- 기존 운영 데이터 backfill (LLM 재추출 / 마이그레이션) — 사유: 사용자 결정 = null 허용 + 신규만 적용. 별도 운영 작업 필요 시 Issue 분리.
- 다른 누락 필드 (#419 ttsText / modelAnswer / referenceType / feedbackPerspective) — 사유: 별도 Issue 진행 중.

## 수용 기준 (Acceptance Criteria)

- [ ] `Project` skeleton record 가 projectName 필드를 보유 + 추출 프롬프트 / `ExtractedProject` DTO / `mapProject()` 매핑 일관 반영.
- [ ] 추출기 LLM 이 이력서에 명시 명칭 있으면 그대로, 없으면 claims 기반 한국어 요약 명칭을 생성 (테스트 fixture 로 양 케이스 검증 가능).
- [ ] Plan 생성 시 `ProjectPlan.projectName` 이 skeleton 입력의 projectName 과 일치 (planner 가 임의 생성·번역하지 않음) — 외부 관찰 가능한 일치성 검증.
- [ ] 다수 프로젝트 이력서로 진행한 신규 인터뷰의 playground opener / chain interrogator 질문 텍스트에 projectName 이 포함됨 — Mock fixture 기반 통합 테스트 + 1건 이상 수동 시나리오로 확인.
- [ ] 기존 row (projectName 부재) 로드·조회 시 도메인 / Plan invariant 위배 없이 정상 처리 (neutral 동작 — null 허용 또는 안전 fallback).
- [ ] Mock 이력서 fixture 통합 테스트 (다수 명시 / general 묶음 / 명칭 부재 다수) 3 케이스 모두 추출 → Plan → 질문 단계 통과.

## 비스코프 (Don't)

- 점수 / 루브릭 / 피드백 (질문 생성 외 다운스트림) projectName 컨텍스트 주입 — 사유: 본 결함 범위 = 질문 생성. 점수·피드백 단계 영향은 별도 검토 후 분리 plan.

## 참고

- 관련 Issue: #412
- 관련 plan: `docs/plans/410-resume-context-defects/` (resume 4-layer 결함), `docs/plans/409-question-score-missing/` (인접 적재 누락)
- 외부 자료: 본 spec 은 추출 단계가 "왜 hallucinate 통과하는가" 의 근본 원인 제거에 초점. 다운스트림 prompt context 가시성은 부수 효과.
