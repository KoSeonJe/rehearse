# Sprint 0: 녹화-분석-피드백 파이프라인 — 전체 요구사항

## Why

MVP 기능은 완성되었으나, 클라이언트 실시간 분석(MediaPipe, Web Audio) 기반 파이프라인이 구조적 한계에 도달함.
영상 싱크 불일치, 비언어 피드백 미생성, STT 정확도 부족 등 10개 이슈가 개별 수정 불가능한 아키텍처 문제로 판명됨.

**새 아키텍처**: 질문세트 단위 녹화 → S3 업로드 → Lambda 서버 분석(Whisper + GPT-4o Vision + LLM) → 피드백 뷰어
- 설계 문서: `docs/architecture/recording-analysis-pipeline.md`

---

## 대체된 이슈 (Closed)

| # | 제목 | 대체 사유 |
|---|------|-----------|
| #64 | 영상 파이프라인 시각화 문서 | pipeline.md가 이 문서 자체 |
| #60 | 영상 녹화/저장/재생 파이프라인 전면 재구축 | sprint-0 FE 녹화 태스크로 대체 |
| #57 | 답변 시에만 녹화 → 전체 면접 연속 녹화 전환 | 질문세트 단위 녹화로 대체 |
| #56 | 타임스탬프 피드백이 실제 영상과 불일치 | 새 아키텍처에서 구조적 해결 |
| #55 | 영상과 음성 싱크 불일치 | 새 아키텍처에서 구조적 해결 |
| #54 | 자기소개와 질문1 답변이 함께 저장 | 질문세트 구조에서 자동 해결 |
| #53 | 피드백 페이지 영상 초기 위치 ≠ 0초 | 질문세트별 영상이므로 항상 0초 시작 |
| #47 | 음성인식 정확도 개선 (AI STT 전환) | Lambda Whisper API로 대체 |
| #59 | 비언어적 피드백이 생성되지 않음 | Lambda GPT-4o Vision으로 대체 |
| #58 | 질문별 모범 답변 및 학습 자료 제공 | sprint-0 면접 생성 리팩토링에서 구현 |

---

## 이슈 매트릭스

| # | 이슈 | 타입 | 스코프 | 태스크 | 우선순위 |
|---|------|------|--------|--------|----------|
| #77 | Legacy 파이프라인 정리 | chore | FE/BE | Task 0 | P0 (blocking) |
| #78 | DB 스키마: question_set 기반 | enhancement | BE | Task 1 | P0 |
| #79 | S3 + EventBridge + Lambda + MediaConvert | enhancement | Infra | Task 2 | P0 |
| #80 | 면접 생성 API 리팩토링 | enhancement | BE | Task 3 | P0 |
| #81 | 외부 API 5개 | enhancement | BE | Task 3 | P0 |
| #82 | 내부 API 5개 + Internal-Api-Key | enhancement | BE | Task 4 | P0 |
| #83 | 좀비 감지 스케줄러 + 면접 완료 집계 | enhancement | BE | Task 5 | P1 |
| #84 | 분석 Lambda (FFmpeg + Whisper + Vision + LLM) | enhancement | Lambda | Task 6 | P0 |
| #85 | 변환 Lambda (WebM → MP4) | enhancement | Lambda | Task 7 | P1 |
| #86 | FE 질문세트 단위 녹화 + S3 업로드 | enhancement | FE | Task 8 | P0 |
| #87 | FE 분석 대기 + 상태 추적 UI | enhancement | FE | Task 9 | P1 |
| #88 | FE 피드백 뷰어 | enhancement | FE | Task 10 | P1 |

---

## 의존성 그래프

```
Phase 0 (최초, blocking):
  Task 0: Legacy 파이프라인 정리     ← 모든 후속 작업의 전제

Phase A (Task 0 완료 후, 병렬):
  Task 1: DB 스키마
  Task 2: S3/Lambda 인프라

Phase B (Task 1 완료 후, 병렬):
  Task 3: BE 외부 API (#80, #81)
  Task 4: BE 내부 API (#82)
  Task 5: BE 좀비 스케줄러 (#83)

Phase C (Task 2,3,4 완료 후, 병렬):
  Task 6: 분석 Lambda (#84)
  Task 7: 변환 Lambda (#85)

Phase D (Task 3 완료 후):
  Task 8: FE 녹화 + 업로드 (#86)

Phase E (Task 6,7,8 완료 후):
  Task 9: FE 분석 대기 UI (#87)

Phase F (Task 9 완료 후):
  Task 10: FE 피드백 뷰어 (#88)
```

---

## 성공 기준

1. 모든 태스크 PR이 develop 브랜치에 머지됨
2. FE: `npm run build` + `npm run lint` 통과
3. BE: `./gradlew build` + 전체 테스트 통과
4. Lambda: 단위 테스트 + 통합 테스트 통과
5. E2E: 면접 생성 → 녹화 → S3 업로드 → Lambda 분석 → 피드백 뷰어 전체 플로우 동작
6. progress.md에 최종 상태 반영
