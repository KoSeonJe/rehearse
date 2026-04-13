package com.rehearse.api.domain.file.entity;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DisplayName("FileMetadata - 파일 메타데이터 엔티티")
class FileMetadataTest {

    private FileMetadata createPendingFileMetadata() {
        return FileMetadata.builder()
                .fileType(FileType.VIDEO)
                .s3Key("videos/test-video.webm")
                .bucket("rehearse-videos-dev")
                .contentType("video/webm")
                .build();
    }

    @Nested
    @DisplayName("updateStatus 메서드")
    class UpdateStatus {

        @Test
        @DisplayName("PENDING에서 UPLOADED로 전이할 수 있다")
        void updateStatus_pendingToUploaded_succeeds() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.UPLOADED);
        }

        @Test
        @DisplayName("PENDING에서 FAILED로 전이할 수 있다")
        void updateStatus_pendingToFailed_succeeds() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when
            fileMetadata.updateStatus(FileStatus.FAILED);

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.FAILED);
        }

        @Test
        @DisplayName("UPLOADED에서 FAILED로 전이할 수 있다")
        void updateStatus_uploadedToFailed_succeeds() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // when
            fileMetadata.updateStatus(FileStatus.FAILED);

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.FAILED);
        }

        @Test
        @DisplayName("FAILED에서 UPLOADED로 전이할 수 있다")
        void updateStatus_failedToUploaded_succeeds() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.FAILED);

            // when
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.UPLOADED);
        }

        @Test
        @DisplayName("PENDING에서 PENDING으로 전이하면 예외가 발생한다")
        void updateStatus_pendingToPending_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when & then
            assertThatThrownBy(() -> fileMetadata.updateStatus(FileStatus.PENDING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("PENDING")
                    .hasMessageContaining("변경할 수 없습니다");
        }

        @Test
        @DisplayName("UPLOADED에서 PENDING으로 전이하면 예외가 발생한다")
        void updateStatus_uploadedToPending_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // when & then
            assertThatThrownBy(() -> fileMetadata.updateStatus(FileStatus.PENDING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("UPLOADED")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("UPLOADED에서 UPLOADED로 전이하면 예외가 발생한다")
        void updateStatus_uploadedToUploaded_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // when & then
            assertThatThrownBy(() -> fileMetadata.updateStatus(FileStatus.UPLOADED))
                    .isInstanceOf(IllegalStateException.class);
        }

        @Test
        @DisplayName("FAILED에서 PENDING으로 전이하면 예외가 발생한다")
        void updateStatus_failedToPending_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.FAILED);

            // when & then
            assertThatThrownBy(() -> fileMetadata.updateStatus(FileStatus.PENDING))
                    .isInstanceOf(IllegalStateException.class)
                    .hasMessageContaining("FAILED")
                    .hasMessageContaining("PENDING");
        }

        @Test
        @DisplayName("FAILED에서 FAILED로 전이하면 예외가 발생한다")
        void updateStatus_failedToFailed_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.FAILED);

            // when & then
            assertThatThrownBy(() -> fileMetadata.updateStatus(FileStatus.FAILED))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("markFailed 메서드")
    class MarkFailed {

        @Test
        @DisplayName("상태를 FAILED로 변경하고 실패 사유를 설정한다")
        void markFailed_fromPending_setsStatusAndReason() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when
            fileMetadata.markFailed("업로드 실패", "S3 연결 시간 초과");

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.FAILED);
            assertThat(fileMetadata.getFailureReason()).isEqualTo("업로드 실패");
            assertThat(fileMetadata.getFailureDetail()).isEqualTo("S3 연결 시간 초과");
        }

        @Test
        @DisplayName("UPLOADED 상태에서도 FAILED로 전이하고 실패 사유를 설정한다")
        void markFailed_fromUploaded_setsStatusAndReason() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStatus(FileStatus.UPLOADED);

            // when
            fileMetadata.markFailed("변환 실패", "지원하지 않는 코덱");

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.FAILED);
            assertThat(fileMetadata.getFailureReason()).isEqualTo("변환 실패");
            assertThat(fileMetadata.getFailureDetail()).isEqualTo("지원하지 않는 코덱");
        }

        @Test
        @DisplayName("이미 FAILED 상태에서 호출하면 예외가 발생한다")
        void markFailed_fromFailed_throwsException() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.markFailed("첫 번째 실패", "상세 내용");

            // when & then
            assertThatThrownBy(() -> fileMetadata.markFailed("두 번째 실패", "다른 상세 내용"))
                    .isInstanceOf(IllegalStateException.class);
        }
    }

    @Nested
    @DisplayName("updateStreamingS3Key 메서드")
    class UpdateStreamingS3Key {

        @Test
        @DisplayName("스트리밍 S3 키를 설정한다")
        void updateStreamingS3Key_setsStreamingKey() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when
            fileMetadata.updateStreamingS3Key("streaming/test-video.m3u8");

            // then
            assertThat(fileMetadata.getStreamingS3Key()).isEqualTo("streaming/test-video.m3u8");
        }

        @Test
        @DisplayName("null로 스트리밍 S3 키를 초기화할 수 있다")
        void updateStreamingS3Key_withNull_clearsStreamingKey() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateStreamingS3Key("streaming/test-video.m3u8");

            // when
            fileMetadata.updateStreamingS3Key(null);

            // then
            assertThat(fileMetadata.getStreamingS3Key()).isNull();
        }
    }

    @Nested
    @DisplayName("updateFileSizeBytes 메서드")
    class UpdateFileSizeBytes {

        @Test
        @DisplayName("파일 크기를 설정한다")
        void updateFileSizeBytes_setsFileSize() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();

            // when
            fileMetadata.updateFileSizeBytes(1024L * 1024L * 50L);

            // then
            assertThat(fileMetadata.getFileSizeBytes()).isEqualTo(52_428_800L);
        }

        @Test
        @DisplayName("null로 파일 크기를 초기화할 수 있다")
        void updateFileSizeBytes_withNull_clearsFileSize() {
            // given
            FileMetadata fileMetadata = createPendingFileMetadata();
            fileMetadata.updateFileSizeBytes(1024L);

            // when
            fileMetadata.updateFileSizeBytes(null);

            // then
            assertThat(fileMetadata.getFileSizeBytes()).isNull();
        }
    }

    @Nested
    @DisplayName("엔티티 생성")
    class Creation {

        @Test
        @DisplayName("생성 직후 상태는 PENDING이다")
        void creation_initialStatus_isPending() {
            // given & when
            FileMetadata fileMetadata = createPendingFileMetadata();

            // then
            assertThat(fileMetadata.getStatus()).isEqualTo(FileStatus.PENDING);
        }

        @Test
        @DisplayName("생성 직후 스트리밍 S3 키는 null이다")
        void creation_streamingS3Key_isNull() {
            // given & when
            FileMetadata fileMetadata = createPendingFileMetadata();

            // then
            assertThat(fileMetadata.getStreamingS3Key()).isNull();
        }

        @Test
        @DisplayName("생성 직후 파일 크기는 null이다")
        void creation_fileSizeBytes_isNull() {
            // given & when
            FileMetadata fileMetadata = createPendingFileMetadata();

            // then
            assertThat(fileMetadata.getFileSizeBytes()).isNull();
        }
    }
}
