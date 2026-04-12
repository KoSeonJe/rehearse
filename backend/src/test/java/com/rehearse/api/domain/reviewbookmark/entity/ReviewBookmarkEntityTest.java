package com.rehearse.api.domain.reviewbookmark.entity;

import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import com.rehearse.api.domain.user.entity.User;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.test.util.ReflectionTestUtils;

import java.time.LocalDateTime;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.assertj.core.api.Assertions.within;
import static java.time.temporal.ChronoUnit.SECONDS;

class ReviewBookmarkEntityTest {

    private User user;
    private TimestampFeedback timestampFeedback;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("테스터")
                .provider(com.rehearse.api.domain.user.entity.OAuthProvider.GOOGLE)
                .providerId("google-123")
                .role(com.rehearse.api.domain.user.entity.UserRole.USER)
                .build();
        ReflectionTestUtils.setField(user, "id", 1L);

        timestampFeedback = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(false)
                .build();
        ReflectionTestUtils.setField(timestampFeedback, "id", 1L);
    }

    @Test
    @DisplayName("생성 직후 isResolved는 false를 반환한다")
    void isResolved_afterCreation_returnsFalse() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        assertThat(bookmark.isResolved()).isFalse();
    }

    @Test
    @DisplayName("markResolved 호출 후 resolvedAt이 현재 시각으로 설정된다")
    void markResolved_setsResolvedAtToNow() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        LocalDateTime before = LocalDateTime.now();
        bookmark.markResolved();

        assertThat(bookmark.getResolvedAt()).isNotNull();
        assertThat(bookmark.getResolvedAt()).isCloseTo(before, within(1, SECONDS));
    }

    @Test
    @DisplayName("markResolved 후 isResolved는 true를 반환한다")
    void isResolved_afterMarkResolved_returnsTrue() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.markResolved();

        assertThat(bookmark.isResolved()).isTrue();
    }

    @Test
    @DisplayName("reopen 호출 후 resolvedAt이 null로 초기화된다")
    void reopen_setsResolvedAtToNull() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();
        bookmark.markResolved();

        bookmark.reopen();

        assertThat(bookmark.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("해결 상태에서 reopen 후 isResolved는 false를 반환한다")
    void isResolved_afterReopen_returnsFalse() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();
        bookmark.markResolved();

        bookmark.reopen();

        assertThat(bookmark.isResolved()).isFalse();
    }

    @Test
    @DisplayName("이미 해결된 북마크에 markResolved를 다시 호출해도 원래 resolvedAt은 유지된다")
    void markResolved_whenAlreadyResolved_preservesOriginalTimestamp() throws InterruptedException {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.markResolved();
        LocalDateTime firstResolvedAt = bookmark.getResolvedAt();

        Thread.sleep(10);
        bookmark.markResolved();

        assertThat(bookmark.getResolvedAt()).isEqualTo(firstResolvedAt);
    }

    @Test
    @DisplayName("updateResolution(true)는 markResolved, false는 reopen과 동일하게 동작한다")
    void updateResolution_togglesStateAccordingToFlag() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.updateResolution(true);
        assertThat(bookmark.isResolved()).isTrue();

        bookmark.updateResolution(false);
        assertThat(bookmark.isResolved()).isFalse();
    }

    @Test
    @DisplayName("verifyOwnedBy - 소유자면 통과")
    void verifyOwnedBy_ownerPasses() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.verifyOwnedBy(1L);
    }

    @Test
    @DisplayName("verifyOwnedBy - 타인 소유면 ReviewBookmarkException(FORBIDDEN_ACCESS)")
    void verifyOwnedBy_otherUserThrows() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        assertThatThrownBy(() -> bookmark.verifyOwnedBy(999L))
                .isInstanceOf(com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException.class);
    }

    @Test
    @DisplayName("verifyOwnedBy - userId가 null이면 ReviewBookmarkException 발생")
    void verifyOwnedBy_nullUserId_throwsException() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        assertThatThrownBy(() -> bookmark.verifyOwnedBy(null))
                .isInstanceOf(com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException.class);
    }

    @Test
    @DisplayName("reopen - 이미 미해결 상태에서 호출해도 resolvedAt이 null로 유지된다")
    void reopen_whenAlreadyOpen_remainsNull() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.reopen();

        assertThat(bookmark.getResolvedAt()).isNull();
    }

    @Test
    @DisplayName("markResolved → reopen → markResolved 왕복 시 상태가 올바르게 전환된다")
    void markResolved_reopen_markResolved_stateTransitionsCorrectly() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(user)
                .timestampFeedback(timestampFeedback)
                .build();

        bookmark.markResolved();
        assertThat(bookmark.isResolved()).isTrue();

        bookmark.reopen();
        assertThat(bookmark.isResolved()).isFalse();
        assertThat(bookmark.getResolvedAt()).isNull();

        bookmark.markResolved();
        assertThat(bookmark.isResolved()).isTrue();
        assertThat(bookmark.getResolvedAt()).isNotNull();
    }
}
