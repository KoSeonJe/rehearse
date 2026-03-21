# 프롬프트 재설계 — 진행 상태

> **최종 업데이트**: 2026-03-21
> **전체 상태**: 구현 완료 + Lambda 배포 완료

---

## Phase 1: [BE] 기반 구조 `DONE`

- [x] Task 1-1: TechStack enum 생성
- [x] Task 1-2: DB 마이그레이션 V9 (tech_stack 컬럼)
- [x] Task 1-3: Interview 엔티티 techStack 필드 + getEffectiveTechStack()
- [x] Task 1-4: YAML 프로필 구조 생성 (base 5개 + overlay 5개 + template 2개)
- [x] Task 1-5: PersonaResolver + BaseProfile/StackOverlay/ResolvedProfile

**PR**: #175 → develop (머지 완료)

---

## Phase 2: [BE] 프롬프트 빌더 리팩토링 `DONE`

- [x] Task 2-1: LevelGuideProvider 생성
- [x] Task 2-2: 프롬프트 템플릿 파일 (Phase 1에서 생성 완료)
- [x] Task 2-3: AiClient Parameter Object + 프롬프트 빌더 2개
- [x] Task 2-4: QuestionGenerationRequestedEvent techStack 전파
- [x] Task 2-5: ClaudeApiClient + MockAiClient + InterviewService 호출 수정
- [x] Task 2-6: InterviewResponse에 techStack 추가
- [x] Task 2-7: CreateInterviewRequest techStack + position-techStack 검증

**PR**: #176 → develop (머지 완료)

---

## Phase 3: [BE] Internal API 확장 `DONE`

- [x] Task 3-1: AnswersResponse에 position/techStack/level 추가 (InterviewFinder 사용)

**PR**: #177 → develop (머지 완료), #178 클린코드 (머지 완료)

---

## Phase 4: [Lambda] 프롬프트 최적화 `DONE`

- [x] Task 4-1: VerbalPromptFactory 생성 (5종 Position×TechStack 키워드 사전)
- [x] Task 4-2: verbal_analyzer.py 수정 (factory 분기 + position=None 폴백)
- [x] Task 4-3: handler.py 시그니처 체인 변경 (3함수)
- [x] Task 4-4: 비언어 분석 프롬프트 압축 (34줄→13줄, ~44% 토큰 절감)

**PR**: #179 → develop (머지 완료)
**배포**: `rehearse-analysis-dev` Lambda 업데이트 완료 (2026-03-21)

---

## Phase 5: [FE] 기술스택 선택 UI `DONE`

- [x] Task 5-1: TechStack 타입 + 상수 정의 (18종)
- [x] Task 5-2: StepTechStack 컴포넌트 생성
- [x] Task 5-3: 위저드 5단계 확장

**PR**: #180 → develop (머지 완료)

---

## Phase 6: 통합 검증 `DONE`

- [x] Task 6-1: BE 통합 테스트 (30개 신규, 전체 147개 통과)
- [x] Task 6-2: 전체 검증 — BE/Lambda/FE 충돌·버그 검증 완료
- [ ] Task 6-3: A/B 테스트 (API 키 필요, 추후 실행)

**PR**: #181 → develop (머지 완료), #182 QuestionCountCalculator 분리 (머지 완료)

---

## 전체 검증 결과 (2026-03-21)

### BE 검증

| 항목 | 결과 |
|------|------|
| techStack 데이터 흐름 (면접생성→Event→질문생성→AI) | 정상 |
| 후속 질문 position/techStack/level 전달 | 정상 |
| NPE 방어 (nullable 필드) | 정상 |
| Event List→Request Set 변환 | 정상 |
| InterviewResponse techStack 매핑 | 정상 |
| YAML 로딩 + 키 일치 | 정상 |
| 템플릿 플레이스홀더 치환 완전성 | 정상 |
| DB 마이그레이션 V8↔V9 충돌 | 없음 |
| @Deprecated ClaudePromptBuilder 참조 | 해결 (#182) |

### Lambda 검증

| 항목 | 결과 |
|------|------|
| handler→verbal_analyzer 데이터 흐름 | 정상 |
| tech_stack=None 폴백 | 정상 |
| json_utils.py import | 정상 |
| verbal/vision 응답 스키마 호환 | 정상 |

### FE 검증

| 항목 | 결과 |
|------|------|
| BE/FE TechStack 18개 값 일치 | 정상 |
| 위저드 5단계 canNext/handleSubmit | 정상 |
| position 변경 시 techStack 리셋 | 정상 |
| API 요청 techStack 포함/제외 | 정상 |
| POSITION_TECH_STACKS ↔ BE allowedPosition 일치 | 정상 |

### 알려진 제한

| 항목 | 설명 | 판단 |
|------|------|------|
| retryQuestionGeneration resumeText=null | 이력서 DB 미저장으로 재시도 시 복구 불가 | 기존 설계, 이번 범위 아님 |
| InterviewResponse.techStack raw null | 사용자 미지정 구분용 의도적 설계 | 정상 |
| A/B 테스트 미실행 | API 키 필요 | 추후 별도 실행 |

---

## 완료 기록

| 날짜 | Phase | PR | 비고 |
|------|-------|-----|------|
| 2026-03-21 | Phase 1 | #175 | BE 기반 구조 |
| 2026-03-21 | Phase 2 | #176 | BE 빌더 리팩토링 + 토큰 최적화 |
| 2026-03-21 | Phase 3 | #177, #178 | Internal API + 클린코드 |
| 2026-03-21 | Phase 4 | #179 | Lambda 프롬프트 + AWS 배포 |
| 2026-03-21 | Phase 5 | #180 | FE 기술스택 위저드 |
| 2026-03-21 | Phase 6 | #181, #182 | 통합 테스트 + QuestionCountCalculator 분리 |
