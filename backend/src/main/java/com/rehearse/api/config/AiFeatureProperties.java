package com.rehearse.api.config;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.cloud.context.config.annotation.RefreshScope;
import org.springframework.stereotype.Component;

@Component
@RefreshScope
@ConfigurationProperties(prefix = "rehearse.features")
public class AiFeatureProperties {

    private FeatureFlag intentClassifier = new FeatureFlag();
    private FeatureFlag answerAnalyzer = new FeatureFlag();
    private FeatureFlag followupV3 = new FeatureFlag();
    private FeatureFlag resumeTrack = new FeatureFlag();
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
