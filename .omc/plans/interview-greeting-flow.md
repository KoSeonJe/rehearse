# 면접 시작 인사 및 자기소개 흐름

**Status**: Completed
**Date**: 2026-03-11

---

## Why

현재 면접 시작 시 "안녕하세요, 모의 면접을 시작하겠습니다. 첫 번째 질문입니다."라는 단순 인사 후 바로 기술 질문으로 진입한다. 실제 면접에서는 면접관 자기소개 → 지원자 자기소개 요청 → 본격적인 질문 순서가 자연스럽다. 이 흐름을 추가하면 면접 경험이 더 현실적이고 지원자가 긴장을 풀 준비 시간을 가질 수 있다.

## Goal

- 면접 시작 시 면접관이 인사 + 자기소개 → 지원자에게 간단한 자기소개 요청 → 자기소개 완료 후 기술 질문 진입
- 자기소개는 평가 대상이 아님 (후속질문 생성 안 함)
- 기존 기술 질문 흐름에 영향 없음

## 현재 흐름

```
Phase: ready → TTS "안녕하세요...첫 번째 질문입니다. {기술질문}" → VAD 대기 → 답변 → 다음 질문
```

## 변경 후 흐름

```
Phase: ready → TTS 인사 + 자기소개 요청
  → VAD 대기 → 지원자 자기소개 답변 (녹음만, 후속질문 없음)
  → 침묵 감지 → TTS "감사합니다. 그럼 본격적으로 면접을 시작하겠습니다. 첫 번째 질문입니다. {기술질문}"
  → VAD 대기 → 기술 답변 → 후속질문 → ... (기존 흐름)
```

---

## 접근 방식: 프론트엔드 `greeting` phase 추가

백엔드 질문 생성 프롬프트는 변경하지 않는다. 자기소개는 기술 면접 질문이 아니므로 프론트엔드에서 별도 phase로 관리한다.

### Trade-offs

| 방식 | 장점 | 단점 |
|------|------|------|
| **FE greeting phase (선택)** | BE 변경 없음, 자기소개가 질문 리스트에 섞이지 않음, 평가 제외 명확 | FE 로직 추가 |
| BE 프롬프트에 자기소개 질문 포함 | 단순 | 자기소개가 "질문"으로 취급됨, 피드백/리포트에 포함됨 |

---

## 수정 사항

### Task 1: InterviewPhase에 `greeting` 추가 [FE]
- **파일**: `frontend/src/stores/interview-store.ts`
- `InterviewPhase` 타입에 `'greeting'` 추가
- `setInterview()`에서 phase를 `'ready'` 대신 `'greeting'`으로 설정
- Implement: `frontend`

### Task 2: greeting phase 로직 구현 [FE]
- **파일**: `frontend/src/hooks/use-interview-session.ts`
- greeting phase 진입 시:
  1. TTS로 인사 + 자기소개 요청 멘트 재생
  2. VAD 활성화 조건에 `'greeting'` 추가
  3. 자기소개 답변 시 녹음하되 후속질문 요청 안 함
  4. 침묵 감지 시 phase를 `'ready'`로 전환 + 전환 멘트 TTS + 첫 질문 TTS
- Implement: `frontend`

### Task 3: greeting phase UI 표시 [FE]
- **파일**: `frontend/src/pages/interview-page.tsx`
- greeting phase일 때 질문 카드 영역에 "자기소개" 안내 표시
- 기존 질문 인덱스 표시(1/N)는 greeting 중 숨김
- Implement: `frontend`

---

## TTS 멘트

### 인사 + 자기소개 요청
```
안녕하세요, 오늘 면접을 진행하게 된 AI 면접관입니다.
면접을 시작하기 전에 간단하게 자기소개 부탁드리겠습니다.
```

### 자기소개 후 → 첫 질문 전환
```
네, 감사합니다. 그럼 본격적으로 면접을 시작하겠습니다. 첫 번째 질문입니다.
{첫 번째 기술 질문 content}
```

---

## 수정 대상 파일

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/src/stores/interview-store.ts` | `InterviewPhase`에 `'greeting'` 추가, `setInterview` phase 변경 |
| `frontend/src/hooks/use-interview-session.ts` | greeting TTS, VAD 조건, 자기소개 답변 처리, phase 전환 |
| `frontend/src/pages/interview-page.tsx` | greeting phase UI |

---

## 검증
1. `npx tsc --noEmit` 통과
2. `npx vite build` 통과
3. 수동 테스트:
   - 면접 시작 → 인사 TTS 재생 → 자기소개 요청 확인
   - 자기소개 답변 → 침묵 감지 → "감사합니다" 전환 멘트 확인
   - 첫 기술 질문 TTS 재생 → 이후 기존 흐름 정상 동작
   - 자기소개에 대한 후속질문이 생성되지 않는지 확인
