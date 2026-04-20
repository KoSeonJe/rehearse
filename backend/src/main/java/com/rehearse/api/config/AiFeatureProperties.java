package com.rehearse.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

/**
 * AI Feature Flag 런타임 설정.
 *
 * <p>{@code POST /actuator/refresh} 호출 시 {@code @RefreshScope} 에 의해 bean 이 재생성되어
 * {@code rehearse.features.*} 값이 서버 재시작 없이 갱신된다.</p>
 *
 * <p>하위 plan 에서 신규 flag 추가 시 이 클래스에 필드를 추가한다.</p>
 */
@Component
@RefreshScope
@ConfigurationProperties(prefix = "rehearse.features")
public class AiFeatureProperties {

    /** plan-01: Intent Classifier (ANSWER / CLARIFY_REQUEST / GIVE_UP 분류기) */
    private FeatureFlag intentClassifier = new FeatureFlag();

    /** plan-02: Answer Analyzer (Step A — 답변 분석) */
    private FeatureFlag answerAnalyzer = new FeatureFlag();

    /** plan-03: Follow-up Generator v3 (Step B — 꼬리질문 생성 v3) */
    private FeatureFlag followupV3 = new FeatureFlag();

    /** plan-05/06/07: Resume Track (이력서 기반 면접 트랙) */
    private FeatureFlag resumeTrack = new FeatureFlag();

    /** plan-04: Context Engineering 4-layer Builder */
    private FeatureFlag contextEngineering = new FeatureFlag();

    public FeatureFlag getIntentClassifier() { return intentClassifier; }
    public void setIntentClassifier(FeatureFlag intentClassifier) { this.intentClassifier = intentClassifier; }

    public FeatureFlag getAnswerAnalyzer() { return answerAnalyzer; }
    public void setAnswerAnalyzer(FeatureFlag answerAnalyzer) { this.answerAnalyzer = answerAnalyzer; }

    public FeatureFlag getFollowupV3() { return followupV3; }
    public void setFollowupV3(FeatureFlag followupV3) { this.followupV3 = followupV3; }

    public FeatureFlag getResumeTrack() { return resumeTrack; }
    public void setResumeTrack(FeatureFlag resumeTrack) { this.resumeTrack = resumeTrack; }

    public FeatureFlag getContextEngineering() { return contextEngineering; }
    public void setContextEngineering(FeatureFlag contextEngineering) { this.contextEngineering = contextEngineering; }

    public static class FeatureFlag {
        private boolean enabled = false;

        public boolean isEnabled() { return enabled; }
        public void setEnabled(boolean enabled) { this.enabled = enabled; }
    }
}
