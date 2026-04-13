package com.rehearse.api.domain.reviewbookmark.service;

import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;
import com.rehearse.api.domain.reviewbookmark.repository.ReviewBookmarkRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.then;

@ExtendWith(MockitoExtension.class)
class ReviewBookmarkFinderTest {

    @InjectMocks
    private ReviewBookmarkFinder reviewBookmarkFinder;

    @Mock
    private ReviewBookmarkRepository reviewBookmarkRepository;

    @Nested
    @DisplayName("findById")
    class FindById {

        @Test
        @DisplayName("findById - 존재하는 북마크 ID 조회 시 북마크 반환")
        void findById_existingId_returnsBookmark() {
            // given
            ReviewBookmark bookmark = createBookmark(1L, 10L);
            given(reviewBookmarkRepository.findById(1L)).willReturn(Optional.of(bookmark));

            // when
            ReviewBookmark result = reviewBookmarkFinder.findById(1L);

            // then
            assertThat(result).isEqualTo(bookmark);
            then(reviewBookmarkRepository).should().findById(1L);
        }

        @Test
        @DisplayName("findById - 존재하지 않는 북마크 ID 조회 시 BOOKMARK_NOT_FOUND 예외 발생")
        void findById_nonExistingId_throwsBookmarkNotFoundException() {
            // given
            given(reviewBookmarkRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewBookmarkFinder.findById(999L))
                    .isInstanceOf(ReviewBookmarkException.class)
                    .hasMessage(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND.getMessage());
        }
    }

    @Nested
    @DisplayName("findByIdAndValidateOwner")
    class FindByIdAndValidateOwner {

        @Test
        @DisplayName("findByIdAndValidateOwner - 본인 소유 북마크 조회 시 북마크 반환")
        void findByIdAndValidateOwner_ownerMatch_returnsBookmark() {
            // given
            Long bookmarkId = 1L;
            Long userId = 10L;
            ReviewBookmark bookmark = createBookmark(bookmarkId, userId);
            given(reviewBookmarkRepository.findById(bookmarkId)).willReturn(Optional.of(bookmark));

            // when
            ReviewBookmark result = reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId);

            // then
            assertThat(result).isEqualTo(bookmark);
        }

        @Test
        @DisplayName("findByIdAndValidateOwner - 존재하지 않는 북마크 ID 조회 시 BOOKMARK_NOT_FOUND 예외 발생")
        void findByIdAndValidateOwner_nonExistingId_throwsBookmarkNotFoundException() {
            // given
            given(reviewBookmarkRepository.findById(999L)).willReturn(Optional.empty());

            // when & then
            assertThatThrownBy(() -> reviewBookmarkFinder.findByIdAndValidateOwner(999L, 10L))
                    .isInstanceOf(ReviewBookmarkException.class)
                    .hasMessage(ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND.getMessage());
        }

        @Test
        @DisplayName("findByIdAndValidateOwner - 다른 사용자의 북마크 조회 시 FORBIDDEN_ACCESS 예외 발생")
        void findByIdAndValidateOwner_differentOwner_throwsForbiddenAccessException() {
            // given
            Long bookmarkId = 1L;
            Long ownerId = 10L;
            Long otherUserId = 99L;
            ReviewBookmark bookmark = createBookmark(bookmarkId, ownerId);
            given(reviewBookmarkRepository.findById(bookmarkId)).willReturn(Optional.of(bookmark));

            // when & then
            assertThatThrownBy(() -> reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, otherUserId))
                    .isInstanceOf(ReviewBookmarkException.class)
                    .hasMessage(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS.getMessage());
        }
    }

    private ReviewBookmark createBookmark(Long bookmarkId, Long userId) {
        return ReviewBookmark.builder()
                .userId(userId)
                .timestampFeedback(null)
                .build();
    }
}
