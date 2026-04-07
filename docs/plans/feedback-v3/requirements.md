# 피드백 코멘트 구조화 (feedback-v3) — 요구사항 정의

> 상태: Draft
> 작성일: 2026-04-07

## Why

피드백 패널이 이미 정형화된 라벨/카드 구조로 리디자인됐다 (`잘한 점 / 아쉬운 점 / 이렇게 해보세요`, `좋은 인상 / 신경 쓰면 좋을 부분 / 이렇게 바꿔보세요` 등). 그러나 람다 프롬프트는 여전히 LLM에게 `✓ ... \n△ ... \n→ ...` 한 덩어리 문자열을 만들게 한 뒤 FE가 prefix를 파싱하는 레거시 구조를 유지 중이다. 이 약한 계약이 깨지면서 사용자 화면에 두 가지 가시적 결함이 발생함:

1. **`△`/`→` 이모지가 본문에 그대로 노출** — LLM이 줄바꿈 없이 한 줄로 합쳐 응답하면 `frontend/src/components/feedback/structured-comment.tsx:24`의 `split('\n')` 파서가 무력화돼, 첫 prefix(`✓`)만 라벨로 치환되고 나머지(`△`, `→`)가 본문 텍스트에 박힌다.
2. **"표정" 배지가 영어로 노출** (`NEUTRAL`) — `expressionLabel`은 영어 enum인데 `frontend/src/components/feedback/delivery-tab.tsx:87`이 raw 값을 그대로 `LevelBadge`에 넘긴다 (다른 level 필드는 `formatFeedbackLevel`로 한글화).

### Decision Framework

- **Why?** "LLM 자유서술 + FE prefix 파싱"이라는 취약한 계약을 폐기하고, 프롬프트를 FE 라벨 구조에 1:1로 정합시켜야 한다.
- **Goal** LLM이 `{positive, negative, suggestion}` 정형 JSON 객체를 직접 반환하고, FE는 파싱이 아닌 필드 매핑만 한다. 이모지·줄바꿈 의존을 완전히 폐기. 표정 배지가 한글로 표시.
- **Evidence** 사용자가 직접 보고한 화면 캡처 (✓는 정상, △/→가 본문에 박힘 / `NEUTRAL` 표시). 코드상 `gemini_analyzer.py`/`vision_analyzer.py`의 ✓△→ 강제 지시문과 `structured-comment.tsx`의 `split('\n')` 파서가 일치하지 않을 때 발생하는 구조적 결함임을 확인.
- **Trade-offs**
  - **포기**: 단일 string column에 ✓△→ 문자열을 그대로 저장하던 단순함
  - **얻음**: LLM 응답 변동성에 대한 견고성, FE 파서 폐기, 향후 라벨 추가 시 스키마 단위로 확장 가능
  - **고려한 대안 1 (기각)**: 프롬프트만 강화해 줄바꿈을 강제 — LLM 의존성이 여전히 남음
  - **고려한 대안 2 (기각)**: 컬럼을 5×3=15개로 분리 — 스키마 폭증, 마이그레이션 부담. `accuracy_issues`가 이미 JSON 문자열을 단일 컬럼에 저장하는 패턴이 있어 동일 패턴으로 충분
  - **고려한 대안 3 (기각)**: 기존 ✓△→ 데이터 변환 마이그레이션 작성 — dev 데이터 폐기 가능하므로 불필요. fallback 파서로 충분

## 목표

1. Lambda LLM 응답이 5종 코멘트 모두에 대해 `{positive, negative, suggestion}` 정형 JSON 객체를 반환
2. BE는 해당 객체를 JSON 문자열로 직렬화해 기존 컬럼에 저장하고, 응답 시 객체로 노출 (스키마 변경 없음)
3. FE `StructuredComment`의 prefix 파싱 로직 완전 폐기, block 필드만 사용
4. `expressionLabel` 영어 enum을 FE 매퍼로 한글화 (`NEUTRAL → 평온` 등)
5. 새 면접 1회 녹화 시 5종 코멘트가 모두 3블록(잘한 점 / 아쉬운 점 / 이렇게 해보세요)으로 분리되어 표시되고, 표정 배지가 한글로 표시되는 것을 dev 환경에서 검증

## 아키텍처 / 설계

### 데이터 흐름

```
LLM 응답 (JSON {positive,negative,suggestion})
   ↓ Lambda handler.py
BE SaveFeedbackRequest.CommentBlock 객체
   ↓ ObjectMapper.writeValueAsString
DB timestamp_feedback.{verbal,nonverbal,vocal,attitude,overall}_comment
   (기존 String 컬럼, 값만 ✓△→ → JSON 문자열)
   ↓ TimestampFeedbackResponse.parseCommentBlock
응답 DTO CommentBlock 객체
   ↓ FE
StructuredComment {block} props → 3블록 렌더
```

### 핵심 결정

| 결정 | 선택 | 근거 |
|---|---|---|
| 응답 형식 | LLM이 `{positive,negative,suggestion}` 반환 | 줄바꿈 의존 제거, 견고성 |
| DB 스키마 | **변경 없음**, 기존 5개 컬럼 재사용 | `accuracy_issues`와 동일 패턴, diff 최소 |
| 데이터 마이그레이션 | **없음** | dev 데이터 폐기 가능, fallback 파서로 처리 |
| `expressionLabel` 한글화 | FE 매퍼 추가 | 다른 enum과 일관 (백엔드는 영어 표준) |
| `emotionLabel` | **변경 없음** (이미 한글) | 마이그레이션 부담 회피 |
| `overallComment` | 5종 모두 동일 `CommentBlock`으로 통일 | 일관성 |

### CommentBlock 정의

```java
// backend
@Getter @NoArgsConstructor
public static class CommentBlock {
    private String positive;    // 잘한 점 1문장
    private String negative;    // 아쉬운 점 1문장
    private String suggestion;  // 개선 방법 1문장
}
```

```ts
// frontend
export interface CommentBlock {
  positive: string | null
  negative: string | null
  suggestion: string | null
}
```

## Scope

- **In**:
  - Lambda 프롬프트 5종(verbal/vocal/attitude/nonverbal/overall) 응답 스키마 정형화
  - Lambda `handler.py` BE 페이로드 매핑
  - BE DTO (`SaveFeedbackRequest`, `TimestampFeedbackResponse`) 직렬화 규약 변경
  - BE 단위 테스트 (`parseCommentBlock`)
  - FE 타입(`CommentBlock`), 컴포넌트(`StructuredComment`, `delivery-tab`, `content-tab`), 매퍼(`formatExpressionLabel`)
- **Out**:
  - DB 스키마 마이그레이션
  - 기존 ✓△→ 데이터 변환
  - `emotionLabel` 영어 enum 통일
  - 백엔드 enum 변경
  - 새로운 피드백 항목 추가

## 제약조건 / 환경

- **dev 단일 환경 기준**. prod 운영 중이 아니므로 트래픽 갭/롤백 시나리오 단순화 가능
- **PR 분리 규칙 준수**: BE / Lambda / FE 각자 독립 PR (CLAUDE.md). 단, **반드시 BE → Lambda → FE 순으로 같은 작업 윈도우에 머지**
- BE 응답 형식이 바뀌는 동안 짧은 화면 깨짐 윈도우 발생 가능 (수 분 단위) — dev 단일 사용자라 무시
- LLM은 OpenAI Gemini와 GPT-4o Vision 두 종류. 두 프롬프트 모두 동일한 정형 JSON을 응답하도록 갱신

## 성공 기준

- [ ] 새 면접 1회 녹화 후 피드백 페이지에서 ✓/△/→ 이모지가 화면에 단 1글자도 보이지 않음
- [ ] 5종 코멘트 모두 "잘한 점 / 아쉬운 점 / 이렇게 해보세요" 3블록으로 분리 표시
- [ ] 표정 배지가 한글로 표시 (자신감/몰입/평온/긴장/혼란)
- [ ] 기존 레거시 ✓△→ 데이터를 가진 레코드를 열어도 화면이 깨지지 않음 (raw가 positive에 표시되거나 빈 화면)
- [ ] BE 단위 테스트 `parseCommentBlock` 3 케이스(JSON / null / 손상 raw) 통과
