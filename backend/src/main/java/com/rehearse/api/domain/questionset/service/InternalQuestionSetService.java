package com.rehearse.api.domain.questionset.service;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.questionset.dto.*;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.*;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.retry.annotation.Backoff;
import org.springframework.retry.annotation.Retryable;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.transaction.support.TransactionSynchronization;
import org.springframework.transaction.support.TransactionSynchronizationManager;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class InternalQuestionSetService {

    private final QuestionSetRepository questionSetRepository;
    private final QuestionSetAnalysisRepository analysisRepository;
    private final QuestionAnswerRepository answerRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final InterviewFinder interviewFinder;
    private final S3Service s3Service;
    private final ObjectMapper objectMapper;

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void updateProgress(Long questionSetId, UpdateProgressRequest request) {
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        if (request.getStatus() == AnalysisStatus.FAILED) {
            analysis.markFailed(request.getFailureReason(), request.getFailureDetail());
            log.warn("분석 실패: questionSetId={}, reason={}", questionSetId, request.getFailureReason());
            return;
        }

        try {
            analysis.updateAnalysisStatus(request.getStatus());
        } catch (IllegalStateException e) {
            throw new BusinessException(QuestionSetErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
        }
        log.info("분석 상태 업데이트: questionSetId={}, status={}", questionSetId, request.getStatus());
    }

    public QuestionSet getQuestionSet(Long questionSetId) {
        return findQuestionSet(questionSetId);
    }

    public List<QuestionAnswer> getAnswers(Long questionSetId) {
        return answerRepository.findByQuestionSetIdWithQuestion(questionSetId);
    }

    public AnswersResponse getAnswersResponse(Long interviewId, Long questionSetId) {
        QuestionSet questionSet = findQuestionSet(questionSetId);
        Interview interview = interviewFinder.findById(interviewId);

        List<AnswerResponse> answers = getAnswers(questionSetId).stream()
                .map(AnswerResponse::from)
                .toList();

        return AnswersResponse.builder()
                .analysisStatus(questionSet.getEffectiveAnalysisStatus().name())
                .position(interview.getPosition().name())
                .techStack(interview.getTechStack() != null ? interview.getTechStack().name() : null)
                .level(interview.getLevel().name())
                .answers(answers)
                .build();
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void saveFeedback(Long questionSetId, SaveFeedbackRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment(request.getQuestionSetComment())
                .build();

        if (request.getTimestampFeedbacks() != null) {
            for (SaveFeedbackRequest.TimestampFeedbackItem item : request.getTimestampFeedbacks()) {
                Question question = null;
                if (item.getQuestionId() != null) {
                    question = questionRepository.findById(item.getQuestionId())
                            .orElseGet(() -> {
                                log.warn("피드백 저장 시 존재하지 않는 questionId={}", item.getQuestionId());
                                return null;
                            });
                }

                TimestampFeedback tf = TimestampFeedback.builder()
                        .question(question)
                        .startMs(item.getStartMs())
                        .endMs(item.getEndMs())
                        .transcript(item.getTranscript())
                        .verbalComment(item.getVerbalComment())
                        .fillerWordCount(item.getFillerWordCount())
                        .eyeContactLevel(item.getEyeContactLevel())
                        .postureLevel(item.getPostureLevel())
                        .expressionLabel(item.getExpressionLabel())
                        .nonverbalComment(item.getNonverbalComment())
                        .overallComment(item.getOverallComment())
                        .isAnalyzed(true)
                        .fillerWords(toJson(item.getFillerWords()))
                        .speechPace(item.getSpeechPace())
                        .toneConfidenceLevel(item.getToneConfidenceLevel())
                        .emotionLabel(item.getEmotionLabel())
                        .vocalComment(item.getVocalComment())
                        .accuracyIssues(item.getAccuracyIssues())
                        .coachingStructure(item.getCoachingStructure())
                        .coachingImprovement(item.getCoachingImprovement())
                        .attitudeComment(item.getAttitudeComment())
                        .build();
                feedback.addTimestampFeedback(tf);
            }
        }

        feedbackRepository.save(feedback);
        analysis.completeAnalysis(
                request.isVerbalCompleted(),
                request.isNonverbalCompleted()
        );

        log.info("분석 결과 저장 완료: questionSetId={}, verbal={}, nonverbal={}",
                questionSetId, request.isVerbalCompleted(), request.isNonverbalCompleted());
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void updateConvertStatus(Long questionSetId, UpdateConvertStatusRequest request) {
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);
        if (request.getStatus() == ConvertStatus.FAILED) {
            analysis.markConvertFailed(request.getFailureReason());
        } else {
            try {
                analysis.updateConvertStatus(request.getStatus());
            } catch (IllegalStateException e) {
                throw new BusinessException(QuestionSetErrorCode.INVALID_CONVERT_STATUS_TRANSITION);
            }
        }

        if (request.getStreamingS3Key() != null) {
            QuestionSet qs = analysis.getQuestionSet();
            var file = qs.getFileMetadata();
            if (file != null) {
                file.updateStreamingS3Key(request.getStreamingS3Key());
            }
        }

        log.info("변환 상태 업데이트: questionSetId={}, status={}", questionSetId, request.getStatus());
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void retryAnalysis(Long questionSetId) {
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        if (!analysis.getAnalysisStatus().isRetryable()) {
            throw new BusinessException(QuestionSetErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
        }

        AnalysisStatus previousStatus = analysis.getAnalysisStatus();
        analysis.retry();

        QuestionSet questionSet = analysis.getQuestionSet();
        var file = questionSet.getFileMetadata();
        if (file == null) {
            throw new BusinessException(QuestionSetErrorCode.FILE_NOT_FOUND);
        }

        String s3Key = file.getS3Key();
        TransactionSynchronizationManager.registerSynchronization(new TransactionSynchronization() {
            @Override
            public void afterCommit() {
                try {
                    s3Service.retriggerUploadEvent(s3Key);
                    log.info("S3 재트리거 완료: questionSetId={}, s3Key={}", questionSetId, s3Key);
                } catch (Exception e) {
                    log.error("S3 재트리거 실패 (좀비 스케줄러가 감지 예정): questionSetId={}, s3Key={}", questionSetId, s3Key, e);
                }
            }
        });

        log.info("분석 재시도 트리거: questionSetId={}, previousStatus={}", questionSetId, previousStatus);
    }

    private QuestionSet findQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }

    private QuestionSetAnalysis findAnalysis(Long questionSetId) {
        return analysisRepository.findByQuestionSetId(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }

    private String toJson(List<String> list) {
        if (list == null) return null;
        try {
            return objectMapper.writeValueAsString(list);
        } catch (JsonProcessingException e) {
            log.warn("List<String> JSON 직렬화 실패: {}", list, e);
            return null;
        }
    }
}
