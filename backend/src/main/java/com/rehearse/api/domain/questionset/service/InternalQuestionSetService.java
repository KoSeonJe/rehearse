package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.analysis.dto.UpdateConvertStatusRequest;
import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import com.rehearse.api.domain.analysis.entity.ConvertStatus;
import com.rehearse.api.domain.analysis.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.analysis.exception.AnalysisErrorCode;
import com.rehearse.api.domain.analysis.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.feedback.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.entity.TimestampFeedback;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.feedback.service.TimestampFeedbackMapper;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.service.InterviewFinder;
import com.rehearse.api.domain.question.dto.AnswerResponse;
import com.rehearse.api.domain.question.dto.AnswersResponse;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.repository.QuestionAnswerRepository;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
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
    private final TimestampFeedbackMapper timestampFeedbackMapper;

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
            throw new BusinessException(AnalysisErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
        }
        log.info("분석 상태 업데이트: questionSetId={}, status={}", questionSetId, request.getStatus());
    }

    public QuestionSet getQuestionSet(Long questionSetId) {
        return findQuestionSet(questionSetId);
    }

    public List<QuestionSet> getQuestionSetsByInterview(Long interviewId) {
        return questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
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

                TimestampFeedback tf = timestampFeedbackMapper.toEntity(item, question);
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
                throw new BusinessException(AnalysisErrorCode.INVALID_CONVERT_STATUS_TRANSITION);
            }
        }

        if (request.getStreamingS3Key() != null) {
            analysis.getQuestionSet().updateStreamingS3Key(request.getStreamingS3Key());
        }

        log.info("변환 상태 업데이트: questionSetId={}, status={}", questionSetId, request.getStatus());
    }

    @Retryable(retryFor = ObjectOptimisticLockingFailureException.class, maxAttempts = 3, backoff = @Backoff(delay = 100))
    @Transactional
    public void retryAnalysis(Long questionSetId) {
        QuestionSetAnalysis analysis = findAnalysis(questionSetId);

        if (!analysis.getAnalysisStatus().isRetryable()) {
            throw new BusinessException(AnalysisErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
        }

        AnalysisStatus previousStatus = analysis.getAnalysisStatus();
        analysis.retry();

        QuestionSet questionSet = analysis.getQuestionSet();
        String s3Key = questionSet.getFileS3Key();
        if (s3Key == null) {
            throw new BusinessException(QuestionSetErrorCode.FILE_NOT_FOUND);
        }
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

}
