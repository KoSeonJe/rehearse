package com.devlens.api.domain.feedback.service;

import com.devlens.api.domain.feedback.dto.*;
import com.devlens.api.domain.feedback.entity.*;
import com.devlens.api.domain.feedback.exception.FeedbackErrorCode;
import com.devlens.api.domain.feedback.repository.FeedbackRepository;
import com.devlens.api.domain.feedback.repository.InterviewAnswerRepository;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.service.InterviewFinder;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.AiClient;
import com.devlens.api.infra.ai.dto.GeneratedFeedback;
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

        // 답변 저장
        for (AnswerData answerData : request.getAnswers()) {
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

        // Claude API로 피드백 생성
        String answersJson = serializeAnswers(request.getAnswers());
        List<GeneratedFeedback> generatedFeedbacks = aiClient.generateFeedback(answersJson);

        // 피드백 저장
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

    private String serializeAnswers(List<AnswerData> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new BusinessException(FeedbackErrorCode.SERIALIZATION_FAILED);
        }
    }
}
