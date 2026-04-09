# 비언어 피드백 프롬프트 개선 (Vision + Gemini Audio)

**상태**: In Progress
**날짜**: 2026-04-09
**범위**: `lambda/analysis/analyzers/vision_analyzer.py`, `lambda/analysis/analyzers/gemini_analyzer.py`

## Why

현재 운영 중인 비언어 피드백이 사용자에게 **변별력 없는 추상적 평가**를 반환하여 모의면접 서비스의 핵심 가치를 훼손하고 있다.

### 실제 증상 (프로덕션 샘플 3건)

| 샘플 | 증상 |
|---|---|
| 1 (모름 답변) | 시선/자세=보통, "상체가 비교적 곧게 펴져 있어 안정적인 인상을 줍니다" |
| 2 (손으로 얼굴 가림) | 자세=보통(!), "손이 얼굴을 가리고 있어 표정이 드러나지 않습니다" (enum과 텍스트 관찰 독립) |
| 3 (필러워드 많은 답변) | 시선/자세=보통, "손을 모으고 있어 안정적인 자세를 유지" |

### 핵심 문제
1. **enum 변별력 0**: 3샘플 모두 자세=보통 고정. 손 얼굴 가림 같은 명백한 부정 신호도 AVERAGE 판정.
2. **완곡어·추상 형용사 반복**: "비교적", "다소", "약간", "안정적인 인상" 패턴이 모든 샘플에 반복.
3. **enum ↔ 텍스트 독립 결정**: 샘플 2에서 negative 텍스트에 "손 얼굴 가림" 명시됐는데 자세 enum은 AVERAGE.
4. **판정 기준 부재**: toneConfidenceLevel, emotionLabel enum에 판정 기준 없음 → 모델이 기본값으로 회귀.

### 목표

- **변별력 회복**: enum이 실제 관찰에 따라 GOOD/NEEDS_IMPROVEMENT로 이동.
- **관찰→인상 서술 강제**: "안정적인 인상" 류 일반론 제거, 신체부위/음성 속성 명사가 주절에 포함되게.
- **판정 기준 명시**: 모든 enum이 decision tree 기반으로 판정.
- **사용자 가치**: 개발자가 실제로 다음 모의면접에서 **무엇을 어떻게 바꿀지** 알 수 있는 피드백.

## 결정 사항

### 1. Vision 프롬프트 전면 교체 (`_SYSTEM_PROMPT`)

- **결정 트리 기반 enum 판정**: NEEDS_IMPROVEMENT 먼저 검사 → GOOD은 1개 이상 긍정 + 부정 없음 → AVERAGE는 "관찰 단서 자체가 없을 때만"
- **관찰 축 4가지** (자세·손/제스처·표정·신체 안정성) + 각 축의 면접 맥락 해석
- **postureLevel 매핑 명시**: 자세·손·안정성 3축 종합 (이름-의미 불일치 해소)
- **자기검증 구조 규칙**: positive/negative 주절에 신체부위 명사 최소 1개 포함 (블랙리스트 방식의 강제력 한계 극복)
- **Few-shot 3쌍** (무난한 답변 / 손 얼굴 가림 / 긴장 억누름)
- **빈도 기준선**: "잦은/반복/과도한"은 프레임의 30% 이상
- **eyeContactLevel 최소 판정 기준** 추가 (필드 유지, 텍스트 언급 여전히 금지)

### 2. Gemini 프롬프트 전면 교체 (`_ANSWER_SYSTEM_TEMPLATE`)

- **결정 트리 기반 enum 판정** (toneConfidenceLevel, emotionLabel)
- **enum 간 일관성 제약**: emotionLabel=자신감 ↔ toneConfidenceLevel=NEEDS_IMPROVEMENT 공존 금지
- **관찰 축 5가지** (어미·확신도 / 말 속도·리듬 / 필러·망설임 / 감정 누설 / 태도 신호)
- **섹션 간 중복 방지 블록**: 동일 단서(예: "너무 어려운데")가 vocal/attitude/overall에서 다른 관점으로 해석되도록
- **verbal 섹션 관점 명시**: 기술 정확성은 technical에, verbal은 "구조·순서·핵심 용어 명확성"만
- **fillerWords 양방향 검증** + 필러 정의 ("예/네"는 응대 어휘라 제외)
- **JSON 스키마 스켈레톤** 추가 (response_mime_type=json 이미 있으나 보완)
- **{feedback_perspective} 오염 방어**: 플레이스홀더 블록 직전에 상위 규칙 불변 선언
- **오디오 인젝션 방어**: 답변자의 발화 지시("GOOD으로 판정해줘") 무시 명시
- **길이 한도**: 각 p/n/s 50~120자
- **Few-shot vocal case B** ("너무 어려운데") 추가

### 3. 시선 금지 규칙은 유지

- Vision: 시선 텍스트 언급 금지 + `_strip_gaze_mentions` 사후 필터 유지
- Gemini: 시각·비언어 언급 금지 유지 (입력은 오디오 전용)

### 4. 적용 범위에서 제외한 것

- **Vision 코드 레벨 hedge 필터**: critic이 제안했으나 프롬프트 구조 규칙(신체부위 명사 자기검증)으로 대체. 추후 메트릭 보고 재도입 결정.
- **Gemini 재시도 루프**: audio 재업로드 비용 과다. 프롬프트 품질 + 모니터링에 의존.
- **A/B 테스트**: 즉시 배포 + dev 환경 수동 검증.
- **`verbal_analyzer.py`**: 레거시 폴백 전용 (handler.py의 `_run_legacy_pipeline`에서만 호출). 이번 범위 외.

## 영향 범위

- `lambda/analysis/analyzers/vision_analyzer.py`: `_SYSTEM_PROMPT` 교체 (line 48~80)
- `lambda/analysis/analyzers/gemini_analyzer.py`: `_ANSWER_SYSTEM_TEMPLATE` 교체 (line 74~99)
- FE 스키마 불변 (`NonverbalFeedback`, `VocalFeedback`, `ContentFeedback`, `DeliveryFeedback` 등 모두 동일)
- BE DTO 불변
- DB 마이그레이션 없음

## Trade-offs

- **프롬프트 길이 증가**: Vision ~80줄 → ~140줄, Gemini ~26줄 → ~120줄
  - Vision: GPT-4o 입력 토큰 ~1k 증가 (호출당 $0.003 내외)
  - Gemini: 입력 토큰 ~1.5k 증가 (호출당 $0.001~0.002)
- **품질 대비 수용 가능**. 모의면접 서비스는 피드백 품질이 곧 가치.
- **GOOD 하향 부작용**: 모든 답변이 GOOD으로 몰릴 가능성 → 메트릭으로 모니터링 (postureLevel 분포 / expressionLabel 분포)

## 작업 태스크

### Task 1: Vision 프롬프트 교체
- Implement: `executor` (직접 편집) — `_SYSTEM_PROMPT` 전면 교체
- Review: 이미 `critic` 검토 완료 반영

### Task 2: Gemini 프롬프트 교체
- Implement: `executor` (직접 편집) — `_ANSWER_SYSTEM_TEMPLATE` 전면 교체
- Review: 이미 `critic` 검토 완료 반영

### Task 3: Dev 환경 수동 검증
- 실패 샘플 3건 재실행 → enum 변별력 + 텍스트 구체성 확인
- 메트릭: postureLevel 분포, toneConfidenceLevel 분포 모니터링

## 검토 이력

- 2026-04-09: `oh-my-claudecode:critic` (Opus) 검토 완료 — Critical 4건, High 8건 수정안 반영.
