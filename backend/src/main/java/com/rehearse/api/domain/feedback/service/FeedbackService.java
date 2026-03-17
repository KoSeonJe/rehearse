package com.rehearse.api.domain.feedback.service;

import com.rehearse.api.domain.feedback.dto.*;
import com.rehearse.api.domain.feedback.entity.*;
import com.rehearse.api.domain.feedback.exception.FeedbackErrorCode;
import com.rehearse.api.domain.feedback.repository.FeedbackRepository;
import com.rehearse.api.domain.feedback.repository.InterviewAnswerRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewStatus;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.ai.AiClient;
import com.rehearse.api.infra.ai.dto.GeneratedFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class FeedbackService {

    private final FeedbackRepository feedbackRepository;
    private final InterviewAnswerRepository interviewAnswerRepository;
    private final InterviewFinder interviewFinder;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public FeedbackListResponse generateFeedback(Long interviewId, GenerateFeedbackRequest request) {
        Interview interview = interviewFinder.findById(interviewId);

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new BusinessException(FeedbackErrorCode.INTERVIEW_NOT_COMPLETED);
        }

        saveAnswers(interview, request.getAnswers());
        List<Feedback> feedbacks = generateAndSaveFeedbacks(interview, request.getAnswers());

        log.info("피드백 생성 완료: interviewId={}, feedbackCount={}", interviewId, feedbacks.size());

        List<FeedbackResponse> responses = feedbacks.stream()
                .map(FeedbackResponse::from)
                .toList();

        return FeedbackListResponse.builder()
                .interviewId(interviewId)
                .feedbacks(responses)
                .totalCount(responses.size())
                .build();
    }

    public FeedbackListResponse getFeedbacks(Long interviewId) {
        interviewFinder.findById(interviewId);

        List<Feedback> feedbacks = feedbackRepository.findByInterviewIdOrderByTimestampSeconds(interviewId);
        List<FeedbackResponse> responses = feedbacks.stream()
                .map(FeedbackResponse::from)
                .toList();

        return FeedbackListResponse.builder()
                .interviewId(interviewId)
                .feedbacks(responses)
                .totalCount(responses.size())
                .build();
    }

    private void saveAnswers(Interview interview, List<AnswerData> answers) {
        for (AnswerData answerData : answers) {
            InterviewAnswer answer = InterviewAnswer.builder()
                    .interview(interview)
                    .questionIndex(answerData.getQuestionIndex())
                    .questionContent(answerData.getQuestionContent())
                    .answerText(answerData.getAnswerText())
                    .nonVerbalSummary(answerData.getNonVerbalSummary())
                    .voiceSummary(answerData.getVoiceSummary())
                    .build();
            interviewAnswerRepository.save(answer);
        }
    }

    private List<Feedback> generateAndSaveFeedbacks(Interview interview, List<AnswerData> answers) {
        String answersJson = serializeAnswers(answers);
        List<GeneratedFeedback> generatedFeedbacks = aiClient.generateFeedback(answersJson);

        List<Feedback> feedbacks = generatedFeedbacks.stream()
                .map(gf -> Feedback.builder()
                        .interview(interview)
                        .timestampSeconds(gf.getTimestampSeconds())
                        .category(FeedbackCategory.valueOf(gf.getCategory()))
                        .severity(FeedbackSeverity.valueOf(gf.getSeverity()))
                        .content(gf.getContent())
                        .suggestion(gf.getSuggestion())
                        .build())
                .toList();

        feedbackRepository.saveAll(feedbacks);
        return feedbacks;
    }

    private String serializeAnswers(List<AnswerData> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new BusinessException(FeedbackErrorCode.SERIALIZATION_FAILED);
        }
    }
}
