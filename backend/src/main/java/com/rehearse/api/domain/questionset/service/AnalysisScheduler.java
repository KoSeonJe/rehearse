package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.ConvertStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.questionset.repository.QuestionSetAnalysisRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private static final int ANALYSIS_TIMEOUT_MINUTES = 10;
    private static final int CONVERT_TIMEOUT_MINUTES = 10;
    private static final int UPLOAD_TIMEOUT_MINUTES = 30;

    private final QuestionSetAnalysisRepository analysisRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectAnalysisZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ANALYSIS_TIMEOUT_MINUTES);
        List<QuestionSetAnalysis> zombies = analysisRepository
                .findByAnalysisStatusInAndUpdatedAtBefore(AnalysisStatus.inProgressStatuses(), threshold);

        processZombies(zombies, analysis -> {
            analysis.markFailed("ZOMBIE_TIMEOUT", "분석이 " + ANALYSIS_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            analysisRepository.saveAndFlush(analysis);
        }, a -> a.getQuestionSet().getId(), "분석");
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectConvertZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CONVERT_TIMEOUT_MINUTES);
        List<QuestionSetAnalysis> zombies = analysisRepository
                .findByConvertStatusAndUpdatedAtBefore(ConvertStatus.PROCESSING, threshold);

        processZombies(zombies, analysis -> {
            analysis.markConvertFailed("CONVERT_TIMEOUT: 변환이 " + CONVERT_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            analysisRepository.saveAndFlush(analysis);
        }, a -> a.getQuestionSet().getId(), "변환");
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void detectPendingUploadZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(UPLOAD_TIMEOUT_MINUTES);
        List<QuestionSetAnalysis> zombies = analysisRepository
                .findByAnalysisStatusAndUpdatedAtBefore(AnalysisStatus.PENDING_UPLOAD, threshold);

        processZombies(zombies, analysis -> {
            analysis.markFailed("UPLOAD_PENDING_TIMEOUT", "업로드 대기가 " + UPLOAD_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            analysisRepository.saveAndFlush(analysis);
        }, a -> a.getQuestionSet().getId(), "업로드 대기");
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void detectUploadZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(UPLOAD_TIMEOUT_MINUTES);
        List<FileMetadata> zombies = fileMetadataRepository
                .findByStatusAndUpdatedAtBefore(FileStatus.PENDING, threshold);

        processZombies(zombies, file -> {
            file.markFailed("UPLOAD_TIMEOUT", "업로드가 " + UPLOAD_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            fileMetadataRepository.saveAndFlush(file);
        }, FileMetadata::getId, "업로드");
    }

    private <T> void processZombies(List<T> zombies, Consumer<T> failAction,
                                     Function<T, Object> idExtractor, String label) {
        int processed = 0;
        for (T entity : zombies) {
            try {
                failAction.accept(entity);
                log.warn("{} 좀비 감지: id={}", label, idExtractor.apply(entity));
                processed++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("{} 좀비 처리 스킵 (동시 업데이트): id={}", label, idExtractor.apply(entity));
            } catch (Exception e) {
                log.error("{} 좀비 처리 실패: id={}", label, idExtractor.apply(entity), e);
            }
        }
        if (processed > 0) {
            log.info("{} 좀비 처리 완료: {}건", label, processed);
        }
    }
}
