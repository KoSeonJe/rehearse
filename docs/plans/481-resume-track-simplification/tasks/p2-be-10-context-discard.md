# Task 10 — DialogueCompactor / Context Layer 폐기

> **위치**: `tasks/p2-be-10-context-discard.md`
> **답하는 질문**: 다층 컨텍스트 + 비동기 요약 어떻게 제거?

---

## 목적

`DialogueCompactor` (비동기 대화 요약 LLM 호출) + `DialogueHistoryLayer` + `SessionStateLayer` + Resume 전용 Focus 분기 폐기. 꼬리질문 컨텍스트 = 직전 main + 사용자 답변 + ResumeSkeleton 만 (단일 prompt).

## 에이전트

- **구현**: `backend` — context layer 파일 삭제 + InterviewContextBuilder 조합 갱신 + FocusHints / FocusLayer 분기 제거
- **리뷰**: `code-reviewer-backend` — context 조합 회귀 / SkeletonCallType enum 정리

## 변경 파일

**Context / 컴팩션 (삭제)**:
- `backend/src/main/java/com/rehearse/api/infra/ai/context/compaction/DialogueCompactor.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/DialogueHistoryLayer.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SessionStateLayer.java`
- `backend/src/main/java/com/rehearse/api/infra/ai/context/SessionStateSnapshot.java` (record)

**callType / enum 멤버 (제거)**:
- `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallType.java` (또는 동등) — `compaction_summarizer` 멤버 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/SkeletonCallType.java` — `RESUME_PLAYGROUND_OPENER` / `RESUME_PLAYGROUND_RESPONDER` 멤버 제거

**Focus 분기 (수정 유지)**:
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusLayer.java` — `CAP_RESUME_PLAYGROUND_OPENER` / `CAP_RESUME_PLAYGROUND_RESPONDER` 상수 제거 + `CURRENT_LEVEL` prompt 라인 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/context/layer/FocusHints.java` — `currentLevel:int` / `answerQuality:int` 필드 제거 + `ResumePlaygroundOpenerHints` / `ResumePlaygroundResponderHints` inner record 제거

**조합자 갱신**:
- `backend/src/main/java/com/rehearse/api/infra/ai/context/InterviewContextBuilder.java` — Layer 조합에서 `DialogueHistoryLayer` / `SessionStateLayer` 제거. Resume 트랙 = `FocusLayer` + `PreambleLayer` + Skeleton 직접 동봉만

**설정 / 메트릭 (정리)**:
- `backend/src/main/resources/application*.yml` — `compaction_summarizer` callType 모델 매핑 + temp 0.3 + maxTokens 800 제거
- `backend/src/main/java/com/rehearse/api/infra/ai/metrics/AiCallMetrics.java` — `compaction_summarizer` 카운터 제거 (대시보드 메모는 Task 15)

## 핵심 로직

```
InterviewContextBuilder (After):
  build(question, userAnswer, ...):
    layers = [PreambleLayer, FocusLayer]
    if (questionType.isResume()):
      layers += SkeletonInjector (직접 주입, FocusHints 경유 X)
    return layers.compose(...)

FocusLayer (After):
  - CURRENT_LEVEL prompt 라인 제거
  - CAP_RESUME_PLAYGROUND_* 상수 제거
  - 잔존 = TECH / BEHAVIORAL focus hint 만
```

## 의존
- 선행 Task: 06 (FollowUpService Resume 분기 제거), 08 (FollowUpQuestionWriter prompt 갱신)
- 외부: 없음

## 테스트 케이스
- [ ] `InterviewContextBuilderTest` — Resume 트랙 context = Preamble + Focus + Skeleton 만, History/SessionState 부재
- [ ] 표준 트랙 (CS / BEHAVIORAL) context 회귀 통과
- [ ] grep `DialogueCompactor` / `DialogueHistoryLayer` / `SessionStateLayer` / `SessionStateSnapshot` 잔존 0
- [ ] grep `compaction_summarizer` / `CAP_RESUME_PLAYGROUND_OPENER` / `RESUME_PLAYGROUND_RESPONDER` (Skeleton/Focus 멤버) / `currentLevel`(FocusHints) / `answerQuality`(FocusHints) / `CURRENT_LEVEL`(prompt) 잔존 0

## 완료 기준
- [ ] context layer 파일 4개 삭제 + Focus 분기 정리 + 설정 정리
- [ ] InterviewContextBuilder Layer 조합 갱신
- [ ] 컴파일 + 회귀 테스트 통과
- [ ] code-reviewer-backend 실행

## 커밋 메시지

```
refactor(BE): DialogueCompactor + Resume 컨텍스트 다층 layer 폐기
```

## 비고

NF Observability — `compaction_summarizer` 카운터 dashboard panel 제거 메모는 Task 15 (`docs/observability/dashboards.md`).
