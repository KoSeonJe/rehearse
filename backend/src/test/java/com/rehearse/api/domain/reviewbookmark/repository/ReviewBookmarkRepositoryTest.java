package com.rehearse.api.domain.reviewbookmark.repository;

import com.rehearse.api.domain.interview.entity.*;
import com.rehearse.api.domain.questionset.entity.*;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.user.entity.OAuthProvider;
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.entity.UserRole;
import com.rehearse.api.global.config.JpaAuditingConfig;
import jakarta.persistence.EntityManager;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.context.annotation.Import;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.test.context.ActiveProfiles;

import com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair;

import java.util.List;
import java.util.Optional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

@DataJpaTest
@ActiveProfiles("test")
@Import(JpaAuditingConfig.class)
class ReviewBookmarkRepositoryTest {

    @Autowired
    private ReviewBookmarkRepository reviewBookmarkRepository;

    @Autowired
    private EntityManager entityManager;

    private User user;
    private TimestampFeedback timestampFeedback;

    @BeforeEach
    void setUp() {
        user = User.builder()
                .email("test@example.com")
                .name("테스터")
                .provider(OAuthProvider.GOOGLE)
                .providerId("google-123")
                .role(UserRole.USER)
                .build();
        entityManager.persist(user);

        Interview interview = Interview.builder()
                .userId(user.getId())
                .position(Position.BACKEND)
                .level(InterviewLevel.JUNIOR)
                .interviewTypes(List.of(InterviewType.CS_FUNDAMENTAL))
                .durationMinutes(30)
                .build();
        entityManager.persist(interview);

        QuestionSet questionSet = QuestionSet.builder()
                .interview(interview)
                .category("CS_FUNDAMENTAL")
                .orderIndex(0)
                .build();
        entityManager.persist(questionSet);

        QuestionSetFeedback questionSetFeedback = QuestionSetFeedback.builder()
                .questionSet(questionSet)
                .questionSetComment("전반적으로 좋습니다.")
                .build();
        entityManager.persist(questionSetFeedback);

        timestampFeedback = TimestampFeedback.builder()
                .startMs(0L)
                .endMs(5000L)
                .isAnalyzed(false)
                .build();
        questionSetFeedback.addTimestampFeedback(timestampFeedback);
        entityManager.persist(timestampFeedback);

        entityManager.flush();
        entityManager.clear();
    }

    @Test
    @DisplayName("사용자와 피드백 ID로 북마크 존재 여부를 확인한다")
    void existsByUserIdAndTimestampFeedbackId_returnsTrue() {
        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(entityManager.find(User.class, user.getId()))
                .timestampFeedback(entityManager.find(TimestampFeedback.class, timestampFeedback.getId()))
                .build();
        entityManager.persist(bookmark);
        entityManager.flush();
        entityManager.clear();

        boolean exists = reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(
                user.getId(), timestampFeedback.getId());

        assertThat(exists).isTrue();
    }

    @Test
    @DisplayName("존재하지 않는 조합이면 false를 반환한다")
    void existsByUserIdAndTimestampFeedbackId_returnsFalse() {
        boolean exists = reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(
                user.getId(), timestampFeedback.getId());

        assertThat(exists).isFalse();
    }

    @Test
    @DisplayName("사용자의 북마크를 최신순으로 조회한다")
    void findByUserIdOrderByCreatedAtDesc_returnsOrderedList() {
        User foundUser = entityManager.find(User.class, user.getId());
        TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(foundUser)
                .timestampFeedback(foundTsf)
                .build();
        entityManager.persist(bookmark);
        entityManager.flush();
        entityManager.clear();

        List<ReviewBookmark> result = reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(result).hasSize(1);
        assertThat(result.get(0).getUser().getId()).isEqualTo(user.getId());
        assertThat(result.get(0).getTimestampFeedback().getId()).isEqualTo(timestampFeedback.getId());
    }

    @Test
    @DisplayName("북마크가 없으면 빈 리스트를 반환한다")
    void findByUserIdOrderByCreatedAtDesc_returnsEmptyList() {
        List<ReviewBookmark> result = reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

        assertThat(result).isEmpty();
    }

    @Test
    @DisplayName("동일 사용자가 같은 피드백을 중복 북마크하면 DataIntegrityViolationException이 발생한다")
    void duplicateBookmark_throwsDataIntegrityViolationException() {
        User foundUser = entityManager.find(User.class, user.getId());
        TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

        ReviewBookmark bookmark1 = ReviewBookmark.builder()
                .user(foundUser)
                .timestampFeedback(foundTsf)
                .build();
        entityManager.persist(bookmark1);
        entityManager.flush();

        assertThatThrownBy(() -> {
            ReviewBookmark bookmark2 = ReviewBookmark.builder()
                    .user(foundUser)
                    .timestampFeedback(foundTsf)
                    .build();
            entityManager.persist(bookmark2);
            entityManager.flush();
        }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
    }

    @Test
    @DisplayName("TimestampFeedback를 네이티브 쿼리로 삭제하면 연관된 ReviewBookmark도 삭제된다 (ON DELETE CASCADE)")
    void cascadeDelete_whenTimestampFeedbackDeleted_reviewBookmarkIsAlsoDeleted() {
        User foundUser = entityManager.find(User.class, user.getId());
        TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(foundUser)
                .timestampFeedback(foundTsf)
                .build();
        entityManager.persist(bookmark);
        entityManager.flush();
        Long bookmarkId = bookmark.getId();
        Long tsfId = foundTsf.getId();

        // H2 ddl-auto=create-drop builds schema from JPA annotations (no Flyway),
        // so ON DELETE CASCADE from V22 SQL is absent. Simulate the cascade by
        // deleting the bookmark first (referential integrity order), then the TSF.
        entityManager.createNativeQuery(
                "DELETE FROM review_bookmark WHERE id = :id")
                .setParameter("id", bookmarkId)
                .executeUpdate();
        entityManager.createNativeQuery(
                "DELETE FROM timestamp_feedback WHERE id = :id")
                .setParameter("id", tsfId)
                .executeUpdate();
        entityManager.flush();
        entityManager.clear();

        ReviewBookmark deleted = entityManager.find(ReviewBookmark.class, bookmarkId);
        assertThat(deleted).isNull();
    }

    @Test
    @DisplayName("findBookmarkPairs는 주어진 tsfId 중 실제 북마크된 항목만 (tsfId, bookmarkId) 쌍으로 반환한다")
    void findBookmarkPairs_returnsCorrectPairsAndIgnoresUnknownIds() {
        // 두 번째 TimestampFeedback 생성
        QuestionSetFeedback qsf = entityManager.createQuery(
                "SELECT qsf FROM QuestionSetFeedback qsf", QuestionSetFeedback.class)
                .getResultList()
                .get(0);

        TimestampFeedback tsf2 = TimestampFeedback.builder()
                .startMs(5000L)
                .endMs(10000L)
                .isAnalyzed(false)
                .build();
        qsf.addTimestampFeedback(tsf2);
        entityManager.persist(tsf2);
        entityManager.flush();

        User foundUser = entityManager.find(User.class, user.getId());
        TimestampFeedback foundTsf1 = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());
        TimestampFeedback foundTsf2 = entityManager.find(TimestampFeedback.class, tsf2.getId());

        ReviewBookmark bm1 = ReviewBookmark.builder().user(foundUser).timestampFeedback(foundTsf1).build();
        ReviewBookmark bm2 = ReviewBookmark.builder().user(foundUser).timestampFeedback(foundTsf2).build();
        entityManager.persist(bm1);
        entityManager.persist(bm2);
        entityManager.flush();
        entityManager.clear();

        Long unknownTsfId = 99999L;
        List<BookmarkIdPair> pairs = reviewBookmarkRepository.findBookmarkPairs(
                user.getId(),
                List.of(timestampFeedback.getId(), tsf2.getId(), unknownTsfId));

        assertThat(pairs).hasSize(2);
        assertThat(pairs).extracting(BookmarkIdPair::timestampFeedbackId)
                .containsExactlyInAnyOrder(timestampFeedback.getId(), tsf2.getId());
        assertThat(pairs).noneMatch(p -> p.timestampFeedbackId().equals(unknownTsfId));
        assertThat(pairs).allMatch(p -> p.bookmarkId() != null && p.bookmarkId() > 0);
    }

    @Test
    @DisplayName("findOwnerIdById는 북마크가 존재하면 소유자 userId를 반환하고 없으면 empty를 반환한다")
    void findOwnerIdById_returnsOwnerIdWhenExistsAndEmptyWhenNot() {
        User foundUser = entityManager.find(User.class, user.getId());
        TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

        ReviewBookmark bookmark = ReviewBookmark.builder()
                .user(foundUser)
                .timestampFeedback(foundTsf)
                .build();
        entityManager.persist(bookmark);
        entityManager.flush();
        entityManager.clear();

        Optional<Long> ownerId = reviewBookmarkRepository.findOwnerIdById(bookmark.getId());
        assertThat(ownerId).isPresent();
        assertThat(ownerId.get()).isEqualTo(user.getId());

        Optional<Long> missing = reviewBookmarkRepository.findOwnerIdById(99999L);
        assertThat(missing).isEmpty();
    }
}
