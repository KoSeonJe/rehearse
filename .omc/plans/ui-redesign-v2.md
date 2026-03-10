# UI 리디자인 v2 — 모노톤 전환 + 홈페이지 재구성

- **Status**: Completed
- **Created**: 2026-03-10
- **Branch**: feat/ui-redesign-v2
- **PR**: #22

## 배경

사용자 피드백:
- 온보딩 페이지가 심심하고 서비스 특별점이 없다
- 다른 웹페이지와 형식이 비슷해서 AI가 만든 것 같다
- 주황색(coral accent)이 별로다 — 심플하고 안 튀는 방향 원함
- 서비스만의 차별점을 강조해줬으면 좋겠다

## 분석 결과

### 문제점
1. Hero → 3-column Cards → 3-step → CTA 구조가 AI 템플릿의 전형
2. Coral accent(#FF6B4A)이 playful 톤 → 면접 서비스의 "전문성, 신뢰" 톤과 충돌
3. 킬러 피처(타임스탬프 비디오 피드백)가 텍스트 한 줄로만 설명
4. 모든 페이지 동일 리듬 (같은 카드, 같은 레이아웃)
5. 캐릭터 float 애니메이션이 유치한 인상

### 참고 디자인 철학
- **토스**: 모노톤 + 여백으로 호흡 + 제품이 스스로 말하게
- **Linear/Vercel**: 검정 CTA, 극단적 타이포 weight 대비
- **NNGroup 연구**: "Show, Don't Tell" — 제품 스크린샷/데모가 텍스트 설명 대비 전환율 40% 높음

## 변경 사항

### 1. 디자인 토큰 (tailwind.config.js)
- accent: `#FF6B4A` → `#191F28` (모노톤)
- accent-hover: `#E5593B` → `#333D4B`
- accent-light: `#FFF0ED` → `#F2F4F6`
- background: `#FAFAFA` → `#F7F8FA` (쿨톤)
- border: `#E8E8E8` → `#E5E8EB` (쿨톤)
- float 애니메이션 제거

### 2. 홈페이지 (home-page.tsx)
- AI 템플릿 구조 탈피 → 제품 데모 중심 레이아웃
- Hero: `font-extralight`(200) + `font-extrabold`(800) 대비
- JetBrains Mono 레이블로 개발자 정체성 (`01 — ai questions`)
- 피드백 리뷰 UI 목업(브라우저 프레임 + 비디오 + 피드백 패널)으로 킬러 피처 직접 노출
- 3-column feature cards → 좌정렬 교차 레이아웃
- 캐릭터 사용 제거 (홈페이지에서)

### 3. 온보딩 (step-job-field.tsx, step-device-test.tsx)
- `font-mono` step 레이블 추가 (`step 1`, `step 2`)
- 타이포 강화 (`font-bold tracking-tight`)
- 직무 선택 카드: accent 색상 → text-primary 기반으로 변경
- 마이크 레벨바: accent → success(초록색) — 의미 기반 색상

### 4. 면접 컨트롤 (interview-controls.tsx)
- 녹음 인디케이터: accent → error(빨간색) — 녹음 상태 직관적 표현

## 영향 범위

### 자동 적용 (accent 토큰 변경으로)
- Button 컴포넌트 (primary, cta → 검정 버튼)
- SelectionCard 선택 상태
- ProgressBar
- Spinner
- BackLink focus ring
- FeedbackPanel active border
- QuestionDisplay/QuestionCard 번호 원
- TimelineMarker focus ring

### 수동 변경
- home-page.tsx — 전면 재작성
- step-job-field.tsx — 스타일 수정
- step-device-test.tsx — 스타일 + 마이크 레벨바 색상
- interview-controls.tsx — 녹음 인디케이터 색상

## 테스트 체크리스트
- [ ] 홈페이지: 모노톤 CTA, 제품 데모 목업, 반응형
- [ ] 온보딩: 직무 선택 카드 선택/해제 상태
- [ ] 면접 설정: SelectionCard 선택 상태 모노톤
- [ ] 면접 진행: 녹음 인디케이터 빨간색, 진행바 검정
- [ ] 피드백 리뷰: active 피드백 border 검정
- [ ] 전체 접근성: focus ring 가시성 확인
