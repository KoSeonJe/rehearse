package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.questionset.entity.AnalysisStatus;
import com.rehearse.api.domain.questionset.entity.QuestionCategory;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.repository.QuestionSetRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.Mockito.never;

@ExtendWith(MockitoExtension.class)
class AnalysisSchedulerTest {

    @InjectMocks
    private AnalysisScheduler analysisScheduler;

    @Mock
    private QuestionSetRepository questionSetRepository;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    // ─────────────────────────────────────────────────────────────
    // detectAnalysisZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectAnalysisZombies: ANALYZING 상태로 타임아웃된 QuestionSet을 FAILED로 마킹한다")
    void detectAnalysisZombies_좀비감지시_FAILED로마킹() {
        // given
        QuestionSet zombie = createAnalyzingQuestionSet();

        given(questionSetRepository.findByAnalysisStatusAndUpdatedAtBefore(
                eq(AnalysisStatus.ANALYZING), any()))
                .willReturn(List.of(zombie));

        // when
        analysisScheduler.detectAnalysisZombies();

        // then
        assertThat(zombie.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(zombie.getFailureReason()).isEqualTo("ZOMBIE_TIMEOUT");
        assertThat(zombie.getFailureDetail()).contains("10분");
    }

    @Test
    @DisplayName("detectAnalysisZombies: 좀비가 없을 때 아무 상태 변경도 일어나지 않는다")
    void detectAnalysisZombies_좀비없을때_아무동작없음() {
        // given
        given(questionSetRepository.findByAnalysisStatusAndUpdatedAtBefore(
                eq(AnalysisStatus.ANALYZING), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectAnalysisZombies();

        // then
        then(questionSetRepository).should(never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // detectFileConvertingZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectFileConvertingZombies: CONVERTING 상태로 타임아웃된 FileMetadata를 FAILED로 마킹한다")
    void detectFileConvertingZombies_좀비감지시_FAILED로마킹() {
        // given
        FileMetadata zombie = createFileMetadataInStatus(FileStatus.CONVERTING);

        given(fileMetadataRepository.findByStatusAndUpdatedAtBefore(
                eq(FileStatus.CONVERTING), any()))
                .willReturn(List.of(zombie));

        // when
        analysisScheduler.detectFileConvertingZombies();

        // then
        assertThat(zombie.getStatus()).isEqualTo(FileStatus.FAILED);
        assertThat(zombie.getFailureReason()).isEqualTo("CONVERTING_TIMEOUT");
        assertThat(zombie.getFailureDetail()).contains("10분");
    }

    @Test
    @DisplayName("detectFileConvertingZombies: 좀비가 없을 때 아무 상태 변경도 일어나지 않는다")
    void detectFileConvertingZombies_좀비없을때_아무동작없음() {
        // given
        given(fileMetadataRepository.findByStatusAndUpdatedAtBefore(
                eq(FileStatus.CONVERTING), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectFileConvertingZombies();

        // then
        then(fileMetadataRepository).should(never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // detectUploadZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectUploadZombies: PENDING 상태로 타임아웃된 FileMetadata를 FAILED로 마킹한다")
    void detectUploadZombies_좀비감지시_FAILED로마킹() {
        // given
        FileMetadata zombie = createFileMetadataInStatus(FileStatus.PENDING);

        given(fileMetadataRepository.findByStatusAndUpdatedAtBefore(
                eq(FileStatus.PENDING), any()))
                .willReturn(List.of(zombie));

        // when
        analysisScheduler.detectUploadZombies();

        // then
        assertThat(zombie.getStatus()).isEqualTo(FileStatus.FAILED);
        assertThat(zombie.getFailureReason()).isEqualTo("UPLOAD_TIMEOUT");
        assertThat(zombie.getFailureDetail()).contains("30분");
    }

    @Test
    @DisplayName("detectUploadZombies: 좀비가 없을 때 아무 상태 변경도 일어나지 않는다")
    void detectUploadZombies_좀비없을때_아무동작없음() {
        // given
        given(fileMetadataRepository.findByStatusAndUpdatedAtBefore(
                eq(FileStatus.PENDING), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectUploadZombies();

        // then
        then(fileMetadataRepository).should(never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private QuestionSet createAnalyzingQuestionSet() {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionCategory.CS)
                .orderIndex(1)
                .build();
        // PENDING -> PENDING_UPLOAD -> ANALYZING
        qs.updateAnalysisStatus(AnalysisStatus.PENDING_UPLOAD);
        qs.updateAnalysisStatus(AnalysisStatus.ANALYZING);
        return qs;
    }

    private FileMetadata createFileMetadataInStatus(FileStatus targetStatus) {
        FileMetadata file = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key("test/video.webm")
                .bucket("rehearse-bucket")
                .contentType("video/webm")
                .build();

        // PENDING 이 기본값 — CONVERTING 이 필요하면 단계별 전이
        if (targetStatus == FileStatus.CONVERTING) {
            file.updateStatus(FileStatus.UPLOADED);
            file.updateStatus(FileStatus.CONVERTING);
        }
        // PENDING 은 초기 상태이므로 추가 전이 불필요
        return file;
    }
}
