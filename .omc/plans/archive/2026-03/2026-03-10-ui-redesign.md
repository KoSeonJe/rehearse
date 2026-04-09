# 리허설 (Rehearse) — UI/UX 리디자인 스펙

> **문서 ID**: PLAN-010
> **작성일**: 2026-03-10
> **상태**: Completed
> **우선순위**: P2 (Polish)
>
> Rehearse → Rehearse 리브랜딩 + 토스 디자인 철학 기반 전면 리디자인

---

## 1. 브랜딩

```
서비스명: 리허설 (Rehearse)
슬로건: "면접, 다시 한 번"
톤: 따뜻하지만 전문적. 응원하되 가볍지 않은.
```

### 로고 캐릭터

- 동그란 얼굴 캐릭터가 거울 앞에 선 모습
- 거울 속 자신은 자신감 있는 표정 (미소)
- 거울 밖 자신은 살짝 긴장된 표정
- 선화(stroke) 스타일, 2px 굵기
- 색상: slate-800 단색 (다크모드 시 slate-200)
- 최소 사이즈: 24x24px (파비콘), 최대: 자유

### 활용

- 파비콘: 캐릭터만 (거울 없이 얼굴만)
- 헤더: 캐릭터 + "Rehearse" 워드마크
- 로딩/빈 상태: 캐릭터에 다양한 표정 부여
  - 로딩: 생각하는 표정
  - 에러: 당황 표정
  - 성공: 환한 미소

### 워드마크

- "Rehearse" Pretendard SemiBold
- 자간(letter-spacing) -0.02em
- 한글 "리허설"은 서브 텍스트로만 사용

---

## 2. 컬러 시스템

```
── 베이스 (모노톤) ──
Background:     #FAFAFA
Surface:        #FFFFFF
Border:         #E8E8E8
Text Primary:   #191F28
Text Secondary: #6B7684
Text Tertiary:  #AEB5BC

── 액센트 (웜 코랄) ──
Primary:        #FF6B4A
Primary Hover:  #E5593B
Primary Light:  #FFF0ED

── 시맨틱 ──
Success:        #00C48C  / Light: #E8FAF4
Warning:        #FFB84D  / Light: #FFF6E5
Error:          #F04452  / Light: #FFF0F1
Info:           #3182F6  / Light: #EBF4FF
```

### 컬러 원칙

- 메인 UI는 모노톤, 액센트는 CTA와 핵심 포인트에만
- AI 서비스 느낌 나는 그라데이션/네온 사용 금지
- 피드백 카드는 시맨틱 Light 버전을 배경으로
- 코랄 액센트는 화면당 1~2개소에만 (남발 금지)

---

## 3. 타이포그래피 + 간격

### 폰트

- Primary: Pretendard Variable
- Mono: JetBrains Mono

### 스케일

| 이름 | 크기 | 굵기 | 행간 | 용도 |
|------|------|------|------|------|
| Display | 32px | Bold | 1.3 | 페이지 히어로 |
| Heading | 24px | SemiBold | 1.4 | 섹션 제목 |
| Title | 20px | SemiBold | 1.4 | 카드 제목 |
| Body | 16px | Regular | 1.6 | 본문 |
| Caption | 14px | Regular | 1.5 | 보조 설명 |
| Small | 12px | Medium | 1.4 | 뱃지, 태그 |

### 간격 (4px 베이스)

4 / 8 / 12 / 16 / 20 / 24 / 32 / 40 / 48 / 64

### 컴포넌트 표준

- 카드 패딩: 24px
- 섹션 간격: 48px
- 페이지 좌우: 모바일 16px / 데스크톱 auto (max-w-3xl 중앙)
- Border Radius: 12px (카드), 8px (버튼/인풋), 999px (뱃지/태그)

---

## 4. 페이지 구조 + UX 플로우

### 전체 플로우

```
[랜딩] → [온보딩(신규)] → [면접 설정] → [준비] → [진행] → [완료] → [리뷰] → [리포트]
                                                                         ↕
                                                                    (자유 이동)
```

### 신규 페이지

1. **랜딩 페이지 (리디자인)**
   - 히어로: 슬로건 + 캐릭터 일러스트 + CTA
   - 3단 가치 제안 (AI 질문생성, 비언어 분석, 타임스탬프 피드백)
   - 사용 흐름 미리보기 (3 step 시각화)
   - 하단 CTA 반복

2. **온보딩 (신규)**
   - Step 1: "어떤 면접을 준비하세요?" (직무 선택)
   - Step 2: 카메라/마이크 권한 + 테스트
   - Step 3: 간단 가이드 (3장 캐러셀)
   - 진행률 바 상단 고정

### 기존 페이지 개선

3. **면접 설정**: 카드 기반 선택 UX, 이력서 업로드 영역 강화
4. **면접 준비**: 질문 미리보기 + "긴장 풀기 팁"
5. **면접 진행**: 2단 레이아웃(비디오 좌+질문 우) → 모바일 스택
6. **완료 페이지**: 캐릭터 애니메이션 + 분석 진행률 표시
7. **피드백 리뷰**: 비디오+타임라인+피드백 동기화 강화
8. **종합 리포트**: 레이더 차트 + 강점/개선 카드 리디자인

### 공통 UX 원칙

- 모든 페이지 상단에 현재 단계 표시
- 로딩: 스켈레톤 + 캐릭터 표정 변화
- 에러: 캐릭터 당황 표정 + 친근한 안내 문구
- 빈 상태: 캐릭터 + 행동 유도 메시지

---

## 5. 컴포넌트 디자인 가이드

### 버튼

| 변형 | 배경 | 텍스트 | Radius | 패딩 |
|------|------|--------|--------|------|
| Primary | #FF6B4A | white | 8px | px-6 py-3 |
| Secondary | white (border #E8E8E8) | #191F28 | 8px | px-6 py-3 |
| Ghost | transparent | #6B7684 | 8px | px-4 py-2 |
| CTA | #FF6B4A | white | 8px | px-8 py-4 text-lg |

- Hover: 0.15s ease, 배경색 변화만 (scale 금지)
- Loading: 텍스트 → 스피너 교체 (버튼 크기 유지)

### 카드

- 기본: bg white, border #E8E8E8, rounded-xl(12px), p-6
- 호버: hover시 border #D1D5DB + shadow-sm (0.2s ease)
- 선택: border #FF6B4A + bg #FFF0ED
- 피드백: 좌측 4px 컬러 바 (severity별 시맨틱 컬러)

### 인풋

- 기본: border #E8E8E8, rounded-lg(8px), px-4 py-3
- 포커스: border #191F28, ring-1 #191F28
- 에러: border #F04452, 하단에 에러 메시지

### 뱃지/태그

- 기본: bg #F5F5F5, text #6B7684, rounded-full, px-3 py-1
- 카테고리: bg #FFF0ED, text #FF6B4A

### 모션 원칙

- 페이지 전환: fade-in 0.2s
- 카드 리스트: stagger 0.05s
- 숫자 카운트: 1s ease-out
- 과장된 bounce/elastic 금지
- transform + opacity만 사용 (GPU 가속)

---

## 6. 구현 우선순위

### Phase 1: 기반 (브랜딩 + 디자인 시스템)

- 1-1. 서비스명 변경 (Rehearse → Rehearse)
- 1-2. 캐릭터 로고 SVG + 파비콘
- 1-3. Tailwind 설정 업데이트
- 1-4. 공통 UI 컴포넌트 리디자인

### Phase 2: 랜딩 + 온보딩

- 2-1. 랜딩 페이지 전면 리디자인
- 2-2. 온보딩 페이지 신규 개발
- 2-3. 캐릭터 활용 (히어로, 로딩, 에러)

### Phase 3: 핵심 페이지 리디자인

- 3-1. 면접 설정 페이지
- 3-2. 면접 준비 페이지
- 3-3. 면접 진행 페이지

### Phase 4: 피드백 UX 강화

- 4-1. 완료 페이지
- 4-2. 피드백 리뷰 페이지
- 4-3. 종합 리포트 페이지

### Phase 5: 폴리싱

- 5-1. 마이크로 인터랙션
- 5-2. 반응형 전체 점검
- 5-3. 접근성 점검
