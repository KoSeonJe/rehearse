package com.devlens.api.infra.ai;

import com.devlens.api.domain.interview.entity.InterviewLevel;
import com.devlens.api.domain.interview.entity.InterviewType;
import com.devlens.api.infra.ai.dto.GeneratedFeedback;
import com.devlens.api.infra.ai.dto.GeneratedFollowUp;
import com.devlens.api.infra.ai.dto.GeneratedQuestion;
import com.devlens.api.infra.ai.dto.GeneratedReport;

import java.util.List;

public interface AiClient {

    List<GeneratedQuestion> generateQuestions(String position, InterviewLevel level, InterviewType interviewType);

    GeneratedFollowUp generateFollowUpQuestion(String questionContent, String answerText, String nonVerbalSummary);

    GeneratedReport generateReport(String feedbackSummary);

    List<GeneratedFeedback> generateFeedback(String answersJson);
}
