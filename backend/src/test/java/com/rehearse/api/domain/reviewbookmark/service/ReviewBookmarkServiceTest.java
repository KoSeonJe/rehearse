package com.rehearse.api.domain.reviewbookmark.service;

import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import com.rehearse.api.domain.questionset.repository.TimestampFeedbackRepository;
import com.rehearse.api.domain.reviewbookmark.dto.CreateReviewBookmarkRequest;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkResponse;
import com.rehearse.api.domain.reviewbookmark.dto.UpdateBookmarkStatusRequest;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;
import com.rehearse.api.domain.reviewbookmark.repository.ReviewBookmarkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;
import static org.mockito.BDDMockito.willDoNothing;
import static org.mockito.BDDMockito.willThrow;

@ExtendWith(MockitoExtension.class)
class ReviewBookmarkServiceTest {

    @InjectMocks
    private ReviewBookmarkService reviewBookmarkService;

    @Mock
    private ReviewBookmarkRepository reviewBookmarkRepository;

    @Mock
    private ReviewBookmarkFinder reviewBookmarkFinder;

    @Mock
    private TimestampFeedbackRepository timestampFeedbackRepository;

    // ====== create ======

    @Test
    @DisplayName("create - 정상 생성 시 ReviewBookmarkResponse 반환")
    void create_success() {
        Long userId = 1L;
        Long tsfId = 10L;
        CreateReviewBookmarkRequest request = new CreateReviewBookmarkRequest(tsfId);

        TimestampFeedback tsf = createMockTimestampFeedback(tsfId);
        ReviewBookmark bookmark = ReviewBookmark.builder().userId(userId).timestampFeedback(tsf).build();
        ReflectionTestUtils.setField(bookmark, "id", 100L);
        ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

        given(reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(userId, tsfId))
                .willReturn(false);
        given(timestampFeedbackRepository.findById(tsfId)).willReturn(Optional.of(tsf));
        given(reviewBookmarkRepository.save(any(ReviewBookmark.class))).willReturn(bookmark);

        ReviewBookmarkResponse response = reviewBookmarkService.create(userId, request);

        assertThat(response.id()).isEqualTo(100L);
        assertThat(response.timestampFeedbackId()).isEqualTo(tsfId);
        then(reviewBookmarkRepository).should().save(any(ReviewBookmark.class));
    }

    @Test
    @DisplayName("create - 이미 존재하는 북마크면 409 BOOKMARK_ALREADY_EXISTS")
    void create_alreadyExists_throws409() {
        Long userId = 1L;
        Long tsfId = 10L;
        CreateReviewBookmarkRequest request = new CreateReviewBookmarkRequest(tsfId);

        given(reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(userId, tsfId))
                .willReturn(true);

        assertThatThrownBy(() -> reviewBookmarkService.create(userId, request))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS.getCode());
                });
    }

    @Test
    @DisplayName("create - race condition 발생 시 DataIntegrityViolationException → 409")
    void create_raceCondition_throws409() {
        Long userId = 1L;
        Long tsfId = 10L;
        CreateReviewBookmarkRequest request = new CreateReviewBookmarkRequest(tsfId);

        TimestampFeedback tsf = createMockTimestampFeedback(tsfId);

        given(reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(userId, tsfId))
                .willReturn(false);
        given(timestampFeedbackRepository.findById(tsfId)).willReturn(Optional.of(tsf));
        given(reviewBookmarkRepository.save(any(ReviewBookmark.class)))
                .willThrow(new DataIntegrityViolationException("duplicate"));

        assertThatThrownBy(() -> reviewBookmarkService.create(userId, request))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS.getCode());
                });
    }

    @Test
    @DisplayName("create - 존재하지 않는 TimestampFeedback이면 404 TIMESTAMP_FEEDBACK_NOT_FOUND")
    void create_tsfNotFound_throws404() {
        Long userId = 1L;
        Long tsfId = 999L;
        CreateReviewBookmarkRequest request = new CreateReviewBookmarkRequest(tsfId);

        given(reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(userId, tsfId))
                .willReturn(false);
        given(timestampFeedbackRepository.findById(tsfId)).willReturn(Optional.empty());

        assertThatThrownBy(() -> reviewBookmarkService.create(userId, request))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.TIMESTAMP_FEEDBACK_NOT_FOUND.getCode());
                });
    }

    // ====== delete ======

    @Test
    @DisplayName("delete - 정상 삭제")
    void delete_success() {
        Long userId = 1L;
        Long bookmarkId = 100L;

        TimestampFeedback tsf = createMockTimestampFeedback(10L);
        ReviewBookmark mockBookmark = ReviewBookmark.builder().userId(userId).timestampFeedback(tsf).build();
        ReflectionTestUtils.setField(mockBookmark, "id", bookmarkId);
        ReflectionTestUtils.setField(mockBookmark, "createdAt", LocalDateTime.now());

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId)).willReturn(mockBookmark);
        willDoNothing().given(reviewBookmarkRepository).delete(mockBookmark);

        reviewBookmarkService.delete(userId, bookmarkId);

        then(reviewBookmarkRepository).should().delete(mockBookmark);
    }

    @Test
    @DisplayName("delete - 북마크 미존재 시 404 BOOKMARK_NOT_FOUND")
    void delete_notFound_throws404() {
        Long userId = 1L;
        Long bookmarkId = 999L;

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId))
                .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND));

        assertThatThrownBy(() -> reviewBookmarkService.delete(userId, bookmarkId))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("delete - 타인 소유 북마크 삭제 시 403 FORBIDDEN_ACCESS")
    void delete_forbidden_throws403() {
        Long userId = 1L;
        Long bookmarkId = 100L;

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId))
                .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS));

        assertThatThrownBy(() -> reviewBookmarkService.delete(userId, bookmarkId))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS.getCode());
                });
    }

    // ====== updateStatus ======

    @Test
    @DisplayName("updateStatus - resolved=true 시 resolvedAt 설정됨")
    void updateStatus_resolvedTrue() {
        Long userId = 1L;
        Long bookmarkId = 100L;
        UpdateBookmarkStatusRequest request = new UpdateBookmarkStatusRequest(true);

        TimestampFeedback tsf = createMockTimestampFeedback(10L);
        ReviewBookmark bookmark = ReviewBookmark.builder().userId(userId).timestampFeedback(tsf).build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);
        ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId)).willReturn(bookmark);

        ReviewBookmarkResponse response = reviewBookmarkService.updateStatus(userId, bookmarkId, request);

        assertThat(response.resolvedAt()).isNotNull();
    }

    @Test
    @DisplayName("updateStatus - resolved=false 시 resolvedAt null")
    void updateStatus_resolvedFalse() {
        Long userId = 1L;
        Long bookmarkId = 100L;
        UpdateBookmarkStatusRequest request = new UpdateBookmarkStatusRequest(false);

        TimestampFeedback tsf = createMockTimestampFeedback(10L);
        ReviewBookmark bookmark = ReviewBookmark.builder().userId(userId).timestampFeedback(tsf).build();
        ReflectionTestUtils.setField(bookmark, "id", bookmarkId);
        ReflectionTestUtils.setField(bookmark, "createdAt", LocalDateTime.now());
        bookmark.markResolved();

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId)).willReturn(bookmark);

        ReviewBookmarkResponse response = reviewBookmarkService.updateStatus(userId, bookmarkId, request);

        assertThat(response.resolvedAt()).isNull();
    }

    @Test
    @DisplayName("updateStatus - 북마크 미존재 시 404 BOOKMARK_NOT_FOUND")
    void updateStatus_notFound_throws404() {
        Long userId = 1L;
        Long bookmarkId = 999L;
        UpdateBookmarkStatusRequest request = new UpdateBookmarkStatusRequest(true);

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId))
                .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND));

        assertThatThrownBy(() -> reviewBookmarkService.updateStatus(userId, bookmarkId, request))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND.getCode());
                });
    }

    @Test
    @DisplayName("updateStatus - 타인 소유 북마크 수정 시 403 FORBIDDEN_ACCESS")
    void updateStatus_forbidden_throws403() {
        Long userId = 1L;
        Long bookmarkId = 100L;
        UpdateBookmarkStatusRequest request = new UpdateBookmarkStatusRequest(true);

        given(reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId))
                .willThrow(new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS));

        assertThatThrownBy(() -> reviewBookmarkService.updateStatus(userId, bookmarkId, request))
                .isInstanceOf(ReviewBookmarkException.class)
                .satisfies(ex -> {
                    ReviewBookmarkException rbe = (ReviewBookmarkException) ex;
                    assertThat(rbe.getCode()).isEqualTo(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS.getCode());
                });
    }

    // ====== helpers ======

    private TimestampFeedback createMockTimestampFeedback(Long id) {
        TimestampFeedback tsf = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(false)
                .build();
        ReflectionTestUtils.setField(tsf, "id", id);
        return tsf;
    }

}
