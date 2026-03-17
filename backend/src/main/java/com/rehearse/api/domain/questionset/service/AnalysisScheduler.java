package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
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

        for (QuestionSet qs : zombies) {
            qs.markFailed("ZOMBIE_TIMEOUT", "분석이 " + ANALYSIS_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            log.warn("분석 좀비 감지: questionSetId={}", qs.getId());
        }

        if (!zombies.isEmpty()) {
            log.info("분석 좀비 처리 완료: {}건", zombies.size());
        }
    }

    @Scheduled(fixedDelay = 60_000)
    @Transactional
    public void detectFileConvertingZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(CONVERTING_TIMEOUT_MINUTES);
        List<FileMetadata> zombies = fileMetadataRepository
                .findByStatusAndUpdatedAtBefore(FileStatus.CONVERTING, threshold);

        for (FileMetadata file : zombies) {
            file.markFailed("CONVERTING_TIMEOUT", "변환이 " + CONVERTING_TIMEOUT_MINUTES + "분 내 완료되지 않음 (WebM 폴백)");
            log.warn("파일 변환 좀비 감지: fileMetadataId={}", file.getId());
        }

        if (!zombies.isEmpty()) {
            log.info("파일 변환 좀비 처리 완료: {}건", zombies.size());
        }
    }

    @Scheduled(fixedDelay = 300_000)
    @Transactional
    public void detectUploadZombies() {
        LocalDateTime threshold = LocalDateTime.now().minusMinutes(UPLOAD_TIMEOUT_MINUTES);
        List<FileMetadata> zombies = fileMetadataRepository
                .findByStatusAndUpdatedAtBefore(FileStatus.PENDING, threshold);

        for (FileMetadata file : zombies) {
            file.markFailed("UPLOAD_TIMEOUT", "업로드가 " + UPLOAD_TIMEOUT_MINUTES + "분 내 완료되지 않음");
            log.warn("업로드 좀비 감지: fileMetadataId={}", file.getId());
        }

        if (!zombies.isEmpty()) {
            log.info("업로드 좀비 처리 완료: {}건", zombies.size());
        }
    }
}
