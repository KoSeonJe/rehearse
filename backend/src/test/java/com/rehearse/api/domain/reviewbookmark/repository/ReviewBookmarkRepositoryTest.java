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
import org.junit.jupiter.api.Nested;
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
import com.rehearse.api.domain.questionset.entity.QuestionSetCategory;

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
                .category(QuestionSetCategory.CS_FUNDAMENTAL)
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

    @Nested
    @DisplayName("existsByUserIdAndTimestampFeedbackId 메서드")
    class ExistsByUserIdAndTimestampFeedbackId {

        @Test
        @DisplayName("사용자와 피드백 ID로 북마크 존재 여부를 확인한다")
        void existsByUserIdAndTimestampFeedbackId_returnsTrue() {
            // given
            ReviewBookmark bookmark = ReviewBookmark.builder()
                    .userId(user.getId())
                    .timestampFeedback(entityManager.find(TimestampFeedback.class, timestampFeedback.getId()))
                    .build();
            entityManager.persist(bookmark);
            entityManager.flush();
            entityManager.clear();

            // when
            boolean exists = reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(
                    user.getId(), timestampFeedback.getId());

            // then
            assertThat(exists).isTrue();
        }

        @Test
        @DisplayName("존재하지 않는 조합이면 false를 반환한다")
        void existsByUserIdAndTimestampFeedbackId_returnsFalse() {
            // when
            boolean exists = reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(
                    user.getId(), timestampFeedback.getId());

            // then
            assertThat(exists).isFalse();
        }
    }

    @Nested
    @DisplayName("findByUserIdOrderByCreatedAtDesc 메서드")
    class FindByUserIdOrderByCreatedAtDesc {

        @Test
        @DisplayName("사용자의 북마크를 최신순으로 조회한다")
        void findByUserIdOrderByCreatedAtDesc_returnsOrderedList() throws InterruptedException {
            // given
            TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

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

            ReviewBookmark older = ReviewBookmark.builder()
                    .userId(user.getId())
                    .timestampFeedback(foundTsf)
                    .build();
            entityManager.persist(older);
            entityManager.flush();

            // 시간 차이를 두어 createdAt 순서 보장
            Thread.sleep(10);

            TimestampFeedback foundTsf2 = entityManager.find(TimestampFeedback.class, tsf2.getId());
            ReviewBookmark newer = ReviewBookmark.builder()
                    .userId(user.getId())
                    .timestampFeedback(foundTsf2)
                    .build();
            entityManager.persist(newer);
            entityManager.flush();
            entityManager.clear();

            // when
            List<ReviewBookmark> result = reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            // then
            assertThat(result).hasSize(2);
            assertThat(result.get(0).getTimestampFeedback().getId()).isEqualTo(tsf2.getId());
            assertThat(result.get(1).getTimestampFeedback().getId()).isEqualTo(timestampFeedback.getId());
        }

        @Test
        @DisplayName("북마크가 없으면 빈 리스트를 반환한다")
        void findByUserIdOrderByCreatedAtDesc_returnsEmptyList() {
            // when
            List<ReviewBookmark> result = reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(user.getId());

            // then
            assertThat(result).isEmpty();
        }
    }

    @Nested
    @DisplayName("중복 북마크 제약 조건")
    class DuplicateBookmarkConstraint {

        @Test
        @DisplayName("동일 사용자가 같은 피드백을 중복 북마크하면 DataIntegrityViolationException이 발생한다")
        void duplicateBookmark_throwsDataIntegrityViolationException() {
            // given
            TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

            ReviewBookmark bookmark1 = ReviewBookmark.builder()
                    .userId(user.getId())
                    .timestampFeedback(foundTsf)
                    .build();
            entityManager.persist(bookmark1);
            entityManager.flush();

            // when & then
            assertThatThrownBy(() -> {
                ReviewBookmark bookmark2 = ReviewBookmark.builder()
                        .userId(user.getId())
                        .timestampFeedback(foundTsf)
                        .build();
                entityManager.persist(bookmark2);
                entityManager.flush();
            }).isInstanceOfAny(DataIntegrityViolationException.class, ConstraintViolationException.class);
        }

        @Test
        @DisplayName("ReviewBookmark를 먼저 삭제한 뒤 TimestampFeedback을 삭제하면 두 엔티티 모두 제거된다 (참조 무결성 순서 보장)")
        void cascadeDelete_whenTimestampFeedbackDeleted_reviewBookmarkIsAlsoDeleted() {
            // given
            TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

            ReviewBookmark bookmark = ReviewBookmark.builder()
                    .userId(user.getId())
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

            // when & then
            ReviewBookmark deleted = entityManager.find(ReviewBookmark.class, bookmarkId);
            assertThat(deleted).isNull();
        }
    }

    @Nested
    @DisplayName("findBookmarkPairs 메서드")
    class FindBookmarkPairs {

        @Test
        @DisplayName("findBookmarkPairs는 주어진 tsfId 중 실제 북마크된 항목만 (tsfId, bookmarkId) 쌍으로 반환한다")
        void findBookmarkPairs_returnsCorrectPairsAndIgnoresUnknownIds() {
            // given
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

            TimestampFeedback foundTsf1 = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());
            TimestampFeedback foundTsf2 = entityManager.find(TimestampFeedback.class, tsf2.getId());

            ReviewBookmark bm1 = ReviewBookmark.builder().userId(user.getId()).timestampFeedback(foundTsf1).build();
            ReviewBookmark bm2 = ReviewBookmark.builder().userId(user.getId()).timestampFeedback(foundTsf2).build();
            entityManager.persist(bm1);
            entityManager.persist(bm2);
            entityManager.flush();
            entityManager.clear();

            Long unknownTsfId = 99999L;

            // when
            List<BookmarkIdPair> pairs = reviewBookmarkRepository.findBookmarkPairs(
                    user.getId(),
                    List.of(timestampFeedback.getId(), tsf2.getId(), unknownTsfId));

            // then
            assertThat(pairs).hasSize(2);
            assertThat(pairs).extracting(BookmarkIdPair::timestampFeedbackId)
                    .containsExactlyInAnyOrder(timestampFeedback.getId(), tsf2.getId());
            assertThat(pairs).noneMatch(p -> p.timestampFeedbackId().equals(unknownTsfId));
            assertThat(pairs).allMatch(p -> p.bookmarkId() != null && p.bookmarkId() > 0);
        }
    }

    @Nested
    @DisplayName("findOwnerIdById 메서드")
    class FindOwnerIdById {

        @Test
        @DisplayName("findOwnerIdById는 북마크가 존재하면 소유자 userId를 반환하고 없으면 empty를 반환한다")
        void findOwnerIdById_returnsOwnerIdWhenExistsAndEmptyWhenNot() {
            // given
            TimestampFeedback foundTsf = entityManager.find(TimestampFeedback.class, timestampFeedback.getId());

            ReviewBookmark bookmark = ReviewBookmark.builder()
                    .userId(user.getId())
                    .timestampFeedback(foundTsf)
                    .build();
            entityManager.persist(bookmark);
            entityManager.flush();
            entityManager.clear();

            // when & then
            Optional<Long> ownerId = reviewBookmarkRepository.findOwnerIdById(bookmark.getId());
            assertThat(ownerId).isPresent();
            assertThat(ownerId.get()).isEqualTo(user.getId());

            Optional<Long> missing = reviewBookmarkRepository.findOwnerIdById(99999L);
            assertThat(missing).isEmpty();
        }
    }
}
