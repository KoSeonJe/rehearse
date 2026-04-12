package com.rehearse.api.domain.reviewbookmark.repository;

import com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

import java.util.Collection;
import java.util.List;
import java.util.Optional;

public interface ReviewBookmarkRepository extends JpaRepository<ReviewBookmark, Long> {

    boolean existsByUserIdAndTimestampFeedbackId(Long userId, Long timestampFeedbackId);

    @EntityGraph("ReviewBookmark.withDetails")
    List<ReviewBookmark> findByUserIdOrderByCreatedAtDesc(Long userId);

    @EntityGraph("ReviewBookmark.withDetails")
    List<ReviewBookmark> findByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(Long userId);

    @EntityGraph("ReviewBookmark.withDetails")
    List<ReviewBookmark> findByUserIdAndResolvedAtIsNotNullOrderByCreatedAtDesc(Long userId);

    @Query("""
        SELECT new com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair(
            rb.timestampFeedback.id, rb.id)
        FROM ReviewBookmark rb
        WHERE rb.userId = :userId
          AND rb.timestampFeedback.id IN :tsfIds
    """)
    List<BookmarkIdPair> findBookmarkPairs(Long userId, Collection<Long> tsfIds);

    @Query("SELECT rb.userId FROM ReviewBookmark rb WHERE rb.id = :bookmarkId")
    Optional<Long> findOwnerIdById(Long bookmarkId);
}
