package com.rehearse.api.domain.reviewbookmark.service;

import com.rehearse.api.domain.reviewbookmark.dto.BookmarkExistsResponse;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkIdPair;
import com.rehearse.api.domain.reviewbookmark.dto.BookmarkStatusFilter;
import com.rehearse.api.domain.reviewbookmark.dto.ReviewBookmarkListItem;
import com.rehearse.api.domain.reviewbookmark.entity.ReviewBookmark;
import com.rehearse.api.domain.reviewbookmark.repository.ReviewBookmarkRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Collection;
import java.util.Collections;
import java.util.List;

@Slf4j
@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class ReviewBookmarkQueryService {

    private final ReviewBookmarkRepository reviewBookmarkRepository;

    public List<ReviewBookmarkListItem> listByUser(Long userId, BookmarkStatusFilter statusFilter) {
        List<ReviewBookmark> bookmarks = fetchByStatus(userId, statusFilter);
        return bookmarks.stream()
                .map(ReviewBookmarkListItem::from)
                .toList();
    }

    public BookmarkExistsResponse findBookmarkPairs(Long userId, Collection<Long> tsfIds) {
        if (tsfIds == null || tsfIds.isEmpty()) {
            return new BookmarkExistsResponse(Collections.emptyList());
        }
        List<BookmarkIdPair> pairs = reviewBookmarkRepository.findBookmarkPairs(userId, tsfIds);
        return new BookmarkExistsResponse(pairs);
    }

    private List<ReviewBookmark> fetchByStatus(Long userId, BookmarkStatusFilter statusFilter) {
        return switch (statusFilter) {
            case ALL -> reviewBookmarkRepository.findByUserIdOrderByCreatedAtDesc(userId);
            case IN_PROGRESS ->
                    reviewBookmarkRepository.findByUserIdAndResolvedAtIsNullOrderByCreatedAtDesc(userId);
            case RESOLVED ->
                    reviewBookmarkRepository.findByUserIdAndResolvedAtIsNotNullOrderByCreatedAtDesc(userId);
        };
    }
}
