package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.orm.ObjectOptimisticLockingFailureException;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;

@Slf4j
@Component
@RequiredArgsConstructor
public class AnalysisScheduler {

    private static final int ANALYSIS_TIMEOUT_MINUTES = 10;
    private static final int CONVERTING_TIMEOUT_MINUTES = 10;
    private static final int UPLOAD_TIMEOUT_MINUTES = 30;

    private final QuestionSetRepository questionSetRepository;
    private final FileMetadataRepository fileMetadataRepository;

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectAnalysisZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(ANALYSIS_TIMEOUT_MINUTES);
        List<QuestionSet> zombies = questionSetRepository
                .findByAnalysisStatusAndUpdatedAtBefore(AnalysisStatus.ANALYZING, threshold);

        int processed = 0;
        for (QuestionSet qs : zombies) {
            try {
                qs.markFailed("ZOMBIE_TIMEOUT", "분석이 " + ANALYSIS_TIMEOUT_MINUTES + "분 내 완료되지 않음");
                questionSetRepository.saveAndFlush(qs);
                log.warn("분석 좀비 감지: questionSetId={}", qs.getId());
                processed++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("분석 좀비 처리 스킵 (동시 업데이트): questionSetId={}", qs.getId());
            } catch (Exception e) {
                log.error("분석 좀비 처리 실패: questionSetId={}", qs.getId(), e);
            }
        }

        if (processed > 0) {
            log.info("분석 좀비 처리 완료: {}건", processed);
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectFileConvertingZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CONVERTING_TIMEOUT_MINUTES);
        List<FileMetadata> zombies = fileMetadataRepository
                .findByStatusAndUpdatedAtBefore(FileStatus.CONVERTING, threshold);

        int processed = 0;
        for (FileMetadata file : zombies) {
            try {
                file.markFailed("CONVERTING_TIMEOUT", "변환이 " + CONVERTING_TIMEOUT_MINUTES + "분 내 완료되지 않음 (WebM 폴백)");
                fileMetadataRepository.saveAndFlush(file);
                log.warn("파일 변환 좀비 감지: fileMetadataId={}", file.getId());
                processed++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("파일 변환 좀비 처리 스킵 (동시 업데이트): fileMetadataId={}", file.getId());
            } catch (Exception e) {
                log.error("파일 변환 좀비 처리 실패: fileMetadataId={}", file.getId(), e);
            }
        }

        if (processed > 0) {
            log.info("파일 변환 좀비 처리 완료: {}건", processed);
        }
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void detectUploadZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(UPLOAD_TIMEOUT_MINUTES);
        List<FileMetadata> zombies = fileMetadataRepository
                .findByStatusAndUpdatedAtBefore(FileStatus.PENDING, threshold);

        int processed = 0;
        for (FileMetadata file : zombies) {
            try {
                file.markFailed("UPLOAD_TIMEOUT", "업로드가 " + UPLOAD_TIMEOUT_MINUTES + "분 내 완료되지 않음");
                fileMetadataRepository.saveAndFlush(file);
                log.warn("업로드 좀비 감지: fileMetadataId={}", file.getId());
                processed++;
            } catch (ObjectOptimisticLockingFailureException e) {
                log.info("업로드 좀비 처리 스킵 (동시 업데이트): fileMetadataId={}", file.getId());
            } catch (Exception e) {
                log.error("업로드 좀비 처리 실패: fileMetadataId={}", file.getId(), e);
            }
        }

        if (processed > 0) {
            log.info("업로드 좀비 처리 완료: {}건", processed);
        }
    }
}
