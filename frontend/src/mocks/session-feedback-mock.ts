import type { SessionFeedbackData } from '@/types/session-feedback'

export const buildMockSessionFeedback = (interviewId: number): SessionFeedbackData => ({
  id: 9999,
  interviewId,
  status: 'COMPLETE',
  overall: {
    dimensionScores: {
      conceptual_accuracy: 3,
      system_design: 2,
      communication: 4,
    },
    levelAssessment: 'Mid (3년차) 수준 — 신입~주니어 사이',
    narrative:
      '핵심 개념을 본인 언어로 설명하는 능력은 평균 이상입니다. 다만 시스템 설계 질문에서 트레이드오프를 명시하지 않고 한 방향으로만 답하는 경향이 보였습니다. 실무 경험에서 나온 예시는 신뢰도를 높였으나, 근거 제시가 수치 없이 추상적으로 끝나는 경우가 많아 아쉬움이 남습니다.',
    coverage: '5문항 중 4문항 충실 답변, 1문항 시간 부족',
  },
  strengths: [
    {
      dimension: '문제 정의',
      observation: '질문 의도를 다시 정리해 답변에 들어감',
      whyMatters: '면접관 의도 일치율↑',
    },
    {
      dimension: '예시 활용',
      observation: '추상 개념을 본인 프로젝트와 연결',
      whyMatters: '실무 신뢰도 가산점',
    },
  ],
  gaps: [
    {
      dimension: '시스템 설계',
      observation: 'CAP·정합성 트레이드오프 언급 부족',
      levelGap: '−1단계',
      concreteAction: '대규모 시스템 설계 면접 책 5장 학습 후 1문제 모의 답변',
    },
    {
      dimension: '근거 제시',
      observation: '"빠르다/효율적이다" 표현이 수치·벤치마크로 보강되지 않음',
      levelGap: '−0.5단계',
      concreteAction: '답변 시 항상 "수치 → 비교 대상 → 결론" 3단 구조 적용',
    },
  ],
  delivery: {
    fillerWords: '"음", "그…" 분당 4.2회 — 평균 대비 1.6배',
    tonePattern: '문장 끝이 올라감 → 자신감 부족 인상',
    action: '문장 끝을 0.3초 더 늘리고 평탄하게 맺는 연습',
  },
  weekPlan: [
    {
      priority: 1,
      topic: '분산 시스템 기초 복습',
      resources: ['DDIA Ch.5–7', '카카오 테크 블로그: Eventually Consistent'],
      practice: '본인 프로젝트의 정합성 모델을 5문장으로 설명하기',
    },
    {
      priority: 2,
      topic: '근거 강화 답변 패턴',
      resources: ['모의 면접 5세트 (이 앱 활용)'],
      practice: '답변마다 수치 1개 의무 포함',
    },
    {
      priority: 3,
      topic: '딜리버리 클리닝',
      resources: [],
      practice: '하루 5분 자기 녹음 후 filler 카운트',
    },
  ],
  coverage: '주요 도메인 80% 커버',
  deliveryRetryable: false,
  lastFailureReason: null,
  retryAttempts: 0,
  createdAt: new Date().toISOString(),
  updatedAt: new Date().toISOString(),
})
