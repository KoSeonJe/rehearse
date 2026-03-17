package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.questionset.dto.*;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.*;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3Service;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class QuestionSetService {

    private final QuestionSetRepository questionSetRepository;
    private final QuestionRepository questionRepository;
    private final QuestionSetAnswerRepository answerRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final S3Service s3Service;

    @Transactional
    public void saveAnswers(Long questionSetId, SaveAnswersRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        List<QuestionSetAnswer> answers = request.getAnswers().stream()
                .map(a -> {
                    Question question = questionRepository.findById(a.getQuestionId())
                            .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
                    return QuestionSetAnswer.builder()
                            .question(question)
                            .startMs(a.getStartMs())
                            .endMs(a.getEndMs())
                            .build();
                })
                .toList();

        answerRepository.saveAll(answers);
        questionSet.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD);

        log.info("답변 구간 저장 완료: questionSetId={}, count={}", questionSetId, answers.size());
    }

    @Transactional
    public UploadUrlResponse generateUploadUrl(Long interviewId, Long questionSetId, UploadUrlRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        String s3Key = String.format("videos/%d/qs_%d.webm", interviewId, questionSetId);

        FileMetadata fileMetadata = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key(s3Key)
                .bucket(s3Service.getBucket())
                .contentType(request.getContentType())
                .build();

        fileMetadataRepository.save(fileMetadata);
        questionSet.assignFileMetadata(fileMetadata);

        String uploadUrl = s3Service.generatePutPresignedUrl(s3Key, request.getContentType());

        log.info("Presigned URL 생성: questionSetId={}, s3Key={}", questionSetId, s3Key);

        return UploadUrlResponse.builder()
                .uploadUrl(uploadUrl)
                .s3Key(s3Key)
                .fileMetadataId(fileMetadata.getId())
                .build();
    }

    public QuestionSetStatusResponse getStatus(Long questionSetId) {
        QuestionSet questionSet = questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
        return QuestionSetStatusResponse.from(questionSet);
    }

    public QuestionSetFeedbackResponse getFeedback(Long questionSetId) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        QuestionSetFeedback feedback = feedbackRepository.findByQuestionSetIdWithTimestampFeedbacks(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.FEEDBACK_NOT_FOUND));

        String streamingUrl = null;
        String fallbackUrl = null;

        FileMetadata file = questionSet.getFileMetadata();
        if (file != null) {
            if (file.getStreamingS3Key() != null) {
                streamingUrl = s3Service.generateGetPresignedUrl(file.getStreamingS3Key());
            }
            fallbackUrl = s3Service.generateGetPresignedUrl(file.getS3Key());
        }

        return QuestionSetFeedbackResponse.from(feedback, streamingUrl, fallbackUrl);
    }

    public QuestionsWithAnswersResponse getQuestionsWithAnswers(Long questionSetId) {
        List<Question> questions = questionRepository.findByQuestionSetIdOrderByOrderIndex(questionSetId);
        List<QuestionSetAnswer> answers = answerRepository.findByQuestionSetIdWithQuestion(questionSetId);

        return QuestionsWithAnswersResponse.from(questions, answers);
    }

    private QuestionSet findQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }
}
