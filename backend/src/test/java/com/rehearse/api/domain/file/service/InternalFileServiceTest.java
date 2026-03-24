package com.rehearse.api.domain.file.service;

import com.rehearse.api.domain.file.dto.UpdateFileStatusRequest;
import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileStatus;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.file.exception.FileErrorCode;
import com.rehearse.api.domain.file.repository.FileMetadataRepository;
import com.rehearse.api.global.exception.BusinessException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.test.util.ReflectionTestUtils;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;

@ExtendWith(MockitoExtension.class)
class InternalFileServiceTest {

    @InjectMocks
    private InternalFileService internalFileService;

    @Mock
    private FileMetadataRepository fileMetadataRepository;

    // ─────────────────────────────────────────────────────────────
    // updateFileStatus
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("updateFileStatus: 실패 사유 없이 요청하면 updateStatus로 상태를 변경한다")
    void updateFileStatus_상태업데이트성공() {
        // given
        FileMetadata file = createPendingFile(1L);
        UpdateFileStatusRequest request = createRequest(FileStatus.UPLOADED, null, null, null, null);

        given(fileMetadataRepository.findById(1L)).willReturn(Optional.of(file));

        // when
        internalFileService.updateFileStatus(1L, request);

        // then
        assertThat(file.getStatus()).isEqualTo(FileStatus.UPLOADED);
    }

    @Test
    @DisplayName("updateFileStatus: failureReason이 있으면 markFailed를 호출한다")
    void updateFileStatus_실패사유포함시_markFailed() {
        // given
        FileMetadata file = createPendingFile(2L);
        UpdateFileStatusRequest request = createRequest(
                FileStatus.FAILED, null, null, "UPLOAD_TIMEOUT", "30분 초과");

        given(fileMetadataRepository.findById(2L)).willReturn(Optional.of(file));

        // when
        internalFileService.updateFileStatus(2L, request);

        // then
        assertThat(file.getStatus()).isEqualTo(FileStatus.FAILED);
        assertThat(file.getFailureReason()).isEqualTo("UPLOAD_TIMEOUT");
        assertThat(file.getFailureDetail()).isEqualTo("30분 초과");
    }

    @Test
    @DisplayName("updateFileStatus: streamingS3Key가 있으면 updateStreamingS3Key를 호출한다")
    void updateFileStatus_streamingS3Key업데이트() {
        // given
        FileMetadata file = createPendingFile(3L);
        UpdateFileStatusRequest request = createRequest(
                FileStatus.UPLOADED, "streaming/video.mp4", null, null, null);

        given(fileMetadataRepository.findById(3L)).willReturn(Optional.of(file));

        // when
        internalFileService.updateFileStatus(3L, request);

        // then
        assertThat(file.getStatus()).isEqualTo(FileStatus.UPLOADED);
        assertThat(file.getStreamingS3Key()).isEqualTo("streaming/video.mp4");
    }

    @Test
    @DisplayName("updateFileStatus: fileSizeBytes가 있으면 updateFileSizeBytes를 호출한다")
    void updateFileStatus_fileSizeBytes업데이트() {
        // given
        FileMetadata file = createPendingFile(4L);
        UpdateFileStatusRequest request = createRequest(
                FileStatus.UPLOADED, null, 1024L * 1024L * 50L, null, null);

        given(fileMetadataRepository.findById(4L)).willReturn(Optional.of(file));

        // when
        internalFileService.updateFileStatus(4L, request);

        // then
        assertThat(file.getFileSizeBytes()).isEqualTo(1024L * 1024L * 50L);
    }

    @Test
    @DisplayName("updateFileStatus: 존재하지 않는 파일 ID이면 FILE_001 예외를 던진다")
    void updateFileStatus_미존재파일_예외() {
        // given
        UpdateFileStatusRequest request = createRequest(FileStatus.UPLOADED, null, null, null, null);
        given(fileMetadataRepository.findById(999L)).willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalFileService.updateFileStatus(999L, request))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(FileErrorCode.NOT_FOUND.getCode());
                });
    }

    // ─────────────────────────────────────────────────────────────
    // findByS3Key
    // ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("findByS3Key: S3 키로 파일을 조회한다")
    void findByS3Key_성공() {
        // given
        FileMetadata file = createPendingFile(5L);
        given(fileMetadataRepository.findByS3Key("uploads/video.webm"))
                .willReturn(Optional.of(file));

        // when
        FileMetadata result = internalFileService.findByS3Key("uploads/video.webm");

        // then
        assertThat(result).isEqualTo(file);
    }

    @Test
    @DisplayName("findByS3Key: 존재하지 않는 S3 키이면 FILE_003 예외를 던진다")
    void findByS3Key_미존재_S3KEY_NOT_FOUND예외() {
        // given
        given(fileMetadataRepository.findByS3Key("no/such/key.webm"))
                .willReturn(Optional.empty());

        // when & then
        assertThatThrownBy(() -> internalFileService.findByS3Key("no/such/key.webm"))
                .isInstanceOf(BusinessException.class)
                .satisfies(ex -> {
                    BusinessException be = (BusinessException) ex;
                    assertThat(be.getCode()).isEqualTo(FileErrorCode.S3_KEY_NOT_FOUND.getCode());
                });
    }

    // ─────────────────────────────────────────────────────────────
    // 헬퍼
    // ─────────────────────────────────────────────────────────────

    private FileMetadata createPendingFile(Long id) {
        FileMetadata file = FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key("uploads/video.webm")
                .bucket("rehearse-bucket")
                .contentType("video/webm")
                .build();
        ReflectionTestUtils.setField(file, "id", id);
        return file;
    }

    private UpdateFileStatusRequest createRequest(
            FileStatus status,
            String streamingS3Key,
            Long fileSizeBytes,
            String failureReason,
            String failureDetail) {

        UpdateFileStatusRequest request = new UpdateFileStatusRequest();
        ReflectionTestUtils.setField(request, "status", status);
        ReflectionTestUtils.setField(request, "streamingS3Key", streamingS3Key);
        ReflectionTestUtils.setField(request, "fileSizeBytes", fileSizeBytes);
        ReflectionTestUtils.setField(request, "failureReason", failureReason);
        ReflectionTestUtils.setField(request, "failureDetail", failureDetail);
        return request;
    }
}
