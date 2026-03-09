package com.devlens.api.domain.feedback.service;

import com.devlens.api.domain.feedback.dto.*;
import com.devlens.api.domain.feedback.entity.*;
import com.devlens.api.domain.feedback.repository.FeedbackRepository;
import com.devlens.api.domain.feedback.repository.InterviewAnswerRepository;
import com.devlens.api.domain.interview.entity.Interview;
import com.devlens.api.domain.interview.entity.InterviewStatus;
import com.devlens.api.domain.interview.repository.InterviewRepository;
import com.devlens.api.global.exception.BusinessException;
import com.devlens.api.infra.ai.AiClient;
import com.devlens.api.infra.ai.dto.GeneratedFeedback;
import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.http.HttpStatus;
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
    private final InterviewRepository interviewRepository;
    private final AiClient aiClient;
    private final ObjectMapper objectMapper;

    @Transactional
    public FeedbackListResponse generateFeedback(Long interviewId, GenerateFeedbackRequest request) {
        Interview interview = findInterviewById(interviewId);

        if (interview.getStatus() != InterviewStatus.COMPLETED) {
            throw new BusinessException(
                    HttpStatus.CONFLICT,
                    "FEEDBACK_001",
                    "완료된 면접에서만 피드백을 생성할 수 있습니다."
            );
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
        findInterviewById(interviewId);

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

    private Interview findInterviewById(Long id) {
        return interviewRepository.findById(id)
                .orElseThrow(() -> new BusinessException(
                        HttpStatus.NOT_FOUND,
                        "INTERVIEW_001",
                        "면접 세션을 찾을 수 없습니다."
                ));
    }

    private String serializeAnswers(List<AnswerData> answers) {
        try {
            return objectMapper.writeValueAsString(answers);
        } catch (JsonProcessingException e) {
            throw new BusinessException(HttpStatus.INTERNAL_SERVER_ERROR, "FEEDBACK_002", "답변 데이터 직렬화에 실패했습니다.");
        }
    }
}
