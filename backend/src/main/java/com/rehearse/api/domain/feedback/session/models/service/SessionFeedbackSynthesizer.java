package com.rehearse.api.domain.feedback.session.models.service;

import com.rehearse.api.domain.feedback.session.synthesis.SessionFeedbackInput;
import com.rehearse.api.infra.ai.dto.GeneratedSessionFeedback;

public interface SessionFeedbackSynthesizer {

    GeneratedSessionFeedback synthesize(SessionFeedbackInput input);
}
