# 면접 경험 품질 향상

- **Status**: Planned
- **Created**: 2026-03-10
- **Branch**: feat/interview-quality

## Why

MVP 면접 기능은 동작하지만, 질문/피드백 품질이 실제 면접 수준에 미치지 못함.
- 이력서 입력 불가 → 지원자 맞춤 질문 생성 불가
- 질문 프롬프트가 1줄짜리 가이드 → 깊이 없는 표면적 질문
- 꼬리질문에 대화 히스토리 없음 → 단발성 질문 반복
- NonVerbal 데이터가 문자열로 변환되어 AI에 전달 → 타임스탬프 피드백 부정확

## Goal

4개 Phase를 통해 면접 질문 깊이, 꼬리질문 맥락, 피드백 정확도를 실제 면접관 수준으로 향상.

## Evidence

- 현재 CS 질문 가이드: "운영체제, 네트워크, 데이터베이스, 자료구조 중심" 1줄
- 꼬리질문: 직전 1개 Q&A만 전달, AI가 맥락 없이 질문
- 피드백: NonVerbalEvent의 timestamp/severity/duration이 summary 문자열로 소실

## Trade-offs

- 프롬프트 길이 증가 → 토큰 비용 상승 (히스토리 최대 10개로 제한하여 완화)
- 이력서 입력 추가 → 사용자 진입 장벽 증가 (optional로 설계하여 완화)
- PDF 파싱은 복잡도 대비 효과가 낮아 MVP에서 제외, 텍스트 입력만 지원

---

## Phase 1: 이력서 입력 (FE + BE)

### Why

이력서 없이는 "왜 그 기술을 선택했나?", "프로젝트에서 구체적으로 뭘 했나?" 같은 핵심 질문이 불가능. position/level만으로는 누구에게나 동일한 질문만 생성됨.

### Goal

면접 설정 시 이력서 텍스트를 입력하면, AI가 이력서 기반 맞춤 질문을 생성하도록 함.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/src/pages/interview-setup-page.tsx` | 이력서 텍스트 입력 필드(textarea) 추가 |
| `frontend/src/api/interview.ts` | `CreateInterviewRequest`에 `resumeText` 필드 추가 |
| `backend/.../dto/CreateInterviewRequest.java` | `resumeText` 필드 추가 (nullable) |
| `backend/.../domain/Interview.java` | `resumeText` 컬럼 추가 (`@Column(columnDefinition = "TEXT")`) |
| `backend/.../service/InterviewService.java` | AI 호출 시 이력서 텍스트 전달 |
| `backend/.../infra/ai/ClaudePromptBuilder.java` | 질문 생성 프롬프트에 이력서 컨텍스트 포함 |

### 구현 상세

- **UI**: textarea (최대 5000자), 선택사항 표시, placeholder로 입력 가이드 제공
- **이력서 optional**: 없으면 기존 방식(position/level만)으로 질문 생성
- **프롬프트 추가 문구**:
  ```
  다음은 지원자의 이력서입니다. 이력서에 언급된 기술 스택, 프로젝트 경험, 역할을 기반으로
  구체적이고 깊이 있는 질문을 생성하세요. 단순 확인 질문이 아닌,
  "왜 그 선택을 했는지", "어떤 트레이드오프가 있었는지"를 묻는 질문을 우선하세요.
  ```
- **PDF 파싱은 이번 범위에서 제외** — 텍스트 붙여넣기만 지원

---

## Phase 2: 질문 프롬프트 강화 (BE)

### Why

현재 CS 가이드가 "운영체제, 네트워크, 데이터베이스, 자료구조 중심" 1줄. 실제 면접관은 한 주제를 깊게 파고들며, 레벨별로 기대 수준이 다름. 현재 프롬프트로는 표면적 질문만 생성됨.

### Goal

시스템 프롬프트를 개선하여 주제별 깊이 있는 질문 체인과 레벨별 난이도 차별화를 달성. 질문 수(5개)는 유지하고 품질만 향상.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `backend/.../infra/ai/ClaudePromptBuilder.java` | 질문 생성 시스템 프롬프트 전면 개선 |

### 구현 상세

- **CS 면접 주제별 깊이 가이드**:
  ```
  각 질문은 하나의 주제를 깊이 파고드는 체인으로 구성하세요:
  - 자료구조: "HashMap 내부 구조 → 해시 충돌 처리 방법 → Java 8 Red-Black Tree 변환 조건"
  - 네트워크: "TCP 3-way handshake → TIME_WAIT 존재 이유 → 대량 연결 시 문제와 해결"
  - 운영체제: "프로세스 vs 스레드 → 컨텍스트 스위칭 비용 → 경량 스레드(코루틴) 비교"
  - 데이터베이스: "인덱스 동작 원리 → B+Tree 선택 이유 → 커버링 인덱스 활용"
  ```

- **이력서 기반 면접 질문 전략**:
  ```
  이력서에 언급된 각 프로젝트/기술에 대해:
  1. "왜 그 기술을 선택했나? 대안은 무엇이 있었나?"
  2. "해당 기술의 트레이드오프는? 실제로 겪은 문제는?"
  3. "다시 설계한다면 어떻게 바꾸겠는가?"
  4. "팀에서 본인의 구체적 역할과 기여는?"
  ```

- **레벨별 난이도 차별화**:
  ```
  JUNIOR: 개념 이해 확인, 기본 동작 원리 설명 요청
  - "HashMap과 TreeMap의 차이점과 각각 언제 사용하는지 설명해주세요"

  MID: 응용력 + 트레이드오프 분석
  - "대규모 트래픽 환경에서 세션 관리를 어떻게 설계하겠습니까? 각 방식의 장단점은?"

  SENIOR: 설계 판단 + 기술 리더십 + 조직 관점
  - "레거시 모놀리스를 MSA로 전환할 때의 전략과, 팀 구조는 어떻게 가져가겠습니까?"
  ```

---

## Phase 3: 꼬리질문 강화 (BE)

### Why

현재 직전 1개 Q&A만 전달하여 AI가 맥락 없이 단발성 질문을 반복. 실제 면접은 대화 흐름이 있고, 이전 답변을 기반으로 더 깊이 파고들거나 방향을 전환함.

### Goal

전체 Q&A 히스토리를 전달하고, 꼬리질문 전략을 가이드하여 실제 면접 대화 흐름을 재현.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `backend/.../infra/ai/ClaudePromptBuilder.java` | followUp 프롬프트에 히스토리 포함 및 전략 가이드 |
| `backend/.../service/InterviewService.java` | 현재 세션의 이전 Q&A 히스토리 수집하여 전달 |
| `backend/.../infra/ai/AiClient.java` | `generateFollowUpQuestion` 시그니처 확장 (히스토리 파라미터) |
| `backend/.../infra/ai/ClaudeApiClient.java` | 히스토리 포함 구현 |
| `backend/.../infra/ai/MockAiClient.java` | mock 구현 수정 (히스토리 파라미터 대응) |

### 구현 상세

- **히스토리 전달**: 현재 세션의 모든 Q&A를 시간순으로 전달 (최대 10개, 토큰 제한)
  ```
  [이전 대화 기록]
  Q1: {질문}
  A1: {답변}
  Q2 (꼬리질문): {질문}
  A2: {답변}
  ...
  ```

- **꼬리질문 타입별 전략 가이드**:
  ```
  답변 평가 후 다음 4가지 전략 중 가장 적합한 것을 선택하세요:

  DEEP_DIVE: 답변이 정확할 때 → 한 단계 더 깊이 파고들기
  - "그렇다면 그 방식의 시간 복잡도는? 최악의 경우는?"

  CLARIFICATION: 답변이 모호할 때 → 구체적 예시나 상황 요청
  - "실제 프로젝트에서 그걸 적용한 경험이 있다면 설명해주세요"

  CHALLENGE: 답변이 맞지만 다른 관점 제시 → 비판적 사고 확인
  - "그 방식의 단점은 무엇이고, 대안은 없을까요?"

  APPLICATION: 이론적 답변일 때 → 실무 적용 능력 확인
  - "그 개념을 현재 프로젝트에 적용한다면 어떻게 하겠습니까?"
  ```

- **CS 면접 꼬리질문**: "답변이 맞으면 한 단계 더 깊이 → 틀리면 힌트 후 재질문"
- **이력서 면접 꼬리질문**: "왜? → 대안은? → 다시 한다면? → 팀에서의 역할은?" 체인

---

## Phase 4: 피드백 품질 (FE + BE)

### Why

NonVerbal 데이터(timestamp, severity, duration)가 프론트엔드에서 summary 문자열로 변환되어 AI에게 전달됨. 원본 구조화 데이터가 손실되어 AI가 정확한 타임스탬프 피드백을 생성할 수 없고, severity 기반 우선순위 판단도 불가능.

### Goal

NonVerbalEvent를 구조화된 JSON 그대로 전달하고, 피드백 프롬프트를 개선하여 타임스탬프 정확도와 기술적 피드백 품질을 향상.

### 변경 파일

| 파일 | 변경 내용 |
|------|-----------|
| `frontend/src/pages/interview-complete-page.tsx` | NonVerbalEvent 구조화 데이터를 JSON 그대로 전달 |
| `backend/.../infra/ai/ClaudePromptBuilder.java` | 피드백 프롬프트에 구조화된 비언어 데이터 형식 가이드 추가 |

### 구현 상세

- **NonVerbalEvent 구조화 전달**:
  ```json
  {
    "nonVerbalEvents": [
      {
        "type": "EYE_CONTACT_LOST",
        "timestamp": 65,
        "severity": "HIGH",
        "value": 0.3,
        "duration": 8
      },
      {
        "type": "POSTURE_SLOUCH",
        "timestamp": 120,
        "severity": "MEDIUM",
        "value": 0.6,
        "duration": 15
      }
    ]
  }
  ```
  - 기존 summary 문자열 변환 로직을 제거하고, 원본 배열 그대로 JSON 직렬화

- **피드백 프롬프트 개선**:
  ```
  비언어적 피드백 작성 규칙:
  1. 각 피드백 항목에 정확한 타임스탬프(MM:SS)를 포함하세요
  2. severity가 HIGH인 이벤트를 우선 피드백하세요
  3. duration이 긴 이벤트는 "지속적인 패턴"으로 언급하세요
  4. 같은 type의 반복 이벤트는 패턴으로 묶어서 피드백하세요
  ```

- **기술적 정확성 검증 가이드 추가**:
  ```
  CS 관련 답변 피드백 시:
  1. 답변의 기술적 사실 관계를 확인하세요
  2. 부정확한 내용이 있으면 구체적으로 어떤 부분이 틀렸는지, 올바른 내용은 무엇인지 명시하세요
  3. "대체로 맞지만" 같은 모호한 평가 대신, 정확한 부분과 부정확한 부분을 분리하세요
  ```

---

## 구현 순서

1. **Phase 2** (질문 프롬프트 강화) — BE만 변경, 즉시 효과
2. **Phase 3** (꼬리질문 강화) — BE 중심, Phase 2와 시너지
3. **Phase 1** (이력서 입력) — FE+BE, 독립적 기능 추가
4. **Phase 4** (피드백 품질) — FE+BE, 데이터 흐름 변경

Phase 2 → 3은 프롬프트 개선으로 빠르게 효과를 볼 수 있고, Phase 1은 UI 변경이 필요하므로 이후 진행. Phase 4는 데이터 흐름 변경이라 마지막에 진행.

## 범위 외

- PDF 이력서 파싱 (별도 작업으로 분리)
- 면접 히스토리 대시보드 (MVP DON'T)
- 질문 수 변경 (5개 유지)
