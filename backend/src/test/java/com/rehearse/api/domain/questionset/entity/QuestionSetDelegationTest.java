package com.rehearse.api.domain.questionset.entity;

import com.rehearse.api.domain.file.entity.FileMetadata;
import com.rehearse.api.domain.file.entity.FileType;
import com.rehearse.api.domain.interview.entity.Interview;
import com.rehearse.api.global.support.TestFixtures;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

@DisplayName("QuestionSet 위임 메서드")
class QuestionSetDelegationTest {

    @Nested
    @DisplayName("updateStreamingS3Key 메서드")
    class UpdateStreamingS3Key {

        @Test
        @DisplayName("fileMetadata가 있으면 streamingS3Key를 업데이트한다")
        void updateStreamingS3Key_withFileMetadata_updatesKey() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            FileMetadata fileMetadata = FileMetadata.builder()
                    .fileType(FileType.VIDEO)
                    .s3Key("videos/test.webm")
                    .bucket("rehearse-videos-dev")
                    .contentType("video/webm")
                    .build();
            questionSet.assignFileMetadata(fileMetadata);

            // when
            questionSet.updateStreamingS3Key("videos/test.mp4");

            // then
            assertThat(fileMetadata.getStreamingS3Key()).isEqualTo("videos/test.mp4");
        }

        @Test
        @DisplayName("fileMetadata가 null이면 아무 일도 하지 않는다")
        void updateStreamingS3Key_noFileMetadata_doesNothing() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when & then — 예외 없이 무시
            questionSet.updateStreamingS3Key("videos/test.mp4");
        }
    }

    @Nested
    @DisplayName("getFileS3Key 메서드")
    class GetFileS3Key {

        @Test
        @DisplayName("fileMetadata가 있으면 s3Key를 반환한다")
        void getFileS3Key_withFileMetadata_returnsKey() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);
            FileMetadata fileMetadata = FileMetadata.builder()
                    .fileType(FileType.VIDEO)
                    .s3Key("videos/test.webm")
                    .bucket("rehearse-videos-dev")
                    .contentType("video/webm")
                    .build();
            questionSet.assignFileMetadata(fileMetadata);

            // when
            String key = questionSet.getFileS3Key();

            // then
            assertThat(key).isEqualTo("videos/test.webm");
        }

        @Test
        @DisplayName("fileMetadata가 null이면 null을 반환한다")
        void getFileS3Key_noFileMetadata_returnsNull() {
            // given
            Interview interview = TestFixtures.createInterview();
            QuestionSet questionSet = TestFixtures.createQuestionSet(interview);

            // when & then
            assertThat(questionSet.getFileS3Key()).isNull();
        }
    }
}
