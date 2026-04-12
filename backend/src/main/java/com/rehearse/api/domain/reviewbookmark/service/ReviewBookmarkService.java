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
import lombok.extern.slf4j.Slf4j;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional
public class ReviewBookmarkService {

    private final ReviewBookmarkRepository reviewBookmarkRepository;
    private final ReviewBookmarkFinder reviewBookmarkFinder;
    private final TimestampFeedbackRepository timestampFeedbackRepository;
    private final UserRepository userRepository;

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
            throw new ReviewBookmarkException(ReviewBookmarkErrorCode.BOOKMARK_ALREADY_EXISTS);
        }
    }

    public void delete(Long userId, Long bookmarkId) {
        ReviewBookmark bookmark = reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId);
        reviewBookmarkRepository.delete(bookmark);
    }

    public ReviewBookmarkResponse updateStatus(Long userId, Long bookmarkId,
                                               UpdateBookmarkStatusRequest request) {
        ReviewBookmark bookmark = reviewBookmarkFinder.findByIdAndValidateOwner(bookmarkId, userId);
        bookmark.updateResolution(request.resolved());
        return ReviewBookmarkResponse.from(bookmark);
    }
}
