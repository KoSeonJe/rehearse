# Rehearse UI Architecture

사용자 여정(User Journey)에 따른 페이지 설계와 비주얼 전략입니다.

## 1. Landing Page (home-page.tsx)
- **Role**: 서비스 가치 전달 및 신뢰 구축
- **Strategy**: 'Journey-based Storytelling'. 실제 [설정-면접-리포트] 화면의 미니어처 Mockup을 스크롤 타임라인으로 배치.
- **Key Element**: 로고 독립 배치, AI 캐릭터 기반의 생동감 있는 데모(Studio background).

## 2. Onboarding Flow (onboarding-page.tsx)
- **Role**: 사용자 데이터 수집 및 서비스 이해
- **Step 1**: 직무 선택 (둥근 카드 리스트)
- **Step 2**: 시각적 여정 가이드 (우리가 만든 페이지 디자인을 활용한 요약)
- **Step 3**: 기기 테스트 (비디오 프리뷰 + 마이크 레벨 바)
- **Visual**: 토스의 '한 번에 하나씩' 철학 적용.

## 3. Interview Studio (interview-page.tsx)
- **Role**: 몰입도 높은 실전 면접 경험 제공
- **Visual**: 'Cinematic Dark Mode'. 실제 AI 비디오(HeyGen, Sora 등) 연동을 고려한 Dual-View 레이아웃.
- **Components**: 
  - 좌측: AI Interviewer (캐릭터/비디오) + HUD Overlay (감정/논리 분석 지표)
  - 우측: User Video + Real-time Transcript
- **HUD**: Glassmorphism 효과와 일렉트릭 바이올렛 포인트 사용.

## 4. Insight Report (interview-report-page.tsx)
- **Role**: 성취감 고취 및 구체적 교정 가이드 제공
- **Visual**: 'Clean Data Report'. 대담한 종합 점수 표기와 면(Surface) 기반의 강점/보완점 카드.
- **Call-to-Action**: 타임스탬프 리뷰로 연결되는 강력한 블랙 버튼.
