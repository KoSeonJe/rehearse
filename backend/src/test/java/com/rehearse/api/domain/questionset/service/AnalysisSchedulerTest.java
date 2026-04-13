package com.rehearse.api.domain.questionset.service;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.domain.interview.entity.InterviewLevel;
import com.rehearse.api.domain.interview.entity.Position;
import com.rehearse.api.domain.analysis.entity.AnalysisStatus;
import com.rehearse.api.domain.analysis.entity.ConvertStatus;
import com.rehearse.api.domain.analysis.entity.QuestionSetAnalysis;
import com.rehearse.api.domain.analysis.repository.QuestionSetAnalysisRepository;
import com.rehearse.api.domain.analysis.service.AnalysisScheduler;
import com.rehearse.api.domain.questionset.entity.QuestionSet;
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;
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
    private QuestionSetAnalysisRepository analysisRepository;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    // ─────────────────────────────────────────────────────────────
    // detectAnalysisZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectAnalysisZombies: ANALYZING 상태로 타임아웃된 QuestionSetAnalysis를 FAILED로 마킹한다")
    void detectAnalysisZombies_좀비감지시_FAILED로마킹() {
        // given
        QuestionSetAnalysis zombie = createAnalysisInStatus(AnalysisStatus.ANALYZING);

        given(analysisRepository.findByAnalysisStatusInAndUpdatedAtBefore(
                eq(AnalysisStatus.inProgressStatuses()), any()))
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
        given(analysisRepository.findByAnalysisStatusInAndUpdatedAtBefore(
                eq(AnalysisStatus.inProgressStatuses()), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectAnalysisZombies();

        // then
        then(analysisRepository).should(never()).saveAndFlush(any());
    }

    // ─────────────────────────────────────────────────────────────
    // detectPendingUploadZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectPendingUploadZombies: PENDING_UPLOAD 상태로 타임아웃된 QuestionSetAnalysis를 FAILED로 마킹한다")
    void detectPendingUploadZombies_좀비감지시_FAILED로마킹() {
        // given
        QuestionSetAnalysis zombie = createAnalysisInStatus(AnalysisStatus.PENDING_UPLOAD);

        given(analysisRepository.findByAnalysisStatusAndUpdatedAtBefore(
                eq(AnalysisStatus.PENDING_UPLOAD), any()))
                .willReturn(List.of(zombie));

        // when
        analysisScheduler.detectPendingUploadZombies();

        // then
        assertThat(zombie.getAnalysisStatus()).isEqualTo(AnalysisStatus.FAILED);
        assertThat(zombie.getFailureReason()).isEqualTo("UPLOAD_PENDING_TIMEOUT");
        assertThat(zombie.getFailureDetail()).contains("30분");
    }

    @Test
    @DisplayName("detectPendingUploadZombies: 좀비가 없을 때 아무 상태 변경도 일어나지 않는다")
    void detectPendingUploadZombies_좀비없을때_아무동작없음() {
        // given
        given(analysisRepository.findByAnalysisStatusAndUpdatedAtBefore(
                eq(AnalysisStatus.PENDING_UPLOAD), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectPendingUploadZombies();

        // then
        then(analysisRepository).should(never()).save(any());
    }

    // ─────────────────────────────────────────────────────────────
    // detectConvertZombies
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("detectConvertZombies: PROCESSING 상태로 타임아웃된 QuestionSetAnalysis를 ConvertStatus.FAILED로 마킹한다")
    void detectConvertZombies_좀비감지시_ConvertFAILED로마킹() {
        // given
        QuestionSetAnalysis zombie = createAnalysisInStatus(AnalysisStatus.PENDING);
        ReflectionTestUtils.setField(zombie, "convertStatus", ConvertStatus.PROCESSING);

        given(analysisRepository.findByConvertStatusAndUpdatedAtBefore(
                eq(ConvertStatus.PROCESSING), any()))
                .willReturn(List.of(zombie));

        // when
        analysisScheduler.detectConvertZombies();

        // then
        assertThat(zombie.getConvertStatus()).isEqualTo(ConvertStatus.FAILED);
        assertThat(zombie.getConvertFailureReason()).contains("CONVERT_TIMEOUT");
        assertThat(zombie.getConvertFailureReason()).contains("10분");
    }

    @Test
    @DisplayName("detectConvertZombies: 좀비가 없을 때 아무 상태 변경도 일어나지 않는다")
    void detectConvertZombies_좀비없을때_아무동작없음() {
        // given
        given(analysisRepository.findByConvertStatusAndUpdatedAtBefore(
                eq(ConvertStatus.PROCESSING), any()))
                .willReturn(List.of());

        // when
        analysisScheduler.detectConvertZombies();

        // then
        then(analysisRepository).should(never()).save(any());
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

    private QuestionSetAnalysis createAnalysisInStatus(AnalysisStatus targetStatus) {
        Interview interview = Interview.builder()
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .durationMinutes(30)
                .build();

        QuestionSet qs = QuestionSet.builder()
                .interview(interview)
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
                .orderIndex(1)
                .build();

        QuestionSetAnalysis analysis = QuestionSetAnalysis.builder()
                .questionSet(qs)
                .build();

        if (targetStatus != AnalysisStatus.PENDING) {
            ReflectionTestUtils.setField(analysis, "analysisStatus", targetStatus);
        }
        return analysis;
    }

    private FileMetadata createFileMetadataInStatus(FileStatus targetStatus) {
        FileMetadata file = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key("test/video.webm")
                .bucket("rehearse-bucket")
                .contentType("video/webm")
                .build();
        // PENDING 은 초기 상태이므로 추가 전이 불필요
        return file;
    }
}
