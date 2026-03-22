package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.questionset.dto.SaveFeedbackRequest;
import com.rehearse.api.domain.questionset.dto.UpdateProgressRequest;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.*;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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
    private final QuestionAnswerRepository answerRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;
    private final QuestionRepository questionRepository;
    private final S3Service s3Service;

    @Transactional
    public void updateProgress(Long questionSetId, UpdateProgressRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        if (request.getProgress() == AnalysisProgress.FAILED) {
            questionSet.markFailed(request.getFailureReason(), request.getFailureDetail());
            log.warn("분석 실패: questionSetId={}, reason={}", questionSetId, request.getFailureReason());
            return;
        }

        if (questionSet.getAnalysisStatus() != AnalysisStatus.ANALYZING) {
            questionSet.updateAnalysisStatus(AnalysisStatus.ANALYZING);
        }
        questionSet.updateAnalysisProgress(request.getProgress());

        log.info("분석 진행 상태 업데이트: questionSetId={}, progress={}", questionSetId, request.getProgress());
    }

    public QuestionSet getQuestionSet(Long questionSetId) {
        return findQuestionSet(questionSetId);
    }

    public List<QuestionAnswer> getAnswers(Long questionSetId) {
        return answerRepository.findByQuestionSetIdWithQuestion(questionSetId);
    }

    @Transactional
    public void saveFeedback(Long questionSetId, SaveFeedbackRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        QuestionSetFeedback feedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetScore(request.getQuestionSetScore())
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
                        .verbalScore(item.getVerbalScore())
                        .verbalComment(item.getVerbalComment())
                        .fillerWordCount(item.getFillerWordCount())
                        .eyeContactScore(item.getEyeContactScore())
                        .postureScore(item.getPostureScore())
                        .expressionLabel(item.getExpressionLabel())
                        .nonverbalComment(item.getNonverbalComment())
                        .overallComment(item.getOverallComment())
                        .isAnalyzed(true)
                        .build();
                feedback.addTimestampFeedback(tf);
            }
        }

        feedbackRepository.save(feedback);
        questionSet.updateAnalysisStatus(AnalysisStatus.COMPLETED);
        questionSet.updateAnalysisProgress(AnalysisProgress.FINALIZING);

        log.info("분석 결과 저장 완료: questionSetId={}, score={}", questionSetId, request.getQuestionSetScore());
    }

    @Transactional
    public void retryAnalysis(Long questionSetId) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        FileMetadata file = questionSet.getFileMetadata();
        if (file == null) {
            throw new BusinessException(QuestionSetErrorCode.FILE_NOT_FOUND);
        }

        boolean analysisNeedsRetry = questionSet.getAnalysisStatus() == AnalysisStatus.FAILED;
        boolean fileNeedsRetry = file.getStatus() == FileStatus.FAILED;

        if (!analysisNeedsRetry && !fileNeedsRetry) {
            throw new BusinessException(QuestionSetErrorCode.INVALID_ANALYSIS_STATUS_TRANSITION);
        }

        if (analysisNeedsRetry) {
            questionSet.updateAnalysisStatus(AnalysisStatus.ANALYZING);
            questionSet.updateAnalysisProgress(AnalysisProgress.STARTED);
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

        log.info("피드백 생성 재시도 트리거: questionSetId={}, analysisRetry={}, fileRetry={}", questionSetId, analysisNeedsRetry, fileNeedsRetry);
    }

    private QuestionSet findQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }
}
