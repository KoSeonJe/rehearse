package com.rehearse.api.domain.reviewbookmark.entity;

import com.rehearse.api.domain.questionset.entity.TimestampFeedback;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;
import com.rehearse.api.domain.user.entity.User;
import jakarta.persistence.*;
import lombok.AccessLevel;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import java.time.LocalDateTime;

@NamedEntityGraph(
        name = "ReviewBookmark.withDetails",
        attributeNodes = {
                @NamedAttributeNode(value = "timestampFeedback", subgraph = "tsf")
        },
        subgraphs = {
                @NamedSubgraph(name = "tsf", attributeNodes = {
                        @NamedAttributeNode("question"),
                        @NamedAttributeNode(value = "questionSetFeedback", subgraph = "qsf")
                }),
                @NamedSubgraph(name = "qsf", attributeNodes = {
                        @NamedAttributeNode(value = "questionSet", subgraph = "qs")
                }),
                @NamedSubgraph(name = "qs", attributeNodes = {
                        @NamedAttributeNode("interview")
                })
        }
)
@Entity
@Table(name = "review_bookmark",
       uniqueConstraints = @UniqueConstraint(name = "uk_rb_user_tsf",
                                             columnNames = {"user_id", "timestamp_feedback_id"}))
@Getter
@NoArgsConstructor(access = AccessLevel.PROTECTED)
@EntityListeners(AuditingEntityListener.class)
public class ReviewBookmark {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "timestamp_feedback_id", nullable = false)
    private TimestampFeedback timestampFeedback;

    private LocalDateTime resolvedAt;

    @CreatedDate
    @Column(nullable = false, updatable = false)
    private LocalDateTime createdAt;

    @Builder
    public ReviewBookmark(User user, TimestampFeedback timestampFeedback) {
        this.user = user;
        this.timestampFeedback = timestampFeedback;
    }

    public boolean isResolved() {
        return resolvedAt != null;
    }

    public void markResolved() {
        if (isResolved()) {
            return;
        }
        this.resolvedAt = LocalDateTime.now();
    }

    public void reopen() {
        this.resolvedAt = null;
    }

    public void updateResolution(boolean resolved) {
        if (resolved) {
            markResolved();
        } else {
            reopen();
        }
    }

    public void verifyOwnedBy(Long userId) {
        if (userId == null || user == null || !userId.equals(user.getId())) {
            throw new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS);
        }
    }
}
