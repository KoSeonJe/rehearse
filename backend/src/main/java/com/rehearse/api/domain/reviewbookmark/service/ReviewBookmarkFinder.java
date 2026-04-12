package com.rehearse.api.domain.reviewbookmark.service;

import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkErrorCode;
import com.rehearse.api.domain.reviewbookmark.exception.ReviewBookmarkException;
import com.rehearse.api.domain.reviewbookmark.repository.ReviewBookmarkRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Component;

@Component
@RequiredArgsConstructor
public class ReviewBookmarkFinder {

    private final ReviewBookmarkRepository reviewBookmarkRepository;

    public ReviewBookmark findById(Long id) {
        return reviewBookmarkRepository.findById(id)
                .orElseThrow(() -> new ReviewBookmarkException(
                        ReviewBookmarkErrorCode.BOOKMARK_NOT_FOUND));
    }

    public ReviewBookmark findByIdAndValidateOwner(Long id, Long userId) {
        ReviewBookmark bookmark = findById(id);
        bookmark.verifyOwnedBy(userId);
        return bookmark;
    }
}
