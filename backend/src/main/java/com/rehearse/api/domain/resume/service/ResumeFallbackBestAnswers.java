package com.rehearse.api.domain.resume.service;

final class ResumeFallbackBestAnswers {

    static final String OPENER =
            "이번 답변은 본인 프로젝트의 핵심 역할과 주요 성과를 시간 순으로 풀어내는 구조가 효과적입니다. "
                    + "어떤 프로젝트였는지 짧게 짚고, 본인이 담당한 역할과 직접 수행한 작업 (Action) 을 구체 서술하세요. "
                    + "결정 근거와 트레이드오프, 그리고 그 결과 (Result) 로 얻은 학습을 한 줄로 마무리하면 "
                    + "STAR 구조 (Situation / Task / Action / Result) 가 자연스럽게 채워집니다.";

    static final String PLAYGROUND =
            "방금 답변에서 떠오른 결정적인 순간 한 가지를 골라, 그 상황 (Situation) 과 풀어야 했던 과제 (Task) 를 "
                    + "짧게 정리하는 흐름이 효과적입니다. 본인이 직접 수행한 작업 (Action) 과 그 결과 (Result) 를 "
                    + "구체 수치로 짚고, 결정 근거와 다른 선택지와의 트레이드오프를 비교하면 답변의 깊이가 살아납니다. "
                    + "마지막에 그 경험에서 얻은 학습 한 줄로 마무리하세요.";

    static final String INTERROGATION =
            "현재 질문 레벨에 맞춰 답변 깊이를 조절하는 것이 핵심입니다. 정의 (WHAT) 단계는 개념과 사용 맥락을, "
                    + "적용 (HOW) 단계는 본인이 직접 구현한 단계 흐름을, 원리 (WHY) 단계는 내부 메커니즘과 결정 근거를, "
                    + "비교 (TRADEOFF) 단계는 측정값 기반 트레이드오프와 다른 후보와의 비교를 짚는 구조가 효과적입니다. "
                    + "STAR 구조와 함께 그 경험에서 얻은 학습 한 줄로 마무리하세요.";

    private ResumeFallbackBestAnswers() {}
}
