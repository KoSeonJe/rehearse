package com.rehearse.api.domain.interview.service;

import com.rehearse.api.domain.interview.dto.FollowUpResponse;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import org.springframework.stereotype.Component;

@Component
public class FollowUpResponseBuilder {

    public FollowUpResponse buildAnswerResponse(GeneratedFollowUp followUp, Question savedQuestion, boolean exhausted) {
        return FollowUpResponse.builder()
                .questionId(savedQuestion.getId())
                .question(followUp.question())
                .ttsQuestion(followUp.ttsQuestion())
                .reason(followUp.reason())
                .type(followUp.type())
                .answerText(followUp.answerText())
                .bestAnswer(savedQuestion.getBestAnswer())
                .skip(false)
                .presentToUser(true)
                .followUpExhausted(exhausted)
                .selectedAnswerFeedbackPerspective(followUp.selectedAnswerFeedbackPerspective())
                .build();
    }
}
