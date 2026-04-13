package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import com.rehearse.api.domain.analysis.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.analysis.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.feedback.dto.QuestionSetFeedbackResponse;
import com.rehearse.api.domain.feedback.entity.QuestionSetFeedback;
import com.rehearse.api.domain.feedback.exception.FeedbackErrorCode;
import com.rehearse.api.domain.feedback.repository.QuestionSetFeedbackRepository;
import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.question.dto.QuestionsWithAnswersResponse;
import com.rehearse.api.domain.question.dto.SaveAnswersRequest;
import com.rehearse.api.domain.question.entity.Question;
import com.rehearse.api.domain.question.entity.QuestionAnswer;
import com.rehearse.api.domain.question.repository.QuestionAnswerRepository;
import com.rehearse.api.domain.question.repository.QuestionRepository;
import com.rehearse.api.domain.questionset.dto.QuestionSetStatusResponse;
import com.rehearse.api.domain.questionset.dto.UploadUrlRequest;
import com.rehearse.api.domain.questionset.dto.UploadUrlResponse;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.exception.QuestionSetErrorCode;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import com.rehearse.api.global.exception.BusinessException;
import com.rehearse.api.infra.aws.S3KeyGenerator;
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
    private final QuestionSetAnalysisRepository analysisRepository;
    private final QuestionRepository questionRepository;
    private final QuestionAnswerRepository answerRepository;
    private final QuestionSetFeedbackRepository feedbackRepository;
    private final FileMetadataRepository fileMetadataRepository;
    private final S3Service s3Service;
    private final S3KeyGenerator s3KeyGenerator;

    @Transactional
    public void saveAnswers(Long questionSetId, SaveAnswersRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        // 빈 요청 가드 — 부분 POST 에 의해 기존 정상 데이터가 지워지는 것을 방지.
        // 프론트엔드는 유효한 answer 목록이 있을 때만 POST 하므로, 빈 요청은 비정상 상태로 간주해 no-op.
        if (request.getAnswers() == null || request.getAnswers().isEmpty()) {
            log.warn("답변 저장 요청이 비어 있음 — 기존 데이터 보호를 위해 skip: questionSetId={}", questionSetId);
            return;
        }

        // 멱등성 보장 — 같은 질문셋에 대해 재호출되어도 기존 answer 를 덮어쓴다.
        // 프론트엔드의 면접 종료 복구 루프가 경합 시 중복 POST 를 일으킬 수 있으나
        // 이 선행 삭제로 DB 에 중복 행이 쌓이지 않는다.
        answerRepository.deleteByQuestionSetId(questionSetId);
        answerRepository.flush();

        List<QuestionAnswer> answers = request.getAnswers().stream()
                .map(a -> {
                    Question question = questionRepository.findById(a.getQuestionId())
                            .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
                    return QuestionAnswer.builder()
                            .question(question)
                            .startMs(a.getStartMs())
                            .endMs(a.getEndMs())
                            .build();
                })
                .toList();

        answerRepository.saveAll(answers);

        QuestionSetAnalysis analysis = findOrCreateAnalysis(questionSet);
        analysis.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD);

        log.info("답변 구간 저장 완료: questionSetId={}, count={}", questionSetId, answers.size());
    }

    @Transactional
    public UploadUrlResponse generateUploadUrl(Long interviewId, Long questionSetId, UploadUrlRequest request) {
        QuestionSet questionSet = findQuestionSet(questionSetId);

        String s3Key = s3KeyGenerator.generateRawVideoKey(interviewId, questionSetId);

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
                .orElseThrow(() -> new BusinessException(FeedbackErrorCode.FEEDBACK_NOT_FOUND));

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
        List<QuestionAnswer> answers = answerRepository.findByQuestionSetIdWithQuestion(questionSetId);

        return QuestionsWithAnswersResponse.from(questions, answers);
    }

    @Transactional
    public void skipRemaining(Long interviewId) {
        List<QuestionSet> questionSets = questionSetRepository.findByInterviewIdOrderByOrderIndex(interviewId);
        int count = 0;
        for (QuestionSet qs : questionSets) {
            QuestionSetAnalysis analysis = qs.getAnalysis();
            if (analysis == null) {
                analysis = analysisRepository.save(QuestionSetAnalysis.builder().questionSet(qs).build());
            }
            if (analysis.trySkip()) {
                count++;
            }
        }

        log.info("미응답 질문세트 SKIPPED 처리: interviewId={}, count={}", interviewId, count);
    }

    private QuestionSet findQuestionSet(Long questionSetId) {
        return questionSetRepository.findById(questionSetId)
                .orElseThrow(() -> new BusinessException(QuestionSetErrorCode.NOT_FOUND));
    }

    private QuestionSetAnalysis findOrCreateAnalysis(QuestionSet questionSet) {
        return analysisRepository.findByQuestionSetId(questionSet.getId())
                .orElseGet(() -> {
                    QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                            .questionSet(questionSet)
                            .build();
                    return analysisRepository.save(analysis);
                });
    }
}
