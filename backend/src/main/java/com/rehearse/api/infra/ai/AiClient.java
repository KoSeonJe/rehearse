package com.rehearse.api.infra.ai;

import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.InterviewType;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.interview.dto.FollowUpRequest;
import com.rehearse.api.infra.ai.dto.GeneratedFeedback;
import com.rehearse.api.infra.ai.dto.GeneratedFollowUp;
import com.rehearse.api.infra.ai.dto.GeneratedQuestion;
import com.rehearse.api.infra.ai.dto.GeneratedReport;

import java.util.List;

public interface AiClient {

    List<GeneratedQuestion> generateQuestions(Position position, String positionDetail,
                                              InterviewLevel level, List<InterviewType> interviewTypes,
                                              List<String> csSubTopics, String resumeText,
                                              Integer durationMinutes);

    GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText,
                                                String nonVerbalSummary,
                                                List<FollowUpRequest.FollowUpExchange> previousExchanges);

    GeneratedReport generateReport(String feedbackSummary);

    List<GeneratedFeedback> generateFeedback(String answersJson);
}
