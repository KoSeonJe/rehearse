package com.rehearse.api.domain.resume.service;

final class ResumeFallbackModelAnswers {

    static final String OPENER =
            "저는 본인 프로젝트에서 핵심 역할을 맡아 주요 기술 도입을 주도한 경험이 있습니다. "
                    + "시작 단계의 상황(Situation)과 풀어야 했던 과제(Task)를 짧게 짚고, "
                    + "직접 수행한 작업(Action)과 그 결과(Result)를 시간 흐름으로 풀어 답변했습니다. "
                    + "이때 적용한 결정 근거와 함께, 다른 후보와의 트레이드오프를 비교해 최종 선택을 정당화한 학습 과정을 정리했습니다.";

    static final String PLAYGROUND =
            "방금 답변에서 떠올린 결정적인 순간을 저는 좀 더 풀어 답변했습니다. "
                    + "그 순간의 상황(Situation)과 마주한 과제(Task)를 짧게 정리하고, "
                    + "본인이 직접 적용한 작업(Action)과 그 결과(Result)를 구체 수치로 짚었습니다. "
                    + "결정 근거가 무엇이었는지, 그리고 다른 선택지와의 트레이드오프를 비교한 학습을 함께 진행했습니다. "
                    + "결과 수치는 응답 시간과 처리량 변화로 측정했습니다.";

    static final String INTERROGATION =
            "현재 질문 깊이에 맞춰 저는 본인이 직접 수행한 작업(Action)과 그 결과(Result)를 풀어 답변했습니다. "
                    + "구현 단계의 상황(Situation)과 풀어야 했던 과제(Task)를 짧게 짚고, "
                    + "내부 메커니즘과 결정 근거를 설명했습니다. "
                    + "끝으로 대안 비교에서는 다른 후보와의 트레이드오프를 측정값과 함께 정리하고, "
                    + "운영 기준으로 최종 선택을 정당화한 학습을 진행했습니다.";

    private ResumeFallbackModelAnswers() {}
}
