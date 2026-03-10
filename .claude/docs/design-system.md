# Rehearse Design System (v7)

리허설의 브랜드 정체성과 UI 일관성을 유지하기 위한 디자인 가이드입니다.

## 1. Core Visual Identity
- **Concept**: Warm Minimalism & Smart Companion (Toss-like flow with unique tech edge)
- **Signature Color**: `Electric Violet (#6366F1)` - 지능형 서비스와 자신감을 상징
- **Base Color**: `Deep Charcoal (#0F172A)` - 신뢰감 있는 텍스트와 레이아웃 기반
- **Background**: `Pure White (#FFFFFF)` & `Surface Gray (#F8FAFC)` - 보더 대신 면(Surface)으로 구획 분리

## 2. Palette (tailwind.config.js)
- `accent`: #6366F1 (Hover: #4F46E5, Light: #EEF2FF)
- `text-primary`: #0F172A
- `text-secondary`: #475569
- `surface`: #F8FAFC
- `success`: #10B981
- `error`: #EF4444

## 3. Typography & Shapes
- **Scale**: 과도한 크기를 지양하고 가독성 중심의 `text-4xl` ~ `text-6xl` 헤드라인 사용
- **Weight**: `font-extrabold` 또는 `font-black`으로 시각적 위계 강조
- **Rounding**: `20px` (Cards), `24px` (Large Buttons/Sections) - 부드러운 인상
- **Shadow**: `shadow-toss` (은은한 그림자), `shadow-accent/20` (포인트 강조)

## 4. Brand Character: 리리(Ri-Ri)
- **Design**: 로고의 '거울 속 자신감 넘치는 캐릭터'와 비주얼 언어 통합
- **Visuals**: 3px 두께의 라인, 둥근 두상, 바이올렛 컬러의 눈
- **Moods**: `default`, `happy`, `thinking`, `confused`, `recording`
- **Animation**: `animate-float`, `animate-bounce-subtle`, `animate-pulse` 등 적용
