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
import com.rehearse.api.domain.user.entity.User;
import com.rehearse.api.domain.user.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.hibernate.exception.ConstraintViolationException;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@RequiredArgsConstructor
public class ReviewBookmarkService {

    private static final String UNIQUE_CONSTRAINT_NAME = "uk_rb_user_tsf";

    private final ReviewBookmarkRepository reviewBookmarkRepository;
    private final TimestampFeedbackRepository timestampFeedbackRepository;
    private final UserRepository userRepository;

    @Transactional
    public ReviewBookmarkResponse create(Long userId, CreateReviewBookmarkRequest request) {
        Long tsfId = request.timestampFeedbackId();

        if (reviewBookmarkRepository.existsByUserIdAndTimestampFeedbackId(userId, tsfId)) {
            throw new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
        }

        TimestampFeedback tsf = timestampFeedbackRepository.findById(tsfId)
                .orElseThrow(() -> new ReviewBookmarkException(
                        ReviewBookmarkErrorCode.TIMESTAMP_FEEDBACK_NOT_FOUND));

        User user = userRepository.getReferenceById(userId);

        try {
            ReviewBookmark saved = reviewBookmarkRepository.save(
                    ReviewBookmark.builder()
                            .user(user)
                            .timestampFeedback(tsf)
                            .build());
            return ReviewBookmarkResponse.from(saved);
        } catch (DataIntegrityViolationException e) {
            if (isUniqueConstraintViolation(e)) {
                throw new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
            }
            throw e;
        }
    }

    @Transactional
    public void delete(Long userId, Long bookmarkId) {
        Long ownerId = reviewBookmarkRepository.findOwnerIdById(bookmarkId)
                .orElseThrow(() -> new ReviewBookmarkException(
                        ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND));

        if (!ownerId.equals(userId)) {
            throw new ReviewBookmarkException(ReviewBookmarkErrorCode.FORBIDDEN_ACCESS);
        }

        reviewBookmarkRepository.deleteById(bookmarkId);
    }

    @Transactional
    public ReviewBookmarkResponse updateStatus(Long userId, Long bookmarkId,
                                               UpdateBookmarkStatusRequest request) {
        ReviewBookmark bookmark = reviewBookmarkRepository.findById(bookmarkId)
                .orElseThrow(() -> new ReviewBookmarkException(
                        ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND));

        bookmark.verifyOwnedBy(userId);
        bookmark.updateResolution(request.resolved());

        return ReviewBookmarkResponse.from(bookmark);
    }

    private boolean isUniqueConstraintViolation(DataIntegrityViolationException e) {
        Throwable cause = e.getCause();
        if (cause instanceof ConstraintViolationException cve) {
            String name = cve.getConstraintName();
            return name != null && name.toLowerCase().contains(UNIQUE_CONSTRAINT_NAME);
        }
        // Fallback: match against root message — some drivers surface the constraint
        // name only in the SQL error text, not the parsed ConstraintViolationException.
        String message = e.getMostSpecificCause().getMessage();
        return message != null && message.toLowerCase().contains(UNIQUE_CONSTRAINT_NAME);
    }
}
